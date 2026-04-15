package myau.config;

import myau.Myau;
import myau.module.modules.render.Animations;

/**
 * Thin wrapper that holds animation settings
 * so everything persists through Uzi's config system.
 * Original logic by syuto/animations-1.6, integrated into Uzi
 */
public class AnimationConfig {
    public static AnimationMode mode       = AnimationMode.VANILLA;
    public static int           scale      = 100;
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
                scale = animModule.scale.getValue();
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

    public static int getScale() {
        return scale;
    }

    public static void setScale(int scale) {
        AnimationConfig.scale = scale;
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
