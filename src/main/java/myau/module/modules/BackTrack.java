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
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import myau.util.RandomUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Packet", "FakePlayer"});
    public final IntProperty nextBacktrackDelay = new IntProperty("NextBacktrackDelay", 0, 0, 2000, () -> mode.getValue() == 0);
    public final IntProperty latencyMin = new IntProperty("MinDelay", 80, 0, 2000, () -> mode.getValue() == 0);
    public final IntProperty latencyMax = new IntProperty("MaxDelay", 80, 0, 2000, () -> mode.getValue() == 0);
    public final FloatProperty distanceMin = new FloatProperty("Distance-Min", 2.0F, 0.0F, 6.0F, () -> mode.getValue() == 0);
    public final FloatProperty distanceMax = new FloatProperty("Distance-Max", 3.0F, 0.0F, 6.0F, () -> mode.getValue() == 0);
    public final IntProperty maxPackets = new IntProperty("MaxPackets", 50, 1, 200, () -> mode.getValue() == 0);
    public final ModeProperty releaseStyle = new ModeProperty("Style", 1, new String[]{"PULSE", "SMOOTH"}, () -> mode.getValue() == 0);
    public final BooleanProperty smart = new BooleanProperty("Smart", true, () -> mode.getValue() == 0);
    public final ModeProperty espMode = new ModeProperty("ESP", 2, new String[]{"NONE", "BOX", "FILLED", "MODEL", "WIREFRAME"});
    public final ColorProperty espColor = new ColorProperty("Color", 0xFFFFFFFF);
    public final BooleanProperty pauseOnHurt = new BooleanProperty("PauseOnHurt", false, () -> mode.getValue() == 0);
    public final IntProperty pauseHurtTime = new IntProperty("PauseHurtTime", 3, 0, 10, () -> mode.getValue() == 0 && pauseOnHurt.getValue());
    public final IntProperty fakePlayerPulseDelay = new IntProperty("FakePlayer-PulseDelay", 200, 50, 500, () -> mode.getValue() == 1);
    public final IntProperty fakePlayerIntavePackets = new IntProperty("FakePlayer-IntavePackets", 5, 1, 30, () -> mode.getValue() == 1);

    private final Queue<QueuedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final Queue<TimedPosition> positions = new ConcurrentLinkedQueue<>();
    private final List<Packet<?>> skipPackets = new ArrayList<>();
    private final TimerUtil cycleTimer = new TimerUtil();

    private static final int MAX_POSITIONS_SIZE = 100;
    private static final String[] NON_DELAYED_SOUND_SUBSTRINGS = new String[]{"game.player.hurt", "game.player.die"};

    private Vec3 trackedPosition = zeroVec();
    private EntityPlayer target;
    private int currentLatency;
    private long delayForNextBacktrack;
    private boolean shouldRender;
    private boolean ignoreWholeTick;
    private EntityOtherPlayerMP fakePlayer;
    private EntityLivingBase currentTarget;
    private boolean fakeShown;
    private final TimerUtil fakePulseTimer = new TimerUtil();

    public BackTrack() {
        super("BackTrack", false, false, "Queue entity packets with configurable latency");
    }

    @Override
    public void onEnabled() {
        clear(false, true, false, true);
        currentLatency = randomLatency();
        delayForNextBacktrack = 0L;
    }

    @Override
    public void onDisabled() {
        clear(true, false, false, true);
        removeFakePlayer();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        clear(false, true, false, true);
        removeFakePlayer();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.ticksExisted <= 10) {
            clear(false, true, false, true);
            removeFakePlayer();
            return;
        }

        if (mode.getValue() == 1) {
            updateFakePlayer();
            return;
        }

        if (shouldBacktrack() && target != null) {
            shouldRender = true;

            double trackedDistance = mc.thePlayer.getDistance(trackedPosition.xCoord, trackedPosition.yCoord, trackedPosition.zCoord);
            double realDistance = mc.thePlayer.getDistanceToEntity(target);
            boolean inDelayWindow = releaseStyle.getValue() == 1 || !cycleTimer.hasTimeElapsed(currentLatency);

            if (trackedDistance <= 6.0D && (!smart.getValue() || trackedDistance >= realDistance) && inDelayWindow) {
                if (isInConfiguredDistance(realDistance)) {
                    handlePackets();
                } else {
                    handlePacketsRange();
                }
            } else {
                clear(true, false, true, false);
            }
        } else {
            clear(true, false, true, false);
        }

        if (packetQueue.isEmpty()) {
            currentLatency = randomLatency();
            if (System.currentTimeMillis() >= delayForNextBacktrack && !shouldBacktrack()) {
                delayForNextBacktrack = System.currentTimeMillis() + nextBacktrackDelay.getValue();
            }
        }

        ignoreWholeTick = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mode.getValue() != 0 || event.getType() != EventType.RECEIVE || event.isCancelled()) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.ticksExisted <= 10) {
            clear(false, true, false, true);
            return;
        }

        Packet<?> packet = event.getPacket();
        if (skipPackets.remove(packet)) return;

        if (packetQueue.isEmpty() && target == null && !shouldBacktrack()) return;

        if (isIgnoredPacket(packet)) return;

        if (isFlushPacket(packet)) {
            clear(true, false, true, true);
            return;
        }

        if (target == null || !shouldBacktrack()) return;

        Vec3 nextPosition = predictPosition(packet, target, trackedPosition);
        if (nextPosition != null) {
            rememberPosition(trackedPosition);
            trackedPosition = nextPosition;
        }

        event.setCancelled(true);
        if (packetQueue.size() >= maxPackets.getValue()) {
            releaseOldestPacket();
        }
        packetQueue.add(new QueuedPacket(packet, System.currentTimeMillis()));
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) return;
        if (mode.getValue() == 1) {
            handleFakePlayerAttack(event);
            return;
        }

        if (!(event.getTarget() instanceof EntityPlayer)) return;
        EntityPlayer attacked = (EntityPlayer) event.getTarget();
        if (attacked == target) return;

        clear(true, false, false, true);
        target = attacked;
        trackedPosition = attacked.getPositionVector();
        rememberPosition(trackedPosition);
        currentLatency = randomLatency();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mode.getValue() != 0 || !shouldRender || target == null || trackedPosition == null || target.isDead || currentLatency <= 0) return;
        if (espMode.getValue() == 0) return;

        Color color = new Color(espColor.getValue(), true);
        double x = trackedPosition.xCoord - mc.getRenderManager().viewerPosX;
        double y = trackedPosition.yCoord - mc.getRenderManager().viewerPosY;
        double z = trackedPosition.zCoord - mc.getRenderManager().viewerPosZ;

        if (espMode.getValue() == 3) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(trackedPosition.xCoord - target.posX, trackedPosition.yCoord - target.posY, trackedPosition.zCoord - target.posZ);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            mc.getRenderManager().renderEntityStatic(target, event.getPartialTicks(), false);
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
            return;
        }

        AxisAlignedBB playerBB = target.getEntityBoundingBox();
        double width = playerBB.maxX - playerBB.minX;
        double height = playerBB.maxY - playerBB.minY;
        AxisAlignedBB bb = new AxisAlignedBB(x - width / 2.0, y, z - width / 2.0, x + width / 2.0, y + height, z + width / 2.0);

        GlStateManager.pushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDepthMask(false);

        if (espMode.getValue() == 2) {
            RenderGlobal.drawOutlinedBoundingBox(bb, color.getRed(), color.getGreen(), color.getBlue(), 80);
        }
        GL11.glLineWidth(espMode.getValue() == 4 ? 2.5F : 2.0F);
        RenderGlobal.drawOutlinedBoundingBox(bb, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDepthMask(true);
        GL11.glLineWidth(1.0F);
        GlStateManager.popMatrix();
    }

    private boolean shouldBacktrack() {
        return mc.thePlayer != null
                && mc.theWorld != null
                && target != null
                && mc.thePlayer.getHealth() > 0.0F
                && (target.getHealth() > 0.0F || Float.isNaN(target.getHealth()))
                && mc.playerController.getCurrentGameType() != WorldSettings.GameType.SPECTATOR
                && System.currentTimeMillis() >= delayForNextBacktrack
                && !target.isDead
                && target != mc.thePlayer
                && !ignoreWholeTick
                && (!pauseOnHurt.getValue() || target.hurtTime < pauseHurtTime.getValue());
    }

    private boolean isInConfiguredDistance(double distance) {
        return distance >= Math.min(distanceMin.getValue(), distanceMax.getValue())
                && distance <= Math.max(distanceMin.getValue(), distanceMax.getValue());
    }

    private boolean isIgnoredPacket(Packet<?> packet) {
        if (packet instanceof S02PacketChat) return true;
        if (packet instanceof S19PacketEntityStatus && target != null && ((S19PacketEntityStatus) packet).getEntity(mc.theWorld) == target) return true;
        if (packet instanceof S29PacketSoundEffect) {
            String soundName = ((S29PacketSoundEffect) packet).getSoundName();
            for (String ignored : NON_DELAYED_SOUND_SUBSTRINGS) {
                if (soundName != null && soundName.contains(ignored)) return true;
            }
        }
        return false;
    }

    private boolean isFlushPacket(Packet<?> packet) {
        if (packet instanceof S08PacketPlayerPosLook || packet instanceof S40PacketDisconnect) return true;
        if (packet instanceof S06PacketUpdateHealth && ((S06PacketUpdateHealth) packet).getHealth() <= 0.0F) return true;
        if (packet instanceof S13PacketDestroyEntities && target != null) {
            for (int id : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                if (id == target.getEntityId()) return true;
            }
        }
        if (packet instanceof S1CPacketEntityMetadata && target != null && ((S1CPacketEntityMetadata) packet).getEntityId() == target.getEntityId()) {
            if (isDeadMetadata((S1CPacketEntityMetadata) packet)) return true;
        }
        return false;
    }

    private boolean isDeadMetadata(S1CPacketEntityMetadata packet) {
        if (packet.func_149376_c() == null) return false;
        for (Object watchedObject : packet.func_149376_c()) {
            if (!(watchedObject instanceof net.minecraft.entity.DataWatcher.WatchableObject)) continue;
            net.minecraft.entity.DataWatcher.WatchableObject data = (net.minecraft.entity.DataWatcher.WatchableObject) watchedObject;
            if (data.getDataValueId() != 6 || data.getObject() == null) continue;
            try {
                double value = Double.parseDouble(data.getObject().toString());
                if (!Double.isNaN(value) && value <= 0.0D) return true;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    private Vec3 predictPosition(Packet<?> packet, EntityPlayer entity, Vec3 base) {
        if (base == null) base = entity.getPositionVector();
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            if (p.getEntity(mc.theWorld) == null || p.getEntity(mc.theWorld).getEntityId() != entity.getEntityId()) return null;
            return base.addVector(p.func_149062_c() / 32.0D, p.func_149061_d() / 32.0D, p.func_149064_e() / 32.0D);
        }
        if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            if (p.getEntityId() != entity.getEntityId()) return null;
            return new Vec3(p.getX() / 32.0D, p.getY() / 32.0D, p.getZ() / 32.0D);
        }
        return null;
    }

    private void rememberPosition(Vec3 position) {
        if (position == null) return;
        while (positions.size() >= MAX_POSITIONS_SIZE) {
            positions.poll();
        }
        positions.add(new TimedPosition(position, System.currentTimeMillis()));
    }

    private void handlePackets() {
        if (releaseStyle.getValue() == 0) {
            if (!cycleTimer.hasTimeElapsed(currentLatency)) return;
            releaseAll();
            cycleTimer.reset();
            return;
        }

        long expireTime = System.currentTimeMillis() - currentLatency;
        while (!packetQueue.isEmpty()) {
            QueuedPacket queuedPacket = packetQueue.peek();
            if (queuedPacket == null || queuedPacket.time > expireTime) break;
            packetQueue.poll();
            receiveQueuedPacket(queuedPacket.packet);
        }
        positions.removeIf(position -> position.time < expireTime);
    }

    private void handlePacketsRange() {
        long time = getRangeTime();
        if (time == -1L) {
            clear(true, false, true, false);
            return;
        }
        while (!packetQueue.isEmpty()) {
            QueuedPacket queuedPacket = packetQueue.peek();
            if (queuedPacket == null || queuedPacket.time > time) break;
            packetQueue.poll();
            receiveQueuedPacket(queuedPacket.packet);
        }
        positions.removeIf(position -> position.time < time);
    }

    private long getRangeTime() {
        if (target == null) return 0L;
        long time = 0L;
        boolean found = false;
        for (TimedPosition data : positions) {
            time = data.time;
            if (isInConfiguredDistance(mc.thePlayer.getDistance(data.position.xCoord, data.position.yCoord, data.position.zCoord))) {
                found = true;
                break;
            }
        }
        return found ? time : -1L;
    }

    private void releaseOldestPacket() {
        QueuedPacket queuedPacket = packetQueue.poll();
        if (queuedPacket != null) receiveQueuedPacket(queuedPacket.packet);
    }

    private void releaseAll() {
        while (!packetQueue.isEmpty()) {
            QueuedPacket queuedPacket = packetQueue.poll();
            if (queuedPacket != null) receiveQueuedPacket(queuedPacket.packet);
        }
        if (target != null) trackedPosition = target.getPositionVector();
    }

    private void clear(boolean handlePackets, boolean clearOnly, boolean applyCooldown, boolean clearTarget) {
        if (handlePackets && !clearOnly) {
            releaseAll();
        } else if (clearOnly) {
            packetQueue.clear();
        }
        positions.clear();
        if (applyCooldown && target != null) {
            delayForNextBacktrack = System.currentTimeMillis() + nextBacktrackDelay.getValue();
        }
        if (clearTarget) {
            target = null;
            trackedPosition = zeroVec();
        }
        shouldRender = false;
        ignoreWholeTick = true;
        currentLatency = randomLatency();
        cycleTimer.reset();
    }

    private void receiveQueuedPacket(Packet<?> packet) {
        if (packet == null || mc.getNetHandler() == null) return;
        skipPackets.add(packet);
        PacketUtil.handlePacket((Packet<INetHandlerPlayClient>) packet);
    }

    private int randomLatency() {
        return randomInt(latencyMin.getValue(), latencyMax.getValue());
    }

    private static int randomInt(int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return RandomUtil.nextInt(low, high);
    }

    private static Vec3 zeroVec() {
        return new Vec3(0.0D, 0.0D, 0.0D);
    }

    public Vec3 getTrackedPositionForDebug(EntityLivingBase entity) {
        if (!this.isEnabled() || mode.getValue() != 0 || entity == null || target == null || trackedPosition == null) return null;
        if (entity.getEntityId() != target.getEntityId()) return null;
        if (trackedPosition.xCoord == 0.0D && trackedPosition.yCoord == 0.0D && trackedPosition.zCoord == 0.0D) return null;
        return trackedPosition;
    }

    private void attackRealTarget(EntityLivingBase entity) {
        if (entity == null || mc.thePlayer == null || mc.getNetHandler() == null) return;
        mc.thePlayer.swingItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK));
        if (mc.playerController != null) {
            mc.thePlayer.attackTargetEntityWithCurrentItem(entity);
        }
    }

    private void createFakePlayer(EntityLivingBase target) {
        if (mc.theWorld == null || mc.getNetHandler() == null || !(target instanceof EntityPlayer)) return;
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(target.getUniqueID());
        if (playerInfo == null) return;

        EntityOtherPlayerMP faker = new EntityOtherPlayerMP(mc.theWorld, playerInfo.getGameProfile());
        faker.rotationYawHead = target.rotationYawHead;
        faker.renderYawOffset = target.renderYawOffset;
        faker.copyLocationAndAnglesFrom(target);
        faker.setHealth(target.getHealth());
        copyEquipment(target, faker);
        mc.theWorld.addEntityToWorld(-1337, faker);
        fakePlayer = faker;
        fakeShown = true;
    }

    private void removeFakePlayer() {
        if (fakePlayer != null && mc.theWorld != null) {
            mc.theWorld.removeEntity(fakePlayer);
        }
        fakePlayer = null;
        currentTarget = null;
        fakeShown = false;
    }

    private void handleFakePlayerAttack(AttackEvent event) {
        if (!(event.getTarget() instanceof EntityLivingBase)) return;
        EntityLivingBase attacked = (EntityLivingBase) event.getTarget();

        if (fakePlayer != null && attacked.getEntityId() == fakePlayer.getEntityId()) {
            attackRealTarget(currentTarget);
            event.setCancelled(true);
            return;
        }

        if (attacked == mc.thePlayer) return;
        if (fakePlayer == null || attacked != currentTarget) {
            removeFakePlayer();
            currentTarget = attacked;
            createFakePlayer(attacked);
            fakePulseTimer.reset();
        }
    }

    private void updateFakePlayer() {
        if (currentTarget == null || fakePlayer == null) {
            if (!fakeShown && currentTarget != null) createFakePlayer(currentTarget);
            return;
        }

        if (currentTarget.isDead || !currentTarget.isEntityAlive() || !fakePlayer.isEntityAlive()) {
            removeFakePlayer();
            return;
        }

        fakePlayer.setHealth(currentTarget.getHealth());
        copyEquipment(currentTarget, fakePlayer);

        boolean shouldPulse = mc.thePlayer.ticksExisted % Math.max(fakePlayerIntavePackets.getValue(), 1) == 0
                || fakePulseTimer.hasTimeElapsed(fakePlayerPulseDelay.getValue());
        if (shouldPulse) {
            fakePlayer.rotationYawHead = currentTarget.rotationYawHead;
            fakePlayer.renderYawOffset = currentTarget.renderYawOffset;
            fakePlayer.copyLocationAndAnglesFrom(currentTarget);
            fakePulseTimer.reset();
        }
    }

    private void copyEquipment(EntityLivingBase source, EntityLivingBase destination) {
        for (int index = 0; index <= 4; index++) {
            ItemStack stack = source.getEquipmentInSlot(index);
            destination.setCurrentItemOrArmor(index, stack == null ? null : stack.copy());
        }
    }

    private static class QueuedPacket {
        private final Packet<?> packet;
        private final long time;

        QueuedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }

    private static class TimedPosition {
        private final Vec3 position;
        private final long time;

        TimedPosition(Vec3 position, long time) {
            this.position = position;
            this.time = time;
        }
    }
}
