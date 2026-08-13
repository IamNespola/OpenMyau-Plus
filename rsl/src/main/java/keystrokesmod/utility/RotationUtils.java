package keystrokesmod.utility;

import com.google.common.base.Predicates;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.client.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;

import java.util.List;

public class RotationUtils {
    public static final Minecraft mc = Minecraft.getMinecraft();
    public static float[] serverRotations = new float[] { 0, 0 } ;
    public static final float PI = (float) Math.PI;
    public static final float TO_DEGREES = 180.0F / PI;

    public static float[] getRotations(BlockPos blockPos, final float n, final float n2) {
        final float[] array = getRotations(blockPos);
        return fixRotation(array[0], array[1], n, n2);
    }

    public static float[] getRotations(BlockPos blockPos) {
        double x = blockPos.getX() + 0.45 - mc.thePlayer.posX;
        double y = blockPos.getY() + 0.45 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = blockPos.getZ() + 0.45 - mc.thePlayer.posZ;

        float angleToBlock = (float) (Math.atan2(z, x) * (180 / Math.PI)) - 90.0f;
        float deltaYaw = MathHelper.wrapAngleTo180_float(angleToBlock - mc.thePlayer.rotationYaw);
        float yaw = mc.thePlayer.rotationYaw + deltaYaw;

        double distance = MathHelper.sqrt_double(x * x + z * z);
        float angleToBlockPitch = (float) (-(Math.atan2(y, distance) * (180 / Math.PI)));
        float deltaPitch = MathHelper.wrapAngleTo180_float(angleToBlockPitch - mc.thePlayer.rotationPitch);
        float pitch = mc.thePlayer.rotationPitch + deltaPitch;

        pitch = clampTo90(pitch);

        return new float[] { yaw, pitch };
    }

    public static float[] getRotations(double posX, double posY, double posZ) {
        double x = posX + 1.0 - mc.thePlayer.posX;
        double y = posY + 1.0 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = posZ + 1.0 - mc.thePlayer.posZ;

        float angleToBlock = (float) (Math.atan2(z, x) * (180 / Math.PI)) - 90.0f;
        float deltaYaw = MathHelper.wrapAngleTo180_float(angleToBlock - mc.thePlayer.rotationYaw);
        float yaw = mc.thePlayer.rotationYaw + deltaYaw;

        double distance = MathHelper.sqrt_double(x * x + z * z);
        float angleToBlockPitch = (float) (-(Math.atan2(y, distance) * (180 / Math.PI)));
        float deltaPitch = MathHelper.wrapAngleTo180_float(angleToBlockPitch - mc.thePlayer.rotationPitch);
        float pitch = mc.thePlayer.rotationPitch + deltaPitch;

        pitch = clampTo90(pitch);

        return new float[] { yaw, pitch };
    }

    public static float[] getRotations(Vec3 vec3) {
        double x = vec3.xCoord + 0.45 - mc.thePlayer.posX;
        double y = vec3.yCoord + 0.45 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = vec3.zCoord + 0.45 - mc.thePlayer.posZ;

        float angleToBlock = (float) (Math.atan2(z, x) * (180 / Math.PI)) - 90.0f;
        float deltaYaw = MathHelper.wrapAngleTo180_float(angleToBlock - mc.thePlayer.rotationYaw);
        float yaw = mc.thePlayer.rotationYaw + deltaYaw;

        double distance = MathHelper.sqrt_double(x * x + z * z);
        float angleToBlockPitch = (float) (-(Math.atan2(y, distance) * (180 / Math.PI)));
        float deltaPitch = MathHelper.wrapAngleTo180_float(angleToBlockPitch - mc.thePlayer.rotationPitch);
        float pitch = mc.thePlayer.rotationPitch + deltaPitch;

        pitch = clampTo90(pitch);

        return new float[] { yaw, pitch };
    }

