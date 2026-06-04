package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TimerRange extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty lagTicks = new IntProperty("Lag ticks", 2, 0, 10);
    public final IntProperty timerTicks = new IntProperty("Timer ticks", 2, 0, 10);
    public final FloatProperty minRange = new FloatProperty("Min range", 3.6F, 0.0F, 8.0F);
    public final FloatProperty maxRange = new FloatProperty("Max range", 5.0F, 0.0F, 8.0F);
    public final IntProperty delay = new IntProperty("Delay", 500, 0, 4000);
    public final IntProperty fov = new IntProperty("Fov", 180, 0, 360);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("Ignore teammates", true);
    public final BooleanProperty onlyOnGround = new BooleanProperty("Only onGround", false);
    public final BooleanProperty clearMotion = new BooleanProperty("Clear motion", false);
    public final BooleanProperty notWhileKB = new BooleanProperty("Not while kb", false);
    public final BooleanProperty notWhileScaffold = new BooleanProperty("Not while scaffold", true);

    private final Queue<Packet<?>> delayedPackets = new ConcurrentLinkedQueue<>();
    private State state = State.NONE;
    private int hasLag;
    private long lastTimerTime = -1L;
    private float yaw;
    private float pitch;
    private double motionX;
    private double motionY;
    private double motionZ;

    public TimerRange() {
        super("TimerRange", false, false, "Use timer help you to beat opponent.");
    }

    @Override
    public void onDisabled() {
        done();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || mc.thePlayer == null || mc.theWorld == null) return;

        switch (state) {
            case NONE:
                if (shouldStart()) {
                    state = State.TIMER;
                }
                break;
            case TIMER:
                for (int i = 0; i < timerTicks.getValue(); i++) {
                    mc.thePlayer.onUpdate();
                }
                yaw = mc.thePlayer.rotationYaw;
                pitch = mc.thePlayer.rotationPitch;
                motionX = mc.thePlayer.motionX;
                motionY = mc.thePlayer.motionY;
                motionZ = mc.thePlayer.motionZ;
                hasLag = 0;
                state = State.LAG;
                break;
            case LAG:
                if (hasLag >= lagTicks.getValue()) {
                    done();
                } else {
                    hasLag++;
                    mc.thePlayer.rotationYaw = yaw;
                    mc.thePlayer.rotationPitch = pitch;
                    mc.thePlayer.motionX = motionX;
                    mc.thePlayer.motionY = motionY;
                    mc.thePlayer.motionZ = motionZ;
                }
                break;
        }
    }

    @EventTarget(Priority.HIGH)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND) return;

        switch (state) {
            case TIMER:
                delayedPackets.add(event.getPacket());
                event.setCancelled(true);
                break;
            case LAG:
                if (event.getPacket() instanceof C03PacketPlayer) {
                    event.setCancelled(true);
                } else {
                    delayedPackets.add(event.getPacket());
                    event.setCancelled(true);
                }
                break;
        }
    }

    private void done() {
        State oldState = state;
        state = State.NONE;
        hasLag = 0;
        lastTimerTime = System.currentTimeMillis();

        for (Packet<?> packet : delayedPackets) {
            PacketUtil.sendPacket(packet);
        }
        delayedPackets.clear();

        if (oldState != State.NONE && mc.thePlayer != null) {
            if (clearMotion.getValue()) {
                mc.thePlayer.motionX = 0.0D;
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.motionZ = 0.0D;
            } else {
                mc.thePlayer.motionX = motionX;
                mc.thePlayer.motionY = motionY;
                mc.thePlayer.motionZ = motionZ;
            }
        }
    }

    private boolean shouldStart() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        Blink blink = (Blink) Myau.moduleManager.modules.get(Blink.class);
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (blink != null && blink.isEnabled()) return false;
        if (onlyOnGround.getValue() && !mc.thePlayer.onGround) return false;
        if (notWhileKB.getValue() && mc.thePlayer.hurtTime > 0) return false;
        if (notWhileScaffold.getValue() && scaffold != null && scaffold.isEnabled()) return false;
        if (!isMoving()) return false;
        if (fov.getValue() == 0) return false;
        if (lastTimerTime > 0 && System.currentTimeMillis() - lastTimerTime < delay.getValue()) return false;

        EntityPlayer target = mc.theWorld.playerEntities.stream()
                .filter(player -> player != mc.thePlayer)
                .filter(player -> !ignoreTeammates.getValue() || !TeamUtil.isSameTeam(player))
                .filter(player -> !TeamUtil.isFriend(player))
                .filter(player -> !TeamUtil.isBot(player))
                .min(Comparator.comparingDouble(player -> mc.thePlayer.getDistanceSqToEntity(player)))
                .orElse(null);

        if (target == null) return false;
        if (fov.getValue() < 360 && !isInFov(fov.getValue(), target)) return false;

        double distance = mc.thePlayer.getDistanceToEntity(target);
        return distance >= minRange.getValue() && distance <= maxRange.getValue();
    }

    private boolean isMoving() {
        return mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F;
    }

    private boolean isInFov(float fov, EntityPlayer target) {
        double diffX = target.posX - mc.thePlayer.posX;
        double diffZ = target.posZ - mc.thePlayer.posZ;
        float yawToTarget = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(yawToTarget - mc.thePlayer.rotationYaw));
        return yawDiff <= fov / 2.0F;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.valueOf(timerTicks.getValue())};
    }

    private enum State {
        NONE,
        TIMER,
        LAG
    }
}
