package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class Gui extends Module {
    public static SliderSetting guiScale;
    public static SliderSetting scrollSpeed;
    public static ButtonSetting removePlayerModel;
    public static ButtonSetting darkBackground;
    public static ButtonSetting limitToScreen;
    public static ButtonSetting removeWatermark;
    public static ButtonSetting rainBowOutlines;

    public Gui() {
        super("Gui", category.client, 54);
        this.registerSetting(guiScale = new SliderSetting("Gui scale", 1, new String[]{ "Small", "Normal", "Large" }));
        this.registerSetting(scrollSpeed = new SliderSetting("Scroll speed", 50, 2, 90, 1));
        this.registerSetting(darkBackground = new ButtonSetting("Dark background", true));
        this.registerSetting(limitToScreen = new ButtonSetting("Limit to screen", false));
        this.registerSetting(rainBowOutlines = new ButtonSetting("Rainbow outlines", true));
        this.registerSetting(removePlayerModel = new ButtonSetting("Remove player model", false));
        this.registerSetting(removeWatermark = new ButtonSetting("Remove watermark", false));
    }

    public void onEnable() {
        // Script settings now live in Myau+'s own module/property system (see ScriptModule),
        // so this opens Myau+'s GUI instead of Raven's own clickgui, which is otherwise dead
        // weight left unreachable to avoid two independent GUIs fighting over the same key.
        if (Utils.nullCheck()) {
            myau.module.Module myauGui = myau.Myau.moduleManager.getModule("ClickGui");
            if (myauGui != null) {
                myauGui.setEnabled(true);
            }
        }

        this.disable();
    }
}
