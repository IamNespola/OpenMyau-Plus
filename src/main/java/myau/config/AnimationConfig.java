package myau.config;

import myau.Myau;
import myau.module.modules.Animations;

/**
 * Thin wrapper that holds animation settings
 * so everything persists through Uzi's config system.
 * Original logic by syuto/animations-1.6, integrated into Uzi
 */
public class AnimationConfig {
    public static AnimationMode mode       = AnimationMode.VANILLA;
    public static int           renderMode = 1;
    public static int           scale      = 100;
    public static float         itemSize   = 0.0F;
    public static float         blockPosX  = 0.0F;
    public static float         blockPosY  = 0.0F;
    public static float         blockPosZ  = 0.0F;
    public static int           swingSpeed = 6;
    public static boolean       enabled    = true;

    /**
     * Sync configuration from the Animations module
     */
    public static void sync() {
        try {
            Animations animModule = (Animations) Myau.moduleManager.modules.get(Animations.class);
            if (animModule != null && animModule.isEnabled()) {
                enabled = true;
                AnimationMode[] modes = AnimationMode.values();
                if (animModule.mode.getValue() < modes.length) {
                    mode = modes[animModule.mode.getValue()];
                }
                renderMode = animModule.render.getValue();
                scale = animModule.scale.getValue();
                itemSize = animModule.itemSize.getValue();
                blockPosX = animModule.blockPosX.getValue();
                blockPosY = animModule.blockPosY.getValue();
                blockPosZ = animModule.blockPosZ.getValue();
                swingSpeed = animModule.swingSpeed.getValue();
            } else {
                enabled = false;
            }
        } catch (Exception ignored) {
        }
    }

    public static AnimationMode getMode() {
        return mode;
    }

    public static void setMode(AnimationMode mode) {
        AnimationConfig.mode = mode;
    }

    public static int getRenderMode() {
        return renderMode;
    }

    public static void setRenderMode(int renderMode) {
        AnimationConfig.renderMode = renderMode;
    }

    public static int getScale() {
        return scale;
    }

    public static void setScale(int scale) {
        AnimationConfig.scale = scale;
    }

    public static float getItemSize() {
        return itemSize;
    }

    public static void setItemSize(float itemSize) {
        AnimationConfig.itemSize = itemSize;
    }

    public static float getBlockPosX() {
        return blockPosX;
    }

    public static void setBlockPosX(float blockPosX) {
        AnimationConfig.blockPosX = blockPosX;
    }

    public static float getBlockPosY() {
        return blockPosY;
    }

    public static void setBlockPosY(float blockPosY) {
        AnimationConfig.blockPosY = blockPosY;
    }

    public static float getBlockPosZ() {
        return blockPosZ;
    }

    public static void setBlockPosZ(float blockPosZ) {
        AnimationConfig.blockPosZ = blockPosZ;
    }

    public static int getSwingSpeed() {
        return swingSpeed;
    }

    public static void setSwingSpeed(int swingSpeed) {
        AnimationConfig.swingSpeed = swingSpeed;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        AnimationConfig.enabled = enabled;
    }
}
