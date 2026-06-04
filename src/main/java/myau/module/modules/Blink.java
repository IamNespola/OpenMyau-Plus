package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Blink extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"NORMAL", "FAKE_LAG"});

    public final BooleanProperty normalPulse = new BooleanProperty("normal-pulse", false, () -> mode.getValue() == 0);
    public final IntProperty normalPulseDelay = new IntProperty("normal-pulse-delay", 1000, 0, 10000, () -> mode.getValue() == 0 && normalPulse.getValue());
    public final BooleanProperty normalInitialPosition = new BooleanProperty("normal-initial-position", false, () -> mode.getValue() == 0);
    public final BooleanProperty normalOverlay = new BooleanProperty("normal-overlay", false, () -> mode.getValue() == 0);

    public final IntProperty fakeLagMaxBlinkTime = new IntProperty("fakelag-max-blink-time", 20000, 1000, 30000, () -> mode.getValue() == 1);
    public final BooleanProperty fakeLagSlowRelease = new BooleanProperty("fakelag-slow-release", false, () -> mode.getValue() == 1);
    public final FloatProperty fakeLagReleaseSpeed = new FloatProperty("fakelag-release-speed", 2.0F, 2.0F, 10.0F, () -> mode.getValue() == 1 && fakeLagSlowRelease.getValue());
    public final BooleanProperty fakeLagAntiAim = new BooleanProperty("fakelag-anti-aim", true, () -> mode.getValue() == 1);
    public final BooleanProperty fakeLagDrawRealPosition = new BooleanProperty("fakelag-draw-real-position", true, () -> mode.getValue() == 1);

    private final List<Packet<?>> normalPackets = new ArrayList<>();
    private final Queue<TimedPacket> fakeLagPackets = new ConcurrentLinkedQueue<>();
    private Vec3 initialPosition;
    private Vec3 realPosition;
    private long startTime = -1L;
    private long stopTime = -1L;
    private long blinkedTime = 0L;
    private boolean releasingFakeLag;

    public Blink() {
        super("Blink", false);
    }

    @Override
    public void onEnabled() {
        start();
    }

    @Override
    public void onDisabled() {
        if (mode.getValue() == 1 && !releasingFakeLag && !fakeLagPackets.isEmpty()) {
            releasingFakeLag = true;
            stopTime = System.currentTimeMillis();
            return;
        }
        flushAll();
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND || mc.thePlayer == null || mc.theWorld == null) return;
        Packet<?> packet = event.getPacket();
        if (isIgnoredPacket(packet) || event.isCancelled()) return;

        if (mode.getValue() == 0) {
            synchronized (normalPackets) {
                normalPackets.add(packet);
            }
            event.setCancelled(true);
            if (normalPulse.getValue() && System.currentTimeMillis() - startTime >= normalPulseDelay.getValue()) {
                flushNormal();
                start();
            }
        } else {
            fakeLagPackets.add(new TimedPacket(packet, System.currentTimeMillis()));
            event.setCancelled(true);
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) {
            flushAll();
            super.setEnabled(false);
            return;
        }

        if (mode.getValue() == 1) {
            if (releasingFakeLag) {
                sendFakeLagPackets(false);
                if (fakeLagPackets.isEmpty()) {
                    releasingFakeLag = false;
                    super.setEnabled(false);
                }
            } else {
                blinkedTime = Math.min(System.currentTimeMillis() - startTime, fakeLagMaxBlinkTime.getValue());
                sendFakeLagPackets(true);
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (mode.getValue() == 0 && normalInitialPosition.getValue() && initialPosition != null) {
            drawBox(initialPosition);
        } else if (mode.getValue() == 1 && fakeLagDrawRealPosition.getValue() && realPosition != null) {
            drawBox(realPosition);
        }
    }

    @EventTarget
    public void onRenderOverlay(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mode.getValue() != 0 || !normalOverlay.getValue()) return;
        ScaledResolution sr = new ScaledResolution(mc);
        String text = "blinking: " + normalPackets.size();
        int width = mc.fontRendererObj.getStringWidth(text) + 10;
        int x = sr.getScaledWidth() / 2 - width / 2;
        int y = sr.getScaledHeight() / 2 + 18;
        Gui.drawRect(x, y, x + width, y + 14, 0x99000000);
        mc.fontRendererObj.drawStringWithShadow(text, x + 5, y + 3, 0xFFFFFFFF);
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        flushAll();
        this.setEnabled(false);
    }

    private void start() {
        normalPackets.clear();
        fakeLagPackets.clear();
        initialPosition = mc.thePlayer == null ? null : new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        realPosition = initialPosition;
        startTime = System.currentTimeMillis();
        stopTime = -1L;
        blinkedTime = 0L;
        releasingFakeLag = false;
    }

    private void flushAll() {
        flushNormal();
        while (!fakeLagPackets.isEmpty()) {
            TimedPacket timedPacket = fakeLagPackets.poll();
            if (timedPacket != null) {
                updateRealPosition(timedPacket.packet);
                PacketUtil.sendPacketNoEvent(timedPacket.packet);
            }
        }
        initialPosition = null;
        realPosition = null;
        releasingFakeLag = false;
    }

    private void flushNormal() {
        synchronized (normalPackets) {
            for (Packet<?> packet : normalPackets) {
                PacketUtil.sendPacketNoEvent(packet);
            }
            normalPackets.clear();
        }
    }

    private void sendFakeLagPackets(boolean delay) {
        try {
            while (!fakeLagPackets.isEmpty()) {
                TimedPacket next = fakeLagPackets.peek();
                if (next == null) break;

                boolean shouldSend;
                if (delay && !(fakeLagAntiAim.getValue() && shouldAntiAim())) {
                    shouldSend = System.currentTimeMillis() - next.time >= fakeLagMaxBlinkTime.getValue();
                } else if (fakeLagSlowRelease.getValue() && releasingFakeLag) {
                    double releaseWindow = fakeLagMaxBlinkTime.getValue() / Math.max(0.1D, fakeLagReleaseSpeed.getValue());
                    double releaseProgress = Math.min((System.currentTimeMillis() - stopTime) / releaseWindow, 1.0D);
                    long keepTime = (long) (fakeLagMaxBlinkTime.getValue() * Math.max(0.0D, (blinkedTime / (double) fakeLagMaxBlinkTime.getValue()) - releaseProgress));
                    shouldSend = System.currentTimeMillis() - next.time >= keepTime;
                } else {
                    shouldSend = true;
                }

                if (!shouldSend) break;
                TimedPacket timedPacket = fakeLagPackets.poll();
                if (timedPacket == null || timedPacket.packet == null) continue;
                updateRealPosition(timedPacket.packet);
                PacketUtil.sendPacketNoEvent(timedPacket.packet);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean shouldAntiAim() {
        if (realPosition == null || mc.theWorld == null || mc.thePlayer == null) return false;
        return mc.theWorld.playerEntities.stream()
                .filter(target -> target != mc.thePlayer)
                .filter(target -> !TeamUtil.isBot(target))
                .filter(target -> !TeamUtil.isSameTeam(target))
                .anyMatch(target -> target.getDistance(realPosition.xCoord, realPosition.yCoord, realPosition.zCoord) < 5.0D
                        && isInFov(target, realPosition, 120.0F));
    }

    private boolean isInFov(EntityPlayer target, Vec3 position, float fov) {
        double diffX = position.xCoord - target.posX;
        double diffZ = position.zCoord - target.posZ;
        float yawToPos = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D);
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(yawToPos - target.getRotationYawHead()));
        return yawDiff <= fov / 2.0F;
    }

    private void updateRealPosition(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer playerPacket = (C03PacketPlayer) packet;
            realPosition = new Vec3(playerPacket.getPositionX(), playerPacket.getPositionY(), playerPacket.getPositionZ());
        }
    }

    private boolean isIgnoredPacket(Packet<?> packet) {
        return packet instanceof C00Handshake
                || packet instanceof C00PacketLoginStart
                || packet instanceof C00PacketServerQuery
                || packet instanceof C01PacketEncryptionResponse
                || packet instanceof C01PacketChatMessage;
    }

    public static void drawBox(Vec3 position) {
        if (position == null || mc.thePlayer == null) return;
        double x = position.xCoord - mc.getRenderManager().viewerPosX;
        double y = position.yCoord - mc.getRenderManager().viewerPosY;
        double z = position.zCoord - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB bb = new AxisAlignedBB(x - 0.3D, y, z - 0.3D, x + 0.3D, y + 1.8D, z + 0.3D);

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2.0F);
        GlStateManager.color(0.45F, 0.85F, 1.0F, 0.75F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        addBoxLines(wr, bb);
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void addBoxLines(WorldRenderer wr, AxisAlignedBB bb) {
        double x1 = bb.minX, y1 = bb.minY, z1 = bb.minZ;
        double x2 = bb.maxX, y2 = bb.maxY, z2 = bb.maxZ;
        line(wr, x1, y1, z1, x2, y1, z1); line(wr, x2, y1, z1, x2, y1, z2);
        line(wr, x2, y1, z2, x1, y1, z2); line(wr, x1, y1, z2, x1, y1, z1);
        line(wr, x1, y2, z1, x2, y2, z1); line(wr, x2, y2, z1, x2, y2, z2);
        line(wr, x2, y2, z2, x1, y2, z2); line(wr, x1, y2, z2, x1, y2, z1);
        line(wr, x1, y1, z1, x1, y2, z1); line(wr, x2, y1, z1, x2, y2, z1);
        line(wr, x2, y1, z2, x2, y2, z2); line(wr, x1, y1, z2, x1, y2, z2);
    }

    private static void line(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2) {
        wr.pos(x1, y1, z1).endVertex();
        wr.pos(x2, y2, z2).endVertex();
    }

    @Override
    public String[] getSuffix() {
        int size = mode.getValue() == 0 ? normalPackets.size() : fakeLagPackets.size();
        return new String[]{mode.getModeString() + " " + size};
    }

    private static class TimedPacket {
        private final Packet<?> packet;
        private final long time;

        private TimedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }
}
