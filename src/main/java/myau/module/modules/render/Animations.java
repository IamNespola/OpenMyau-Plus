package myau.module.modules.render;

import myau.config.AnimationConfig;
import myau.config.AnimationMode;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;

/**
 * Animations Module
 * Original logic by syuto/animations-1.6, integrated into Uzi
 */
public class Animations extends Module {

    public final ModeProperty mode = new ModeProperty("Mode", 0,
            new String[]{"VANILLA", "EXHIBITION", "ETB", "SIGMA", "DORTWARE", "PLAIN",
                    "SPIN", "AVATAR", "SWONG", "SWANG", "SWANK", "STYLES",
                    "NUDGE", "PUNCH", "JIGSAW", "SLIDE"});

    public final IntProperty scale = new IntProperty("Scale", 100, 50, 150);
    public final IntProperty swingSpeed = new IntProperty("SwingSpeed", 0, 0, 100);

    public Animations() {
        super("Animations", Category.RENDER,true);
    }

    @Override
    public void onEnabled() {
        syncConfig();
    }

    @Override
    public void onDisabled() {
        AnimationConfig.setEnabled(false);
    }

    private void syncConfig() {
        AnimationConfig.setEnabled(true);
        AnimationMode[] modes = AnimationMode.values();
        if (mode.getValue() < modes.length) {
            AnimationConfig.setMode(modes[mode.getValue()]);
        }
        AnimationConfig.setScale(scale.getValue());
        AnimationConfig.setSwingSpeed(swingSpeed.getValue());
    }

    public void onUpdate() {
        if (this.isEnabled()) {
            syncConfig();
        }
    }

    @Override
    public String[] getSuffix() {
        String[] modes = {"Vanilla", "Exhibition", "ETB", "Sigma", "Dortware", "Plain",
                "Spin", "Avatar", "Swong", "Swang", "Swank", "Styles",
                "Nudge", "Punch", "Jigsaw", "Slide"};
        return new String[]{modes[mode.getValue()]};
    }
}