    public static float[] getRotations(BlockPos blockPos, EnumFacing enumFacing) {
        double x = blockPos.getX() + 0.5D;
        double y = blockPos.getY() + 0.5D;
        double z = blockPos.getZ() + 0.5D;

        x += (double) enumFacing.getDirectionVec().getX() * 0.5D;
        y += (double) enumFacing.getDirectionVec().getY() * 0.5D;
        z += (double) enumFacing.getDirectionVec().getZ() * 0.5D;
        Vec3 vec = new Vec3(x, y, z);

        Vec3 playerVec = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.height, mc.thePlayer.posZ);
        Vec3 diff = vec.subtract(playerVec);
        double distance = Math.hypot(diff.xCoord, diff.zCoord);
        float yaw = (float) (MathHelper.atan2(diff.zCoord, diff.xCoord) * TO_DEGREES) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(diff.yCoord, distance) * TO_DEGREES));
        return new float[] { applyVanilla(yaw), clampTo90(pitch) };
    }

    public static float interpolateValue(float tickDelta, float old, float newFloat) {
        return old + (newFloat - old) * tickDelta;
    }

    public static float[] getRotations(Entity entity, final float yaw, final float pitch) {
        final float[] array = getRotations(entity);
        if (array == null) {
            return null;
        }
        return fixRotation(array[0], array[1], yaw, pitch);
    }

    public static double distanceFromYaw(final Entity entity, final boolean b) {
        return Math.abs(MathHelper.wrapAngleTo180_double(i(entity.posX, entity.posZ) - (mc.thePlayer.rotationYaw)));
    }

    public static float i(final double n, final double n2) {
        return (float)(Math.atan2(n - mc.thePlayer.posX, n2 - mc.thePlayer.posZ) * 57.295780181884766 * -1.0);
    }

    public static boolean isPossibleToHit(Entity target, double reach, float[] rotations) {
        final Vec3 eyePosition = mc.thePlayer.getPositionEyes(1.0f);

        final float yaw = rotations[0];
        final float pitch = rotations[1];

        final float radianYaw = -yaw * 0.017453292f - (float)Math.PI;
        final float radianPitch = -pitch * 0.017453292f;

        final float cosYaw = MathHelper.cos(radianYaw);
        final float sinYaw = MathHelper.sin(radianYaw);
        final float cosPitch = -MathHelper.cos(radianPitch);
        final float sinPitch = MathHelper.sin(radianPitch);

        final Vec3 lookVector = new Vec3(
                sinYaw * cosPitch, // x
                sinPitch,         // y
                cosYaw * cosPitch // z
        );

        final double lookVecX = lookVector.xCoord * reach;
        final double lookVecY = lookVector.yCoord * reach;
        final double lookVecZ = lookVector.zCoord * reach;

        final Vec3 endPosition = eyePosition.addVector(lookVecX, lookVecY, lookVecZ);

        final Entity renderViewEntity = mc.getRenderViewEntity();
        final AxisAlignedBB expandedBox = renderViewEntity
                .getEntityBoundingBox()
                .addCoord(lookVecX, lookVecY, lookVecZ)
                .expand(1.0, 1.0, 1.0);

        final List<Entity> entitiesInPath = mc.theWorld.getEntitiesWithinAABBExcludingEntity(renderViewEntity, expandedBox);
        for (Entity entity : entitiesInPath) {
            if (entity == target && entity.canBeCollidedWith()) {
                final float borderSize = entity.getCollisionBorderSize();
                final AxisAlignedBB entityBox = entity.getEntityBoundingBox()
                        .expand(borderSize, borderSize, borderSize);
                final MovingObjectPosition intercept = entityBox.calculateIntercept(eyePosition, endPosition);
                return intercept != null;
            }
        }

        return false;
    }

    public static boolean inRange(final BlockPos blockPos, final double n) {
        final float[] array = RotationUtils.getRotations(blockPos);
        final Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0f);
        final float n2 = -array[0] * 0.017453292f;
        final float n3 = -array[1] * 0.017453292f;
        final float cos = MathHelper.cos(n2 - 3.1415927f);
        final float sin = MathHelper.sin(n2 - 3.1415927f);
        final float n4 = -MathHelper.cos(n3);
        final Vec3 vec3 = new Vec3(sin * n4, MathHelper.sin(n3), cos * n4);
        Block block = BlockUtils.getBlock(blockPos);
        IBlockState blockState = BlockUtils.getBlockState(blockPos);
        if (block != null && blockState != null) {
            AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, blockPos, blockState);
            if (boundingBox != null) {
                Vec3 targetVec = getPositionEyes.addVector(vec3.xCoord * n, vec3.yCoord * n, vec3.zCoord * n);
                MovingObjectPosition intercept = boundingBox.calculateIntercept(getPositionEyes, targetVec);
                if (intercept != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float[] getRotations(final Entity entity) {
        return getRotations(entity, PLAYER_OFFSETS.NONE);
    }

    public static float[] getRotations(final Entity entity, PLAYER_OFFSETS playerOffset) {
        if (entity == null) {
            return null;
        }
        final double n = entity.posX - mc.thePlayer.posX;
        final double n2 = entity.posZ - mc.thePlayer.posZ;
        double n3;
        if (entity instanceof EntityLivingBase) {
            final EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
            n3 = entityLivingBase.posY + playerOffset.getHeightOffset(entityLivingBase) * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        }
        else {
            n3 = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        }
        return new float[] { mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float((float) (Math.atan2(n2, n) * 57.295780181884766) - 90.0f - mc.thePlayer.rotationYaw), clampTo90(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float((float) (-(Math.atan2(n3, MathHelper.sqrt_double(n * n + n2 * n2)) * 57.295780181884766)) - mc.thePlayer.rotationPitch) + 3.0f)};
    }

    public static float[] getRotationsPredicated(final Entity entity, final int ticks) {
        if (entity == null) {
            return null;
        }
        if (ticks == 0) {
            return getRotations(entity);
        }
        double posX = entity.posX;
        final double posY = entity.posY;
        double posZ = entity.posZ;
        final double n2 = posX - entity.lastTickPosX;
        final double n3 = posZ - entity.lastTickPosZ;
        for (int i = 0; i < ticks; ++i) {
            posX += n2;
            posZ += n3;
        }
        final double n4 = posX - mc.thePlayer.posX;
        double n5;
        if (entity instanceof EntityLivingBase) {
            n5 = posY + entity.getEyeHeight() * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        }
        else {
            n5 = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        }
        final double n6 = posZ - mc.thePlayer.posZ;
        return new float[] { applyVanilla(mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float((float)(Math.atan2(n6, n4) * 57.295780181884766) - 90.0f - mc.thePlayer.rotationYaw)), clampTo90(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float((float)(-(Math.atan2(n5, MathHelper.sqrt_double(n4 * n4 + n6 * n6)) * 57.295780181884766)) - mc.thePlayer.rotationPitch) + 3.0f) };
    }

    public static float clampTo90(final float n) {
        return MathHelper.clamp_float(n, -90.0f, 90.0f);
    }

    public static float[] fixRotation(float yaw, float n2, final float n3, final float n4) {
        float n5 = yaw - n3;
        final float abs = Math.abs(n5);
        final float n7 = n2 - n4;
        final float n8 = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        final double n9 = n8 * n8 * n8 * 1.2;
        final float n10 = (float) (Math.round((double) n5 / n9) * n9);
        final float n11 = (float) (Math.round((double) n7 / n9) * n9);
        yaw = n3 + n10;
        n2 = n4 + n11;
        if (abs >= 1.0f) {
            final int n12 = (int) Settings.randomYawFactor.getInput();
            if (n12 != 0) {
                final int n13 = n12 * 100 + Utils.randomizeInt(-30, 30);
                yaw += Utils.randomizeInt(-n13, n13) / 100.0;
            }
        } else if (abs <= 0.04) {
            yaw += ((abs > 0.0f) ? 0.01 : -0.01);
        }
        return new float[] { yaw, clampTo90(n2) };
    }

    public static float angle(final double n, final double n2) {
        return (float) (Math.atan2(n - mc.thePlayer.posX, n2 - mc.thePlayer.posZ) * 57.295780181884766 * -1.0);
    }

    public static MovingObjectPosition rayCast(double distance, float yaw, float pitch, boolean collisionCheck) {
        final Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0f);
        final float n4 = -yaw * 0.017453292f;
        final float n5 = -pitch * 0.017453292f;
        final float cos = MathHelper.cos(n4 - 3.1415927f);
        final float sin = MathHelper.sin(n4 - 3.1415927f);
        final float n6 = -MathHelper.cos(n5);
        final Vec3 vec3 = new Vec3(sin * n6, MathHelper.sin(n5), cos * n6);
        return mc.theWorld.rayTraceBlocks(getPositionEyes, getPositionEyes.addVector(vec3.xCoord * distance, vec3.yCoord * distance, vec3.zCoord * distance), true, collisionCheck, true);
    }

    public static MovingObjectPosition rayTraceCustom(double blockReachDistance, float yaw, float pitch) {
        final Vec3 vec3 = mc.thePlayer.getPositionEyes(1.0F);
        final Vec3 vec31 = getVectorForRotation(pitch, yaw);
        final Vec3 vec32 = vec3.addVector(vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance);
        return mc.theWorld.rayTraceBlocks(vec3, vec32, false, false, true);
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * ((float)Math.PI / 180F) - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * ((float)Math.PI / 180F) - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * ((float)Math.PI / 180F));
        float f3 = MathHelper.sin(-pitch * ((float)Math.PI / 180F));
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static float applyVanilla(float yaw, boolean stop) {
        if (stop) {
            return yaw;
        }
        int scaleFactor = (int) Math.floor(serverRotations[0] / 360);
        float unwrappedYaw = yaw + 360 * scaleFactor;
        if (unwrappedYaw < serverRotations[0] - 180) {
            unwrappedYaw += 360;
        }
        else if (unwrappedYaw > serverRotations[0] + 180) {
            unwrappedYaw -= 360;
        }

        float deltaYaw = unwrappedYaw - serverRotations[0];
        return serverRotations[0] + deltaYaw;
    }

    public static MovingObjectPosition rayTrace(double range, float partialTicks, float[] rotations, EntityLivingBase ignoreCollision) {
        if (ignoreCollision != null) {
            MovingObjectPosition target = rayTraceIgnore(range, partialTicks, rotations, ignoreCollision);
            if (target != null) {
                return target;
            }
        }
        Entity targetEntity = null;
        MovingObjectPosition hitObject;
        double d0 = range;
        if (rotations == null) {
            rotations = new float[] { mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch };
        }
        hitObject = rayTraceCustom(d0, rotations[0], rotations[1]);
        double distanceTo = d0;
        Vec3 vec3 = mc.thePlayer.getPositionEyes(partialTicks);
        if (mc.playerController.extendedReach()) {
            d0 = 6.0;
            distanceTo = 6.0;
        }

        if (hitObject != null) {
            distanceTo = hitObject.hitVec.distanceTo(vec3);
        }

        Vec3 vec31 = RotationUtils.getVectorForRotation(rotations[1], rotations[0]);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
        Vec3 vec33 = null;
        float f = 1.0F;
        List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f), Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
        double d2 = distanceTo;

        for(int j = 0; j < list.size(); ++j) {
            Entity entity1 = list.get(j);
            float f1 = entity1.getCollisionBorderSize();
            AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
            MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
            if (axisalignedbb.isVecInside(vec3)) {
                if (d2 >= 0.0) {
                    targetEntity = entity1;
                    vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                    d2 = 0.0;
                }
            }
            else if (movingobjectposition != null) {
                double d3 = vec3.distanceTo(movingobjectposition.hitVec);
                if (d3 < d2 || d2 == 0.0) {
                    if (entity1 == mc.thePlayer.ridingEntity && !mc.thePlayer.canRiderInteract()) {
                        if (d2 == 0.0) {
                            targetEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                        }
                    }
                    else {
                        targetEntity = entity1;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    }
                }
            }
        }

        if (targetEntity != null && d2 < distanceTo) {
            return new MovingObjectPosition(targetEntity, vec33);
        }
        return null;
    }

    public static MovingObjectPosition rayTraceIgnore(double range, float partialTicks, float[] rotations, EntityLivingBase ignoreCollision) {
        MovingObjectPosition blockHit = rayTraceCustom(range,
                rotations[0],
                rotations[1]);

        Vec3 start = mc.thePlayer.getPositionEyes(partialTicks);
        double blockDistance = range;
        if (blockHit != null) {
            blockDistance = blockHit.hitVec.distanceTo(start);
        }

        if (ignoreCollision != null) {
            if (rotations == null) {
                rotations = new float[]{
                        mc.thePlayer.rotationYaw,
                        mc.thePlayer.rotationPitch
                };
            }
            Vec3 lookVec = RotationUtils.getVectorForRotation(
                    rotations[1],  // pitch
                    rotations[0]   // yaw
            );
            Vec3 end = start.addVector(
                    lookVec.xCoord * range,
                    lookVec.yCoord * range,
                    lookVec.zCoord * range
            );

            float f1 = ignoreCollision.getCollisionBorderSize();
            AxisAlignedBB aabb = ignoreCollision.getEntityBoundingBox()
                    .expand(f1, f1, f1);
            MovingObjectPosition ignoreMOP = aabb.calculateIntercept(start, end);

            if (aabb.isVecInside(start)) {
                return new MovingObjectPosition(ignoreCollision, start);
            }
            if (ignoreMOP != null) {
                double ignoreDist = start.distanceTo(ignoreMOP.hitVec);
                if (ignoreDist < blockDistance) {
                    return new MovingObjectPosition(
                            ignoreCollision,
                            ignoreMOP.hitVec
                    );
                }
            }
        }
        if (blockHit != null) {
            return blockHit;
        }
        return null;
    }

    public static float applyVanilla(float yaw) {
        return applyVanilla(yaw, false);
    }

    public static enum PLAYER_OFFSETS {
        EYE,
        CHEST,
        FOOT,
        NONE;

        public double getHeightOffset(Entity entity) {
            switch (this) {
                case NONE:
                case EYE:
                    return entity.getEyeHeight();
                case CHEST:
                    return entity.height / 2;
                case FOOT:
                    return 0;
            }
            return entity.getEyeHeight();
        }
    }
}
