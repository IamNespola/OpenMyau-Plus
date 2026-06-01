package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;

public class FakeLag extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty delay = new IntProperty("delay", 550, 0, 1000);
    public final IntProperty recoilTime = new IntProperty("recoil-time", 750, 0, 2000);
    public final FloatProperty minAllowedDistToEnemy = new FloatProperty("min-allowed-dist", 1.5F, 0.0F, 6.0F);
    public final FloatProperty maxAllowedDistToEnemy = new FloatProperty("max-allowed-dist", 3.5F, 0.0F, 6.0F);
    public final BooleanProperty blinkOnAction = new BooleanProperty("blink-on-action", true);
    public final BooleanProperty pauseOnNoMove = new BooleanProperty("pause-on-no-move", true);
    public final BooleanProperty pauseOnChest = new BooleanProperty("pause-on-chest", false);
    public final BooleanProperty line = new BooleanProperty("line", true);
    public final ColorProperty lineColor = new ColorProperty("line-color", Color.GREEN.getRGB(), () -> line.getValue());

    private final LinkedList<QueueData> packetQueue = new LinkedList<>();
    private final LinkedList<PositionData> positions = new LinkedList<>();

    private long resetTimer;
    private boolean wasNearEnemy;
    private boolean ignoreWholeTick;

    public FakeLag() {
        super("FakeLag", false);
    }

    @Override
    public void onEnabled() {
        this.clearPackets();
        this.resetTimer = System.currentTimeMillis();
        this.wasNearEnemy = false;
        this.ignoreWholeTick = false;
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null) {
            this.blink(true);
        } else {
            this.clearPackets();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.isCancelled()) return;
        if (this.ignoreWholeTick || this.wasNearEnemy) return;

        Packet<?> packet = event.getPacket();
        if (this.isIgnoredPacket(packet)) return;

        if (this.pauseOnNoMove.getValue() && !this.isMoving()) {
            this.blink(true);
            return;
        }

        if (mc.thePlayer.hurtTime != 0 && mc.thePlayer.getHealth() < mc.thePlayer.getMaxHealth()) {
            this.blink(true);
            return;
        }

        if (this.blinkOnAction.getValue() && packet instanceof C02PacketUseEntity) {
            this.blink(true);
            return;
        }

        if (this.pauseOnChest.getValue() && mc.currentScreen instanceof GuiContainer) {
            this.blink(true);
            return;
        }

        if (this.shouldFlush(packet)) {
            this.blink(true);
            return;
        }

        if (!this.hasTimePassed(this.resetTimer, this.recoilTime.getValue())) return;
        if (mc.isSingleplayer() || mc.getCurrentServerData() == null) {
            this.blink(true);
            return;
        }

        if (event.getType() == EventType.SEND) {
            event.setCancelled(true);
            this.packetQueue.add(new QueueData(packet, System.currentTimeMillis()));

            if (packet instanceof C03PacketPlayer) {
                C03PacketPlayer playerPacket = (C03PacketPlayer) packet;
                if (playerPacket.isMoving()) {
                    this.positions.add(new PositionData(new Vec3(playerPacket.getPositionX(), playerPacket.getPositionY(), playerPacket.getPositionZ()), System.currentTimeMillis()));
                }
            }
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.blink(false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || mc.thePlayer == null || mc.theWorld == null) return;

        this.checkEnemyDistance();

        if (mc.thePlayer.isDead || mc.thePlayer.isUsingItem()) {
            this.blink(true);
            return;
        }

        if (!this.hasTimePassed(this.resetTimer, this.recoilTime.getValue())) return;

        this.handlePackets(false);
        this.ignoreWholeTick = false;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || !this.line.getValue() || this.positions.size() < 2) return;

        Color color = new Color(this.lineColor.getValue(), true);
        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (PositionData position : this.positions) {
            GL11.glVertex3d(position.pos.xCoord - renderX, position.pos.yCoord - renderY, position.pos.zCoord - renderZ);
        }
        GL11.glEnd();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private boolean shouldFlush(Packet<?> packet) {
        if (packet instanceof C0EPacketClickWindow || packet instanceof C0DPacketCloseWindow) return true;
        if (packet instanceof S08PacketPlayerPosLook || packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C07PacketPlayerDigging || packet instanceof C12PacketUpdateSign || packet instanceof C19PacketResourcePackStatus) return true;
        if (packet instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) return true;
        return packet instanceof S27PacketExplosion && (((S27PacketExplosion) packet).func_149149_c() != 0.0F || ((S27PacketExplosion) packet).func_149144_d() != 0.0F || ((S27PacketExplosion) packet).func_149147_e() != 0.0F);
    }

    private boolean isIgnoredPacket(Packet<?> packet) {
        return packet instanceof C00Handshake
                || packet instanceof C00PacketServerQuery
                || packet instanceof C01PacketPing
                || packet instanceof C01PacketChatMessage
                || packet instanceof C00PacketKeepAlive
                || packet instanceof S01PacketPong;
    }

    private void checkEnemyDistance() {
        if (this.maxAllowedDistToEnemy.getValue() <= 0.0F || this.positions.isEmpty()) {
            this.wasNearEnemy = false;
            return;
        }

        Vec3 serverPos = this.positions.getFirst().pos;
        float min = Math.min(this.minAllowedDistToEnemy.getValue(), this.maxAllowedDistToEnemy.getValue());
        float max = Math.max(this.minAllowedDistToEnemy.getValue(), this.maxAllowedDistToEnemy.getValue());
        this.wasNearEnemy = false;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead) continue;

            double distance = player.getDistance(serverPos.xCoord, serverPos.yCoord, serverPos.zCoord);
            if (distance >= min && distance <= max) {
                this.blink(true);
                this.wasNearEnemy = true;
                return;
            }
        }
    }

    private void blink(boolean handlePackets) {
        if (handlePackets) {
            this.resetTimer = System.currentTimeMillis();
        }
        this.handlePackets(true);
        this.ignoreWholeTick = true;
    }

    private void handlePackets(boolean clear) {
        long now = System.currentTimeMillis();
        Iterator<QueueData> packetIterator = this.packetQueue.iterator();
        while (packetIterator.hasNext()) {
            QueueData data = packetIterator.next();
            if (clear || data.time <= now - this.delay.getValue()) {
                PacketUtil.sendPacketNoEvent(data.packet);
                packetIterator.remove();
            }
        }

        Iterator<PositionData> positionIterator = this.positions.iterator();
        while (positionIterator.hasNext()) {
            PositionData data = positionIterator.next();
            if (clear || data.time <= now - this.delay.getValue()) {
                positionIterator.remove();
            }
        }
    }

    private void clearPackets() {
        this.packetQueue.clear();
        this.positions.clear();
    }

    private boolean hasTimePassed(long timer, int delay) {
        return System.currentTimeMillis() - timer >= delay;
    }

    private boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F);
    }

    public Vec3 getServerPositionForDebug() {
        if (!this.isEnabled() || this.positions.isEmpty()) return null;
        return this.positions.getFirst().pos;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.valueOf(this.packetQueue.size())};
    }

    private static class QueueData {
        private final Packet<?> packet;
        private final long time;

        private QueueData(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    private static class PositionData {
        private final Vec3 pos;
        private final long time;

        private PositionData(Vec3 pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }
}