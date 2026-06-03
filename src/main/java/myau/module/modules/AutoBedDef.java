package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.MoveInputEvent;
import myau.events.Render2DEvent;
import myau.events.SwapItemEvent;
import myau.events.TickEvent;
import myau.events.UpdateEvent;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AutoBedDef extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty range = new FloatProperty("range", 5.0f, 3.0f, 7.0f);
    public final IntProperty placeDelay = new IntProperty("place-delay", 80, 0, 300);
    public final IntProperty speed = new IntProperty("speed", 100, 30, 100);
    public final IntProperty rotTol = new IntProperty("rot-tolerance", 35, 5, 180);
    public final BooleanProperty swing = new BooleanProperty("swing", true);
    public final BooleanProperty showProgress = new BooleanProperty("show-progress", true);
    public final ModeProperty moveFix = new ModeProperty("move-fix", 1, new String[]{"NONE", "SILENT"});

    private static final int ROT_PRIORITY = 6;
    private static final int BED_SEARCH_RADIUS = 8;

    private float serverYaw;
    private float serverPitch;
    private float aimYaw;
    private float aimPitch;
    private float lastSteppedYaw;
    private float lastSteppedPitch;

    private BlockPos targetBlock;
    private EnumFacing targetFacing;
    private Vec3 targetHitVec;

    private long lastPlaceTime = 0;
    private int lastSlot = -1;
    private float progress = 0f;

    private BlockPos lockedBedFoot = null;
    private EnumFacing lockedBedFacing = null;

    public AutoBedDef() {
        super("AutoBedDef", false);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            serverYaw = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
            aimYaw = serverYaw;
            aimPitch = serverPitch;
            lastSteppedYaw = serverYaw;
            lastSteppedPitch = serverPitch;
            lastSlot = mc.thePlayer.inventory.currentItem;
        }
        progress = 0f;
        resetTarget();
        lockedBedFoot = null;
        lockedBedFacing = null;
    }

    @Override
    public void onDisabled() {
        if (lastSlot != -1 && mc.thePlayer != null && mc.thePlayer.inventory.currentItem != lastSlot) {
            mc.thePlayer.inventory.currentItem = lastSlot;
        }
        progress = 0f;
        resetTarget();
        lockedBedFoot = null;
        lockedBedFacing = null;
    }

    private void resetTarget() {
        targetBlock = null;
        targetFacing = null;
        targetHitVec = null;
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled()) return;
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;

        serverYaw = event.getYaw();
        serverPitch = event.getPitch();

        updateProgress();

        if (!isValidBed(lockedBedFoot)) {
            lockedBedFoot = null;
            lockedBedFacing = null;
            findNearestBed();
        }
        if (lockedBedFoot == null) return;

        int woolSlot = findWoolSlot();
        if (woolSlot == -1) {
            resetTarget();
            return;
        }
        if (mc.thePlayer.inventory.currentItem != woolSlot) {
            mc.thePlayer.inventory.currentItem = woolSlot;
        }

        List<BlockPos> defensePositions = getDefensePositions();
        int nextIdx = findNextDefensePosIndex(defensePositions);
        if (nextIdx == -1) {
            resetTarget();
            return;
        }
        BlockPos nextTarget = defensePositions.get(nextIdx);

        boolean isRoof = nextIdx >= 6;
        computePlacement(nextTarget, isRoof);
        if (targetBlock == null) return;

        float yawDiff = MathHelper.wrapAngleTo180_float(aimYaw - serverYaw);
        float pitchDiff = aimPitch - serverPitch;
        float maxTurn = (float) speed.getValue();

        lastSteppedYaw = serverYaw + MathHelper.clamp_float(yawDiff, -maxTurn, maxTurn);
        lastSteppedPitch = MathHelper.clamp_float(
                serverPitch + MathHelper.clamp_float(pitchDiff, -maxTurn, maxTurn), -90f, 90f);

        event.setRotation(lastSteppedYaw, lastSteppedPitch, ROT_PRIORITY);
        event.setPervRotation(moveFix.getValue() != 0 ? lastSteppedYaw : mc.thePlayer.rotationYaw, ROT_PRIORITY);
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (!isEnabled()) return;
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;
        if (targetBlock == null || targetFacing == null || targetHitVec == null) return;

        float useYaw = RotationState.isActived() && RotationState.getPriority() == ROT_PRIORITY
                ? RotationState.getSmoothedYaw() : lastSteppedYaw;
        float usePitch = lastSteppedPitch;

        if (!withinRotationTolerance(useYaw, usePitch, aimYaw, aimPitch)) return;

        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < placeDelay.getValue()) return;

        double reach = range.getValue();
        MovingObjectPosition mop = rayTrace(useYaw, usePitch, reach);

        if (mop == null
                || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || !mop.getBlockPos().equals(targetBlock)
                || mop.sideHit != targetFacing) return;

        ItemStack held = mc.thePlayer.inventory.getCurrentItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;

        float savedYaw = mc.thePlayer.rotationYaw;
        float savedPitch = mc.thePlayer.rotationPitch;
        boolean wasSneaking = mc.thePlayer.isSneaking();

        mc.thePlayer.rotationYaw = useYaw;
        mc.thePlayer.rotationPitch = usePitch;
        mc.thePlayer.setSneaking(true);

        mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, held, targetBlock, targetFacing, mop.hitVec);

        mc.thePlayer.setSneaking(wasSneaking);
        mc.thePlayer.rotationYaw = savedYaw;
        mc.thePlayer.rotationPitch = savedPitch;

        if (swing.getValue()) {
            mc.thePlayer.swingItem();
        } else {
            try {
                mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
            } catch (Exception ignored) {
            }
        }

        lastPlaceTime = now;
        resetTarget();
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (!isEnabled()) return;
        if (moveFix.getValue() == 1
                && RotationState.isActived()
                && RotationState.getPriority() == ROT_PRIORITY
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (isEnabled()) {
            lastSlot = event.setSlot(lastSlot);
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!isEnabled() || mc.currentScreen != null) return;
        if (!showProgress.getValue()) return;
        if (mc.fontRendererObj == null) return;

        String text = String.format("%.0f%%", progress * 100.0f);

        GL11.glPushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ScaledResolution sr = new ScaledResolution(mc);
        int width = mc.fontRendererObj.getStringWidth(text);

        mc.fontRendererObj.drawString(
                text,
                (float) sr.getScaledWidth() / 2.0f - (float) width / 2.0f,
                (float) sr.getScaledHeight() / 5.0f * 2.0f,
                getProgressColor().getRGB() & 16777215 | -1090519040,
                true
        );

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GL11.glPopMatrix();
    }

    private void findNearestBed() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        BlockPos origin = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                MathHelper.floor_double(mc.thePlayer.posY),
                MathHelper.floor_double(mc.thePlayer.posZ));

        double bestDist = Double.MAX_VALUE;

        for (int dx = -BED_SEARCH_RADIUS; dx <= BED_SEARCH_RADIUS; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -BED_SEARCH_RADIUS; dz <= BED_SEARCH_RADIUS; dz++) {
                    BlockPos p = origin.add(dx, dy, dz);
                    if (!isBedFoot(p)) continue;

                    EnumFacing facing = getBedFacing(p);
                    if (facing == null) continue;

                    double dist = mc.thePlayer.getDistanceSq(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                    if (dist < bestDist) {
                        bestDist = dist;
                        lockedBedFoot = p;
                        lockedBedFacing = facing;
                    }
                }
            }
        }
    }

    private boolean isBedFoot(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        if (!(b instanceof BlockBed)) return false;
        int meta = b.getMetaFromState(mc.theWorld.getBlockState(pos));
        return (meta & 8) == 0;
    }

    private boolean isValidBed(BlockPos pos) {
        if (pos == null) return false;
        return mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed;
    }

    private EnumFacing getBedFacing(BlockPos pos) {
        int meta = mc.theWorld.getBlockState(pos).getBlock().getMetaFromState(mc.theWorld.getBlockState(pos));
        switch (meta & 3) {
            case 0:
                return EnumFacing.SOUTH;
            case 1:
                return EnumFacing.WEST;
            case 2:
                return EnumFacing.NORTH;
            case 3:
                return EnumFacing.EAST;
            default:
                return null;
        }
    }

    private List<BlockPos> getDefensePositions() {
        List<BlockPos> list = new ArrayList<>();
        if (lockedBedFoot == null || lockedBedFacing == null) return list;

        BlockPos foot = lockedBedFoot;
        BlockPos head = foot.offset(lockedBedFacing);
        EnumFacing fwd = lockedBedFacing;
        EnumFacing back = fwd.getOpposite();
        EnumFacing left = leftOf(fwd);
        EnumFacing right = left.getOpposite();

        list.add(foot.offset(left));
        list.add(foot.offset(right));
        list.add(head.offset(left));
        list.add(head.offset(right));
        list.add(foot.offset(back));
        list.add(head.offset(fwd));
        list.add(foot.up(1));
        list.add(head.up(1));

        return list;
    }

    private int findNextDefensePosIndex(List<BlockPos> positions) {
        for (int i = 0; i < positions.size(); i++) {
            if (isReplaceable(positions.get(i))) return i;
        }
        return -1;
    }

    private void computePlacement(BlockPos wantPlace, boolean isRoof) {
        resetTarget();

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double reach = range.getValue();
        double reachSq = reach * reach;

        for (EnumFacing face : EnumFacing.values()) {
            BlockPos support = wantPlace.offset(face);

            if (isReplaceable(support)) continue;
            Block supportBlock = mc.theWorld.getBlockState(support).getBlock();
            if (!isRoof && supportBlock instanceof BlockBed) continue;

            double dx = support.getX() + 0.5 - eyes.xCoord;
            double dy = support.getY() + 0.5 - eyes.yCoord;
            double dz = support.getZ() + 0.5 - eyes.zCoord;
            if (dx * dx + dy * dy + dz * dz > (reach + 1.5) * (reach + 1.5)) continue;

            EnumFacing clickFace = face.getOpposite();
            double[] offsets = {0.2, 0.4, 0.5, 0.6, 0.8};

            float bestDiff = isRoof ? Float.MIN_VALUE : Float.MAX_VALUE;
            float bestYaw = Float.NaN;
            float bestPitch = Float.NaN;
            Vec3 bestHit = null;

            for (double u : offsets) {
                for (double v : offsets) {
                    Vec3 hitPos = getFacePoint(support, clickFace, u, v);

                    double hdx = hitPos.xCoord - eyes.xCoord;
                    double hdy = hitPos.yCoord - eyes.yCoord;
                    double hdz = hitPos.zCoord - eyes.zCoord;
                    if (hdx * hdx + hdy * hdy + hdz * hdz > reachSq) continue;

                    float[] rot = computeRotations(hdx, hdy, hdz);

                    MovingObjectPosition mop = rayTrace(rot[0], rot[1], reach);
                    if (mop == null
                            || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                            || !mop.getBlockPos().equals(support)
                            || mop.sideHit != clickFace) continue;

                    float diff = Math.abs(MathHelper.wrapAngleTo180_float(rot[0] - serverYaw)) + Math.abs(rot[1] - serverPitch);

                    boolean better = isRoof ? (diff > bestDiff) : (diff < bestDiff);
                    if (better) {
                        bestDiff = diff;
                        bestYaw = rot[0];
                        bestPitch = rot[1];
                        bestHit = mop.hitVec;
                    }
                }
            }

            if (bestHit != null) {
                aimYaw = bestYaw;
                aimPitch = bestPitch;
                targetBlock = support;
                targetFacing = clickFace;
                targetHitVec = bestHit;
                return;
            }
        }
    }

    private Vec3 getFacePoint(BlockPos block, EnumFacing face, double u, double v) {
        double x = block.getX();
        double y = block.getY();
        double z = block.getZ();
        switch (face) {
            case DOWN:
                return new Vec3(x + u, y, z + v);
            case UP:
                return new Vec3(x + u, y + 1.0, z + v);
            case NORTH:
                return new Vec3(x + u, y + v, z);
            case SOUTH:
                return new Vec3(x + u, y + v, z + 1.0);
            case WEST:
                return new Vec3(x, y + v, z + u);
            case EAST:
                return new Vec3(x + 1.0, y + v, z + u);
            default:
                return new Vec3(x + 0.5, y + 0.5, z + 0.5);
        }
    }

    private float[] computeRotations(double dx, double dy, double dz) {
        double hd = Math.sqrt(dx * dx + dz * dz);
        float yaw = MathHelper.wrapAngleTo180_float((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, hd)));
        return new float[]{yaw, pitch};
    }

    private MovingObjectPosition rayTrace(float yaw, float pitch, double distance) {
        float yr = (float) Math.toRadians(yaw);
        float pr = (float) Math.toRadians(pitch);
        double lx = -Math.sin(yr) * Math.cos(pr);
        double ly = -Math.sin(pr);
        double lz = Math.cos(yr) * Math.cos(pr);
        Vec3 start = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 end = start.addVector(lx * distance, ly * distance, lz * distance);
        return mc.theWorld.rayTraceBlocks(start, end);
    }

    private boolean withinRotationTolerance(float useYaw, float usePitch, float targetYaw, float targetPitch) {
        float dy = Math.abs(MathHelper.wrapAngleTo180_float(useYaw - targetYaw));
        float dp = Math.abs(MathHelper.wrapAngleTo180_float(usePitch - targetPitch));
        return dy <= rotTol.getValue() && dp <= rotTol.getValue();
    }

    private int findWoolSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null || stack.stackSize == 0) continue;
            if (!(stack.getItem() instanceof ItemBlock)) continue;
            if (((ItemBlock) stack.getItem()).getBlock() == Blocks.wool) return slot;
        }
        return -1;
    }

    private boolean isReplaceable(BlockPos pos) {
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        return b == Blocks.air
                || b == Blocks.water
                || b == Blocks.flowing_water
                || b == Blocks.lava
                || b == Blocks.flowing_lava
                || b == Blocks.fire;
    }

    private EnumFacing leftOf(EnumFacing f) {
        switch (f) {
            case NORTH:
                return EnumFacing.WEST;
            case WEST:
                return EnumFacing.SOUTH;
            case SOUTH:
                return EnumFacing.EAST;
            case EAST:
                return EnumFacing.NORTH;
            default:
                return EnumFacing.NORTH;
        }
    }

    private void updateProgress() {
        if (lockedBedFoot == null || lockedBedFacing == null) {
            progress = 0f;
            return;
        }
        List<BlockPos> positions = getDefensePositions();
        int filled = 0;
        for (BlockPos pos : positions) {
            if (!isReplaceable(pos)) filled++;
        }
        progress = positions.isEmpty() ? 0f : (float) filled / (float) positions.size();
    }

    private Color getProgressColor() {
        if (progress <= 0.33f) return new Color(255, 85, 85);
        else if (progress <= 0.66f) return new Color(255, 255, 85);
        else return new Color(85, 255, 85);
    }

    public int getSlot() {
        return lastSlot;
    }
}
