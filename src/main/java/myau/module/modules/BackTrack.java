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
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
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
    public final IntProperty latencyMin = new IntProperty("Latency-Min", 100, 0, 1000, () -> mode.getValue() == 0);
    public final IntProperty latencyMax = new IntProperty("Latency-Max", 150, 0, 1000, () -> mode.getValue() == 0);
    public final IntProperty nextDelayMin = new IntProperty("NextDelay-Min", 0, 0, 2000, () -> mode.getValue() == 0);
    public final IntProperty nextDelayMax = new IntProperty("NextDelay-Max", 10, 0, 2000, () -> mode.getValue() == 0);
    public final IntProperty trackingBuffer = new IntProperty("TrackingBuffer", 500, 0, 2000, () -> mode.getValue() == 0);
    public final FloatProperty distanceMin = new FloatProperty("Distance-Min", 1.0F, 0.0F, 10.0F, () -> mode.getValue() == 0);
    public final FloatProperty distanceMax = new FloatProperty("Distance-Max", 3.0F, 0.0F, 10.0F, () -> mode.getValue() == 0);
    public final IntProperty chance = new IntProperty("Chance", 50, 0, 100, () -> mode.getValue() == 0);
    public final IntProperty lastAttackTimeToWork = new IntProperty("LastAttackTime", 1000, 0, 5000, () -> mode.getValue() == 0);
    public final IntProperty maxPackets = new IntProperty("MaxPackets", 80, 1, 200, () -> mode.getValue() == 0);
    public final ModeProperty targetMode = new ModeProperty("TargetMode", 0, new String[]{"ATTACK", "RANGE"}, () -> mode.getValue() == 0);
    public final ModeProperty espMode = new ModeProperty("ESP", 2, new String[]{"NONE", "BOX", "FILLED", "MODEL", "WIREFRAME"});
    public final ColorProperty espColor = new ColorProperty("Color", 0xFFFFFFFF);
    public final ModeProperty releaseStyle = new ModeProperty("Style", 1, new String[]{"PULSE", "SMOOTH"}, () -> mode.getValue() == 0);
    public final BooleanProperty smart = new BooleanProperty("Smart", true, () -> mode.getValue() == 0);
    public final BooleanProperty pauseOnHurt = new BooleanProperty("PauseOnHurt", false, () -> mode.getValue() == 0);
    public final IntProperty pauseHurtTime = new IntProperty("PauseHurtTime", 3, 0, 10, () -> mode.getValue() == 0 && pauseOnHurt.getValue());
    public final IntProperty fakePlayerPulseDelay = new IntProperty("FakePlayer-PulseDelay", 200, 50, 500, () -> mode.getValue() == 1);
    public final IntProperty fakePlayerIntavePackets = new IntProperty("FakePlayer-IntavePackets", 5, 1, 30, () -> mode.getValue() == 1);

    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final List<Packet<?>> skipPackets = new ArrayList<>();
    private final TimerUtil cycleTimer = new TimerUtil();
    private final TimerUtil trackingBufferTimer = new TimerUtil();
    private final TimerUtil backtrackCooldownTimer = new TimerUtil();
    private final TimerUtil attackTimer = new TimerUtil();

    private Vec3 trackedPosition = zeroVec();
    private EntityPlayer target;
    private int currentLatency;
    private int currentChance;
    private int nextDelay;
    private boolean hasAttacked;
    private EntityOtherPlayerMP fakePlayer;
    private EntityLivingBase currentTarget;
    private boolean fakeShown;
    private final TimerUtil fakePulseTimer = new TimerUtil();

    public BackTrack() {
        super("BackTrack", false, false, "Queue entity packets with configurable latency");
    }

    @Override
    public void onEnabled() {
        clear(false, true, false);
        currentChance = randomInt(0, 100);
        currentLatency = randomLatency();
        nextDelay = randomNextDelay();
    }

    @Override
    public void onDisabled() {
        clear(true, false, false);
        removeFakePlayer();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        clear(false, true, false);
        removeFakePlayer();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.ticksExisted <= 10) {
            clear(false, true, false);
            removeFakePlayer();
            return;
        }

        if (mode.getValue() == 1) {
            updateFakePlayer();
            return;
        }

        if (targetMode.getValue() == 1) {
            EntityPlayer enemy = findRangeTarget();
            if (enemy == null) {
                clear(true, false, true);
            } else {
                processTarget(enemy, false);
            }
        }

        boolean hadQueued = !packetQueue.isEmpty();
        if (shouldCancelPackets()) {
            flushExpiredPackets();
        } else if (hadQueued) {
            clear(true, false, true);
        }

        if (packetQueue.isEmpty()) {
            currentLatency = randomLatency();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mode.getValue() != 0 || event.getType() != EventType.RECEIVE || event.isCancelled()) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.ticksExisted <= 10) {
            clear(false, true, false);
            return;
        }

        Packet<?> packet = event.getPacket();
        if (skipPackets.remove(packet)) return;

        if (isFlushPacket(packet)) {
            clear(true, false, true);
            return;
        }

        if (target == null || !shouldCancelPackets()) return;
        if (!isTrackableTargetPacket(packet)) return;

        Vec3 nextPosition = predictPosition(packet, target, trackedPosition);
        if (nextPosition != null) {
            if (smart.getValue() && isRealTargetBetter(nextPosition)) {
                clear(true, false, true);
                return;
            }
            trackedPosition = nextPosition;
        }

        if (packetQueue.size() >= maxPackets.getValue()) {
            releaseOldestPacket();
        }

        packetQueue.add(new TimedPacket(packet, currentLatency));
        event.setCancelled(true);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mode.getValue() == 1) {
            handleFakePlayerAttack(event);
            return;
        }

        if (!(event.getTarget() instanceof EntityPlayer)) return;
        hasAttacked = true;
        attackTimer.reset();
        currentChance = randomInt(0, 100);

        if (targetMode.getValue() == 0) {
            processTarget((EntityPlayer) event.getTarget(), true);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mode.getValue() != 0 || target == null || trackedPosition == null || target.isDead || currentLatency <= 0) return;
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

    private void processTarget(EntityPlayer enemy, boolean fromAttack) {
        if (!shouldBacktrack(enemy, fromAttack)) return;
        if (enemy != target) {
            clear(true, false, false);
            trackedPosition = enemy.getPositionVector();
        }
        target = enemy;
    }

    private boolean shouldBacktrack(EntityPlayer enemy, boolean fromAttack) {
        if (enemy == null || enemy == mc.thePlayer || enemy.isDead || enemy.getHealth() <= 0.0F) return false;
        if (pauseOnHurt.getValue() && enemy instanceof EntityLivingBase && ((EntityLivingBase) enemy).hurtTime >= pauseHurtTime.getValue()) return false;
        if (currentChance >= chance.getValue()) return false;
        if (!backtrackCooldownTimer.hasTimeElapsed(nextDelay)) return false;
        if (!fromAttack && lastAttackTimeToWork.getValue() > 0 && (!hasAttacked || attackTimer.hasTimeElapsed(lastAttackTimeToWork.getValue()))) return false;

        double realDistance = mc.thePlayer.getDistanceToEntity(enemy);
        boolean inRange = realDistance >= Math.min(distanceMin.getValue(), distanceMax.getValue()) && realDistance <= Math.max(distanceMin.getValue(), distanceMax.getValue());
        if (inRange) trackingBufferTimer.reset();
        return inRange || !trackingBufferTimer.hasTimeElapsed(trackingBuffer.getValue());
    }

    private boolean shouldCancelPackets() {
        return target != null && !target.isDead && shouldBacktrack(target, false);
    }

    private EntityPlayer findRangeTarget() {
        double max = Math.max(distanceMin.getValue(), distanceMax.getValue());
        EntityPlayer bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead || player.getHealth() <= 0.0F) continue;

            double distance = mc.thePlayer.getDistanceToEntity(player);
            if (distance > max + 1.0D || distance >= bestDistance) continue;

            bestTarget = player;
            bestDistance = distance;
        }

        return bestTarget;
    }

    private boolean isFlushPacket(Packet<?> packet) {
        if (packet instanceof S08PacketPlayerPosLook || packet instanceof S40PacketDisconnect || packet instanceof S06PacketUpdateHealth) return true;
        if (packet instanceof S13PacketDestroyEntities && target != null) {
            for (int id : ((S13PacketDestroyEntities) packet).getEntityIDs()) {
                if (id == target.getEntityId()) return true;
            }
        }
        return false;
    }

    private boolean isTrackableTargetPacket(Packet<?> packet) {
        if (target == null) return false;
        if (packet instanceof S14PacketEntity) {
            Entity entity = ((S14PacketEntity) packet).getEntity(mc.theWorld);
            return entity != null && entity.getEntityId() == target.getEntityId();
        }
        return packet instanceof S18PacketEntityTeleport && ((S18PacketEntityTeleport) packet).getEntityId() == target.getEntityId();
    }

    private Vec3 predictPosition(Packet<?> packet, EntityPlayer entity, Vec3 base) {
        if (base == null) base = entity.getPositionVector();
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            return base.addVector(p.func_149062_c() / 32.0D, p.func_149061_d() / 32.0D, p.func_149064_e() / 32.0D);
        }
        if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            return new Vec3(p.getX() / 32.0D, p.getY() / 32.0D, p.getZ() / 32.0D);
        }
        return null;
    }

    private boolean isRealTargetBetter(Vec3 nextPosition) {
        double realDistance = mc.thePlayer.getDistanceToEntity(target);
        double trackedDistance = mc.thePlayer.getDistance(nextPosition.xCoord, nextPosition.yCoord, nextPosition.zCoord);
        return realDistance + 0.15D < trackedDistance;
    }

    private void flushExpiredPackets() {
        if (releaseStyle.getValue() == 0) {
            if (!cycleTimer.hasTimeElapsed(currentLatency)) return;
            releaseAll();
            cycleTimer.reset();
            return;
        }

        while (!packetQueue.isEmpty()) {
            TimedPacket timedPacket = packetQueue.peek();
            if (timedPacket == null || !timedPacket.timer.hasTimeElapsed(timedPacket.latency)) break;
            packetQueue.poll();
            receiveQueuedPacket(timedPacket.packet);
        }
    }

    private void releaseOldestPacket() {
        TimedPacket timedPacket = packetQueue.poll();
        if (timedPacket != null) receiveQueuedPacket(timedPacket.packet);
    }

    private void releaseAll() {
        while (!packetQueue.isEmpty()) {
            TimedPacket timedPacket = packetQueue.poll();
            if (timedPacket != null) receiveQueuedPacket(timedPacket.packet);
        }
        if (target != null) trackedPosition = target.getPositionVector();
    }

    private void clear(boolean handlePackets, boolean clearOnly, boolean applyCooldown) {
        if (handlePackets && !clearOnly) {
            releaseAll();
        } else if (clearOnly) {
            packetQueue.clear();
        }
        if (applyCooldown && target != null) {
            nextDelay = randomNextDelay();
            backtrackCooldownTimer.reset();
        }
        target = null;
        trackedPosition = zeroVec();
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

    private int randomNextDelay() {
        return randomInt(nextDelayMin.getValue(), nextDelayMax.getValue());
    }

    private static int randomInt(int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return RandomUtil.nextInt(low, high);
    }

    private static Vec3 zeroVec() {
        return new Vec3(0.0D, 0.0D, 0.0D);
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

    private static class TimedPacket {
        private final Packet<?> packet;
        private final TimerUtil timer;
        private final int latency;

        TimedPacket(Packet<?> packet, int latency) {
            this.packet = packet;
            this.timer = new TimerUtil();
            this.latency = Math.max(latency, 1);
        }
    }
}
