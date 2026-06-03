package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;

public class AutoHeadHitter extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty     placeDelay        = new IntProperty    ("place-delay",          10,  0, 100);
    public final IntProperty     speed             = new IntProperty    ("speed",                100, 30, 100);
    public final IntProperty     rotTol            = new IntProperty    ("rot-tolerance",         30,  5, 180);
    public final BooleanProperty swing             = new BooleanProperty("swing",               true);
    public final ModeProperty    moveFix           = new ModeProperty   ("move-fix",               1, new String[]{"NONE", "SILENT"});
    public final IntProperty     forwardDirection  = new IntProperty    ("forward-direction",      4,  1,  5);
    public final IntProperty     sidewaysDirection = new IntProperty    ("sideways-direction",     1,  1,  2);
    public final IntProperty     airTickThreshold  = new IntProperty    ("air-tick-threshold",     3,  0,  6);
    public final FloatProperty   cooldown          = new FloatProperty  ("cooldown",             0.5f, 0.1f, 1.0f);

    private static final int ROT_PRIORITY = 6;

    private float serverYaw;
    private float serverPitch;
    private float aimYaw;
    private float aimPitch;

    private BlockPos   targetBlock;
    private EnumFacing targetFacing;
    private Vec3       targetHitVec;

    private int stage = 0;

    private EnumFacing colDir      = null;
    private EnumFacing sideDir     = null;
    private boolean    wasOnGround = true;

    private BlockPos lockedColBot = null;

    private int  lastSlot      = -1;
    private long lastPlaceTime = 0;
    private int  airTicks      = 0;
    private long cooldownUntil = 0;

    public AutoHeadHitter() {
        super("AutoHeadHitter", false);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) {
            serverYaw   = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
            aimYaw      = serverYaw;
            aimPitch    = serverPitch;
            lastSlot    = mc.thePlayer.inventory.currentItem;
            wasOnGround = mc.thePlayer.onGround;
        }
        resetState();
    }

    @Override
    public void onDisabled() {
        if (lastSlot != -1 && mc.thePlayer != null
                && mc.thePlayer.inventory.currentItem != lastSlot) {
            mc.thePlayer.inventory.currentItem = lastSlot;
        }
        cooldownUntil = 0;
        resetState();
    }

    private void resetState() {
        targetBlock   = null;
        targetFacing  = null;
        targetHitVec  = null;
        stage         = 0;
        colDir        = null;
        sideDir       = null;
        lockedColBot  = null;
        lastPlaceTime = 0;
        airTicks      = 0;
    }

    @EventTarget(Priority.HIGH)
    public void onTick(TickEvent event) {
        if (!isEnabled()) return;
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null) return;

        if (mc.thePlayer.onGround) {
            colDir  = yawToForward(mc.thePlayer.rotationYaw);
            sideDir = leftOf(colDir);
            lockedColBot = new BlockPos(
                    MathHelper.floor_double(mc.thePlayer.posX),
                    MathHelper.floor_double(mc.thePlayer.posY),
                    MathHelper.floor_double(mc.thePlayer.posZ))
                    .offset(colDir, forwardDirection.getValue())
                    .offset(sideDir, sidewaysDirection.getValue());
            stage        = 0;
            wasOnGround  = true;
            airTicks     = 0;
            targetBlock  = null;
            targetFacing = null;
            targetHitVec = null;
        } else {
            airTicks++;
            if (wasOnGround) wasOnGround = false;
        }
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled()) return;
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) return;
        if (mc.thePlayer.onGround) return;
        if (!mc.thePlayer.isSprinting()) return;
        if (airTicks < airTickThreshold.getValue()) return;
        if (System.currentTimeMillis() < cooldownUntil) return;
        if (colDir == null || sideDir == null || lockedColBot == null) return;

        serverYaw   = event.getYaw();
        serverPitch = event.getPitch();

        int blockSlot = findBlockSlot();
        if (blockSlot == -1) {
            targetBlock = null; targetFacing = null; targetHitVec = null;
            return;
        }
        if (mc.thePlayer.inventory.currentItem != blockSlot)
            mc.thePlayer.inventory.currentItem = blockSlot;

        refreshStage();
        if (stage >= 4) {
            cooldownUntil = System.currentTimeMillis() + (long)(cooldown.getValue() * 1000f);
            resetState();
            return;
        }

        computeAimForStage();
        if (targetBlock == null) return;

        float yawDiff   = MathHelper.wrapAngleTo180_float(aimYaw - serverYaw);
        float pitchDiff = aimPitch - serverPitch;
        float maxTurn   = speed.getValue().floatValue();

        float steppedYaw   = serverYaw + MathHelper.clamp_float(yawDiff, -maxTurn, maxTurn);
        float steppedPitch = MathHelper.clamp_float(
                serverPitch + MathHelper.clamp_float(pitchDiff, -maxTurn, maxTurn), -90.0f, 90.0f);

        event.setRotation(steppedYaw, steppedPitch, ROT_PRIORITY);
        event.setPervRotation(
                moveFix.getValue() != 0 ? steppedYaw : mc.thePlayer.rotationYaw,
                ROT_PRIORITY);

        if (!withinRotationTolerance(steppedYaw, steppedPitch)) return;

        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < placeDelay.getValue()) return;

        double reach = mc.playerController.getBlockReachDistance();
        MovingObjectPosition mop = rayTrace(steppedYaw, steppedPitch, reach);

        if (mop == null
                || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || !mop.getBlockPos().equals(targetBlock)
                || mop.sideHit != targetFacing) {
            return;
        }

        ItemStack held = mc.thePlayer.inventory.getCurrentItem();
        if (held == null || !(held.getItem() instanceof ItemBlock)) return;

        float savedYaw   = mc.thePlayer.rotationYaw;
        float savedPitch = mc.thePlayer.rotationPitch;
        mc.thePlayer.rotationYaw   = steppedYaw;
        mc.thePlayer.rotationPitch = steppedPitch;

        mc.playerController.onPlayerRightClick(
                mc.thePlayer, mc.theWorld, held,
                targetBlock, targetFacing, mop.hitVec);

        mc.thePlayer.rotationYaw   = savedYaw;
        mc.thePlayer.rotationPitch = savedPitch;

        if (swing.getValue()) {
            mc.thePlayer.swingItem();
        } else {
            try { mc.getNetHandler().addToSendQueue(new C0APacketAnimation()); }
            catch (Exception ignored) {}
        }

        lastPlaceTime = now;
        stage++;
        targetBlock  = null;
        targetFacing = null;
        targetHitVec = null;
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

    private void refreshStage() {
        BlockPos colBot = lockedColBot;
        BlockPos colMid = colBot.up(1);
        BlockPos colTop = colBot.up(2);
        BlockPos hhPos  = colTop.offset(sideDir.getOpposite(), 1);

        if      (isReplaceable(colBot)) stage = 0;
        else if (isReplaceable(colMid)) stage = 1;
        else if (isReplaceable(colTop)) stage = 2;
        else if (isReplaceable(hhPos))  stage = 3;
        else                            stage = 4;
    }

    private void computeAimForStage() {
        targetBlock  = null;
        targetFacing = null;
        targetHitVec = null;

        BlockPos colBot = lockedColBot;
        BlockPos colMid = colBot.up(1);
        BlockPos colTop = colBot.up(2);

        switch (stage) {
            case 0: {
                BlockPos ground = colBot.down();
                if (isReplaceable(ground)) return;
                aimAtFaceCenter(ground, EnumFacing.UP);
                break;
            }
            case 1:
                if (isReplaceable(colBot)) return;
                aimAtFaceCenter(colBot, EnumFacing.UP);
                break;
            case 2:
                if (isReplaceable(colMid)) return;
                aimAtFaceCenter(colMid, EnumFacing.UP);
                break;
            case 3: {
                if (isReplaceable(colTop)) return;

                EnumFacing placeFace = sideDir.getOpposite();
                double reach = mc.playerController.getBlockReachDistance();
                Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);

                double fcx = colTop.getX() + 0.5 + placeFace.getFrontOffsetX() * 0.5;
                double fcy = colTop.getY() + 0.5 + placeFace.getFrontOffsetY() * 0.5;
                double fcz = colTop.getZ() + 0.5 + placeFace.getFrontOffsetZ() * 0.5;
                double distSq = (fcx - eyes.xCoord) * (fcx - eyes.xCoord)
                        + (fcy - eyes.yCoord) * (fcy - eyes.yCoord)
                        + (fcz - eyes.zCoord) * (fcz - eyes.zCoord);
                if (distSq > (reach + 1) * (reach + 1)) return;

                double[] offsets = {0.1, 0.3, 0.5, 0.7, 0.9};
                float bestYaw   = Float.NaN;
                float bestPitch = Float.NaN;
                Vec3  bestHit   = null;
                float bestDiff  = Float.MAX_VALUE;

                for (double u : offsets) {
                    for (double v : offsets) {
                        Vec3 hitPos = facePoint(colTop, placeFace, u, v);
                        float[] rot = computeRotations(
                                hitPos.xCoord - eyes.xCoord,
                                hitPos.yCoord - eyes.yCoord,
                                hitPos.zCoord - eyes.zCoord);

                        MovingObjectPosition mop = rayTrace(rot[0], rot[1], reach);
                        if (mop == null
                                || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                                || !mop.getBlockPos().equals(colTop)
                                || mop.sideHit != placeFace) continue;

                        float diff = Math.abs(MathHelper.wrapAngleTo180_float(rot[0] - serverYaw))
                                + Math.abs(rot[1] - serverPitch);
                        if (diff < bestDiff) {
                            bestDiff  = diff;
                            bestYaw   = rot[0];
                            bestPitch = rot[1];
                            bestHit   = mop.hitVec;
                        }
                    }
                }

                if (bestHit == null) return;

                aimYaw       = bestYaw;
                aimPitch     = bestPitch;
                targetBlock  = colTop;
                targetFacing = placeFace;
                targetHitVec = bestHit;
                break;
            }
        }
    }

    private Vec3 facePoint(BlockPos block, EnumFacing face, double u, double v) {
        double x = block.getX();
        double y = block.getY();
        double z = block.getZ();
        switch (face) {
            case DOWN:  return new Vec3(x + u, y,       z + v);
            case UP:    return new Vec3(x + u, y + 1.0, z + v);
            case NORTH: return new Vec3(x + u, y + v,   z);
            case SOUTH: return new Vec3(x + u, y + v,   z + 1.0);
            case WEST:  return new Vec3(x,     y + v,   z + u);
            case EAST:  return new Vec3(x + 1.0, y + v, z + u);
            default:    return new Vec3(x + 0.5, y + 0.5, z + 0.5);
        }
    }

    private void aimAtFaceCenter(BlockPos block, EnumFacing face) {
        double fx = block.getX() + 0.5;
        double fy = block.getY() + 0.5;
        double fz = block.getZ() + 0.5;

        switch (face) {
            case DOWN:  fy = block.getY();       break;
            case UP:    fy = block.getY() + 1.0; break;
            case NORTH: fz = block.getZ();       break;
            case SOUTH: fz = block.getZ() + 1.0; break;
            case WEST:  fx = block.getX();       break;
            case EAST:  fx = block.getX() + 1.0; break;
        }

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        float[] rot = computeRotations(fx - eyes.xCoord, fy - eyes.yCoord, fz - eyes.zCoord);
        aimYaw   = rot[0];
        aimPitch = rot[1];

        targetBlock  = block;
        targetFacing = face;
        targetHitVec = new Vec3(fx, fy, fz);
    }

    private float[] computeRotations(double dx, double dy, double dz) {
        double hd    = Math.sqrt(dx * dx + dz * dz);
        float  yaw   = MathHelper.wrapAngleTo180_float(
                (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        float  pitch = (float)(-Math.toDegrees(Math.atan2(dy, hd)));
        return new float[]{yaw, pitch};
    }

    private MovingObjectPosition rayTrace(float yaw, float pitch, double distance) {
        float  yawRad   = (float) Math.toRadians(yaw);
        float  pitchRad = (float) Math.toRadians(pitch);
        double lx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double ly = -Math.sin(pitchRad);
        double lz =  Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 start = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 end   = start.addVector(lx * distance, ly * distance, lz * distance);
        return mc.theWorld.rayTraceBlocks(start, end);
    }

    private EnumFacing yawToForward(float yaw) {
        yaw = MathHelper.wrapAngleTo180_float(yaw);
        if      (yaw < -135f || yaw > 135f) return EnumFacing.NORTH;
        else if (yaw < -45f)                return EnumFacing.EAST;
        else if (yaw <  45f)                return EnumFacing.SOUTH;
        else                                return EnumFacing.WEST;
    }

    private EnumFacing leftOf(EnumFacing f) {
        switch (f) {
            case NORTH: return EnumFacing.WEST;
            case SOUTH: return EnumFacing.EAST;
            case EAST:  return EnumFacing.NORTH;
            default:    return EnumFacing.SOUTH;
        }
    }

    private boolean withinRotationTolerance(float tYaw, float tPitch) {
        float dy = Math.abs(MathHelper.wrapAngleTo180_float(tYaw   - serverYaw));
        float dp = Math.abs(MathHelper.wrapAngleTo180_float(tPitch - serverPitch));
        return dy <= rotTol.getValue() && dp <= rotTol.getValue();
    }

    private int findBlockSlot() {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null || stack.stackSize == 0) continue;
            if (!(stack.getItem() instanceof ItemBlock)) continue;
            if (((ItemBlock) stack.getItem()).getBlock() != Blocks.air) return slot;
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

    public int getSlot() { return lastSlot; }
}