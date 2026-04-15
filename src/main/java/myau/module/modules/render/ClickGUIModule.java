package myau.module.modules.render;

import org.lwjgl.input.Keyboard;

import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.ui.impl.clickgui.normal.ClickGuiScreen;

public class ClickGUIModule extends Module {

    public BooleanProperty saveGuiState = new BooleanProperty("Save GUI State", true);
    public BooleanProperty shadow = new BooleanProperty("Shadow", true);

    public IntProperty windowWidth = new IntProperty("Window Width", 600, 300, 1200);
    public IntProperty windowHeight = new IntProperty("Window Height", 400, 200, 800);
    public FloatProperty cornerRadius = new FloatProperty("Corner Radius", 8.0f, 0.0f, 20.0f);

    public ClickGUIModule() {
        super("ClickGUI",Category.RENDER, false, true);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        if (mc.theWorld == null) {
            this.setEnabled(false);
            return;
        }
        ClickGuiScreen gui = ClickGuiScreen.getInstance();
        if (gui != null) {
            mc.displayGuiScreen(gui);
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        mc.displayGuiScreen(null);
        if (mc.currentScreen == null) {
            mc.setIngameFocus();
        }
    }
}
