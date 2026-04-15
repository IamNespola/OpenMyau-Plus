package myau.module.modules.combat;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;
import myau.util.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import net.minecraft.util.MovingObjectPosition;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * KnockbackDelay — delays incoming velocity + transaction packets
 * to manipulate when knockback is applied to the player.
 *
 * Queues server-bound packets while the player is hurt and releases
 * them after a configurable delay, keeping packet order intact.
 */
public class KnockbackDelay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final IntProperty airDelay = new IntProperty("AirDelay", 90, 0, 1000);
    private final IntProperty groundDelay = new IntProperty("GroundDelay", 0, 0, 1000);
    private final IntProperty chance = new IntProperty("Chance", 100, 0, 100);
    private final BooleanProperty realtimeDamage = new BooleanProperty("RealtimeDamage", true);
    private final BooleanProperty requireTarget = new BooleanProperty("RequireTarget", false);
    private final BooleanProperty onlySwords = new BooleanProperty("OnlySwords", false);

    private final Queue<TimedPacket> packets = new ConcurrentLinkedQueue<>();
    private boolean blink;

    public KnockbackDelay() {
        super("KnockbackDelay", Category.COMBAT,false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{airDelay.getValue() + " - " + groundDelay.getValue()};
    }

    @Override
    public void onDisabled() {
        reset();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20) return;

        if (mc.currentScreen != null) {
            reset();
            return;
        }

        if (!shouldActivate()) {
            reset();
            return;
        }

        int delay = mc.thePlayer.onGround ? groundDelay.getValue() : airDelay.getValue();

        if (!packets.isEmpty()) {
            handle(delay);
        }

        if (mc.thePlayer.hurtTime > 0) {
            blink = true;
        } else if (packets.isEmpty()) {
            blink = false;
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        reset();
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20 || event.isCancelled()) return;

        Packet<?> packet = event.getPacket();

        // Always let these critical/cosmetic packets through immediately
        if (packet instanceof S07PacketRespawn) return;
        if (packet instanceof S03PacketTimeUpdate) return;
        if (packet instanceof S06PacketUpdateHealth) return;
        if (packet instanceof S13PacketDestroyEntities) return;
        if (packet instanceof S02PacketChat) return;
        if (packet instanceof S25PacketBlockBreakAnim) return;
        if (packet instanceof S2FPacketSetSlot) return;

        if (packet instanceof S2BPacketChangeGameState) {
            int state = ((S2BPacketChangeGameState) packet).getGameState();
            if (state == 1 || state == 2 || state == 7 || state == 8) return;
        }

        if (packet instanceof S2CPacketSpawnGlobalEntity) {
            if (((S2CPacketSpawnGlobalEntity) packet).func_149053_g() == 1) return;
        }

        if (packet instanceof S29PacketSoundEffect) {
            if ("ambient.weather.thunder".equalsIgnoreCase(((S29PacketSoundEffect) packet).getSoundName())) return;
        }

        // Let damage status through in realtime so hurt animation plays immediately
        if (realtimeDamage.getValue() && packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus statusPacket = (S19PacketEntityStatus) packet;
            if (statusPacket.getOpCode() == 2 && statusPacket.getEntity(mc.theWorld) == mc.thePlayer) {
                return;
            }
        }

        if (blink) {
            event.setCancelled(true);
            packets.add(new TimedPacket(packet, System.currentTimeMillis()));
        }
    }

    private boolean shouldActivate() {
        if (RandomUtil.nextInt(0, 100) > chance.getValue()) return false;

        if (requireTarget.getValue() && findTarget() == null) return false;

        if (onlySwords.getValue() && !ItemUtil.isHoldingSword()) return false;

        return true;
    }

    private void reset() {
        if (!blink) return;
        blink = false;
        flush();
    }

    private void handle(int delay) {
        while (!packets.isEmpty()) {
            TimedPacket wrapper = packets.peek();
            if (wrapper != null && wrapper.elapsed(delay)) {
                packets.poll();
                processPacketSilent(wrapper.packet);
            } else {
                break;
            }
        }
    }

    private void flush() {
        TimedPacket wrapper;
        while ((wrapper = packets.poll()) != null) {
            processPacketSilent(wrapper.packet);
        }
    }

    @SuppressWarnings("unchecked")
    private void processPacketSilent(Packet<?> packet) {
        try {
            if (mc.getNetHandler() != null) {
                ((Packet<INetHandlerPlayClient>) packet).processPacket(mc.getNetHandler());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class TimedPacket {
        private final Packet<?> packet;
        private final long time;

        public TimedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }

        public boolean elapsed(int delayMs) {
            return System.currentTimeMillis() - time >= delayMs;
        }
    }

    private Entity findTarget() {
        KillAura ka = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (ka != null && ka.isEnabled() && ka.target != null) {
            try {
                Field entityField = KillAura.AttackData.class.getDeclaredField("entity");
                entityField.setAccessible(true);
                return (Entity) entityField.get(ka.target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mc.pointedEntity != null) return mc.pointedEntity;

        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return mc.objectMouseOver.entityHit;
        }

        return null;
    }
}