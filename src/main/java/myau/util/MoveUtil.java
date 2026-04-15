package myau.util;

import myau.Myau;
import myau.management.RotationState;
import myau.module.modules.combat.TargetStrafe;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class MoveUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isForwardPressed() {
        if (mc.gameSettings.keyBindForward.isKeyDown() != mc.gameSettings.keyBindBack.isKeyDown())
            return true;
        return mc.gameSettings.keyBindLeft.isKeyDown() != mc.gameSettings.keyBindRight.isKeyDown();
    }

    public static int getForwardValue() {
        int forwardValue = 0;
        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            ++forwardValue;
        }
        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            --forwardValue;
        }
        return forwardValue;
    }

    public static int getLeftValue() {
        int leftValue = 0;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            ++leftValue;
        }
        if (mc.gameSettings.keyBindRight.isKeyDown()) {
            --leftValue;
        }
        return leftValue;
    }

    public static float getMoveYaw() {
        return adjustYaw(RotationState.isActived() ? RotationState.getSmoothedYaw() : mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveForward, mc.thePlayer.movementInput.moveStrafe);
    }

    public static float adjustYaw(float yaw, float forward, float strafe) {
        TargetStrafe targetStrafe = (TargetStrafe) Myau.moduleManager.modules.get(TargetStrafe.class);
        if (targetStrafe.isEnabled()) {
            if (!Float.isNaN(targetStrafe.getTargetYaw())) {
                return targetStrafe.getTargetYaw();
            }
        }
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        if (strafe != 0.0f) {
            float multiplier = forward == 0.0f ? 1.0f : 0.5f * Math.signum(forward);
            yaw += -90.0f * multiplier * Math.signum(strafe);
        }
        return MathHelper.wrapAngleTo180_float(yaw);
    }

    public static float getDirectionYaw() {
        if (getSpeed() == 0.0) {
            return MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        }
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0f);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.28015;
        if (getSpeedTime() > 0) {
            baseSpeed = 0.28015 * (1.0 + 0.15 * (double) getSpeedLevel());
        }
        return baseSpeed;
    }

    public static double getBaseJumpHigh(int speedLevel) {
        double jumpHeight = 0.452;
        if (speedLevel == 1) {
            jumpHeight = 0.49720000000000003;
        } else if (speedLevel >= 2) {
            jumpHeight *= 1.2;
        }
        return jumpHeight;
    }

    public static double getJumpMotion() {
        int speedLevel = 0;
        if (getSpeedTime() > 0) {
            speedLevel = getSpeedLevel();
        }
        return getBaseJumpHigh(speedLevel);
    }

    public static double getSpeed() {
        return getSpeed(mc.thePlayer.motionX, mc.thePlayer.motionZ);
    }

    public static double getSpeed(double motionX, double motionZ) {
        return Math.hypot(motionX, motionZ);
    }

    public static void setSpeed(double speed) {
        setSpeed(speed, getDirectionYaw());
    }

    public static void setSpeed(double speed, float yaw) {
        mc.thePlayer.motionX = -Math.sin(Math.toRadians(yaw)) * speed;
        mc.thePlayer.motionZ = Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static void addSpeed(double speed, float yaw) {
        mc.thePlayer.motionX += -Math.sin(Math.toRadians(yaw)) * speed;
        mc.thePlayer.motionZ += Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static int getSpeedLevel() {
        int speedLevel = 0;
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            speedLevel = (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return speedLevel;
    }

    public static int getSpeedTime() {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration();
        }
        return 0;
    }

    public static float getAllowedHorizontalDistance() {
        float slipperiness = mc.thePlayer.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(mc.thePlayer.posZ))).getBlock().slipperiness * 0.91f;
        return mc.thePlayer.getAIMoveSpeed() * (0.16277136f / (slipperiness * slipperiness * slipperiness));
    }

    public static double[] predictMovement() {
        float strafeInput = (float) getLeftValue() * 0.98f;
        float forwardInput = (float) getForwardValue() * 0.98f;
        float inputMagnitude = strafeInput * strafeInput + forwardInput * forwardInput;
        if (inputMagnitude >= 1.0E-4f) {
            inputMagnitude = MathHelper.sqrt_float(inputMagnitude);
            if (inputMagnitude < 1.0f) {
                inputMagnitude = 1.0f;
            }
            inputMagnitude = getAllowedHorizontalDistance() / inputMagnitude;
            float sinYaw = MathHelper.sin(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            float cosYaw = MathHelper.cos(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            strafeInput *= inputMagnitude;
            forwardInput *= inputMagnitude;
            return new double[]{strafeInput * cosYaw - forwardInput * sinYaw, forwardInput * cosYaw + strafeInput * sinYaw};
        }
        return new double[]{0.0, 0.0};
    }

    public static void fixStrafe(float targetYaw) {
        float angle = MathHelper.wrapAngleTo180_float(adjustYaw(mc.thePlayer.rotationYaw, getForwardValue(), getLeftValue()) - targetYaw + 22.5f);
        switch ((int) (angle + 180.0f) / 45 % 8) {
            case 0: {
                mc.thePlayer.movementInput.moveForward = -1.0f;
                mc.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            }
            case 1: {
                mc.thePlayer.movementInput.moveForward = -1.0f;
                mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 2: {
                mc.thePlayer.movementInput.moveForward = 0.0f;
                mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 3: {
                mc.thePlayer.movementInput.moveForward = 1.0f;
                mc.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 4: {
                mc.thePlayer.movementInput.moveForward = 1.0f;
                mc.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            }
            case 5: {
                mc.thePlayer.movementInput.moveForward = 1.0f;
                mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
            case 6: {
                mc.thePlayer.movementInput.moveForward = 0.0f;
                mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
            case 7: {
                mc.thePlayer.movementInput.moveForward = -1.0f;
                mc.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
        }
        if (mc.thePlayer.movementInput.sneak) {
            mc.thePlayer.movementInput.moveForward *= 0.3f;
            mc.thePlayer.movementInput.moveStrafe *= 0.3f;
        }
    }
}
