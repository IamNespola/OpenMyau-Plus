package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty latencyMin = new IntProperty("Latency-Min", 50, 10, 500);
    public final IntProperty latencyMax = new IntProperty("Latency-Max", 100, 50, 1000);
    public final FloatProperty distanceMin = new FloatProperty("Distance-Min", 0.0F, 0.0F, 6.0F);
    public final FloatProperty distanceMax = new FloatProperty("Distance-Max", 4.0F, 0.5F, 6.0F);
    public final ModeProperty espMode = new ModeProperty("ESP", 0, new String[]{"NONE", "BOX", "FILLED", "MODEL", "WIREFRAME"});
    public final ColorProperty espColor = new ColorProperty("Color", 0xFFFFFFFF);
    public final ModeProperty releaseStyle = new ModeProperty("Style", 0, new String[]{"PULSE", "SMOOTH"});
    public final BooleanProperty smart = new BooleanProperty("Smart", true);

    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final List<Packet<?>> skipPackets = new ArrayList<>();
    private final TimerUtil cycleTimer = new TimerUtil();
    private Vec3 vec3;
    private EntityPlayer target;
    private int currentLatency;

    public BackTrack() {
        super("BackTrack", false, true, "Queue entity packets with configurable latency");
    }

    @Override
    public void onEnabled() {
        packetQueue.clear();
        skipPackets.clear();
        vec3 = null;
        target = null;
        currentLatency = 0;
    }

    @Override
    public void onDisabled() {
        releaseAll();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        packetQueue.clear();
        skipPackets.clear();
        vec3 = null;
        target = null;
        currentLatency = 0;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null) return;

        if (target == null || vec3 == null) return;

        if (smart.getValue() && target.hurtTime <= 2) {
            double realDist = mc.thePlayer.getDistanceToEntity(target);
            double backtrackDist = mc.thePlayer.getDistance(vec3.xCoord, vec3.yCoord, vec3.zCoord);
            if (realDist + 0.5 < backtrackDist) {
                currentLatency = 0;
                releaseAll();
                target = null;
                vec3 = null;
                return;
            }
        }

        double distance = mc.thePlayer.getDistanceToEntity(target);
        if (distance < distanceMin.getValue() || distance > distanceMax.getValue()) {
            currentLatency = 0;
            releaseAll();
            target = null;
            vec3 = null;
        }

        if (releaseStyle.getValue() == 0) {
            if (!cycleTimer.hasTimeElapsed(currentLatency)) return;
            while (!packetQueue.isEmpty()) {
                try {
                    Packet<?> packet = packetQueue.remove().getPacket();
                    skipPackets.add(packet);
                    receivePacket(packet);
                } catch (NullPointerException ignored) {}
            }
            cycleTimer.reset();
            if (packetQueue.isEmpty() && target != null) {
                vec3 = target.getPositionVector();
            }
            return;
        }

        while (!packetQueue.isEmpty()) {
            try {
                if (packetQueue.element().timer.hasTimeElapsed(packetQueue.element().latency)) {
                    Packet<?> packet = packetQueue.remove().getPacket();
                    skipPackets.add(packet);
                    receivePacket(packet);
                } else {
                    break;
                }
            } catch (NullPointerException ignored) {}
        }

        if (packetQueue.isEmpty() && target != null) {
            vec3 = target.getPositionVector();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;
        if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 20) {
            packetQueue.clear();
            return;
        }

        Packet<?> packet = event.getPacket();
        if (skipPackets.contains(packet)) {
            skipPackets.remove(packet);
            return;
        }

        if (target == null) {
            releaseAll();
            return;
        }

        if (event.isCancelled()) return;

        if (packet instanceof S08PacketPlayerPosLook || packet instanceof S40PacketDisconnect) {
            releaseAll();
            target = null;
            vec3 = null;
            return;
        } else if (packet instanceof S13PacketDestroyEntities) {
            S13PacketDestroyEntities wrapper = (S13PacketDestroyEntities) packet;
            for (int id : wrapper.getEntityIDs()) {
                if (id == target.getEntityId()) {
                    target = null;
                    vec3 = null;
                    releaseAll();
                    return;
                }
            }
        }

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity s14 = (S14PacketEntity) packet;
            Entity e = s14.getEntity(mc.theWorld);
            if (e == null || e.getEntityId() != target.getEntityId()) return;
            vec3 = vec3.addVector(s14.func_149062_c() / 32.0D, s14.func_149061_d() / 32.0D, s14.func_149064_e() / 32.0D);
            packetQueue.add(new TimedPacket(packet, currentLatency));
            event.setCancelled(true);
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport s18 = (S18PacketEntityTeleport) packet;
            if (s18.getEntityId() != target.getEntityId()) return;
            if (packetQueue.size() >= 50) return;
            vec3 = new Vec3(s18.getX() / 32.0D, s18.getY() / 32.0D, s18.getZ() / 32.0D);
            packetQueue.add(new TimedPacket(packet, currentLatency));
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        Entity ent = event.getTarget();
        if (ent instanceof EntityPlayer) {
            if (target == null || ent != target) {
                vec3 = ent.getPositionVector();
            }
            target = (EntityPlayer) ent;

            double distance = mc.thePlayer.getDistanceToEntity(target);
            if (distance < distanceMin.getValue() || distance > distanceMax.getValue()) return;

            currentLatency = latencyMin.getValue() + new Random().nextInt(Math.max(1, latencyMax.getValue() - latencyMin.getValue()));
            cycleTimer.reset();
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (target == null || vec3 == null || target.isDead || currentLatency == 0) return;

        int mode = espMode.getValue();
        if (mode == 0) return;

        Color color = new Color(espColor.getValue());

        double x = vec3.xCoord - mc.getRenderManager().viewerPosX;
        double y = vec3.yCoord - mc.getRenderManager().viewerPosY;
        double z = vec3.zCoord - mc.getRenderManager().viewerPosZ;

        if (mode == 3) {
            double dx = vec3.xCoord - target.posX;
            double dy = vec3.yCoord - target.posY;
            double dz = vec3.zCoord - target.posZ;
            GlStateManager.pushMatrix();
            GlStateManager.translate(dx, dy, dz);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            mc.getRenderManager().renderEntityStatic(target, event.getPartialTicks(), false);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            return;
        }

        AxisAlignedBB playerBB = target.getEntityBoundingBox();
        double w = playerBB.maxX - playerBB.minX;
        double h = playerBB.maxY - playerBB.minY;

        AxisAlignedBB bb = new AxisAlignedBB(
                x - w / 2, y, z - w / 2,
                x + w / 2, y + h, z + w / 2
        );

        GlStateManager.pushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);

        switch (mode) {
            case 1:
                GL11.glLineWidth(2.0F);
                RenderGlobal.drawOutlinedBoundingBox(bb, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
                break;
            case 2:
                RenderGlobal.drawOutlinedBoundingBox(bb, color.getRed(), color.getGreen(), color.getBlue(), 63);
                break;
            case 4:
                GL11.glLineWidth(2.0F);
                RenderGlobal.drawOutlinedBoundingBox(bb, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
                break;
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glLineWidth(1.0F);
        GlStateManager.popMatrix();
    }

    private void releaseAll() {
        if (!packetQueue.isEmpty()) {
            for (TimedPacket tp : packetQueue) {
                Packet<?> packet = tp.getPacket();
                skipPackets.add(packet);
                receivePacket(packet);
            }
            packetQueue.clear();
        }
    }

    private void receivePacket(Packet<?> packet) {
        if (packet == null) return;
        try {
            ((Packet<INetHandlerPlayClient>) packet).processPacket(mc.getNetHandler());
        } catch (ThreadQuickExitException ignored) {}
    }

    private static class TimedPacket {
        private final Packet<?> packet;
        private final TimerUtil timer;
        private final int latency;

        TimedPacket(Packet<?> packet, int latency) {
            this.packet = packet;
            this.timer = new TimerUtil();
            this.latency = Math.max(latency, 1);
        }

        Packet<?> getPacket() { return packet; }
    }
}
