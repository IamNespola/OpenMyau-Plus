package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;

import java.awt.*;
import myau.ui.impl.clickgui.clean.CleanClickGuiScreen;
import myau.ui.impl.clickgui.normal.ClickGuiScreen;
import myau.ui.impl.clickgui.rise.RiseClickGUI;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class ClickGUIModule extends Module {

    // ── Color palette (same as TargetESP) ────────────────────────────────────
    public static final int[] COLORS = {
            0xFF4FC3F7, // Sky Blue
            0xFF81C784, // Green
            0xFFFF8A65, // Orange
            0xFFBA68C8, // Purple
            0xFFFFD54F, // Yellow
            0xFFFF6B6B, // Red
            0xFF4DB6AC, // Teal
            0xFFFFFFFF, // White
    };
    public static final String[] COLOR_NAMES = {
            "Sky Blue", "Green", "Orange", "Purple", "Yellow", "Red", "Teal", "White"
    };

    public ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Clean", "Normal", "Rise"});
    public FloatProperty cleanScale = new FloatProperty("Scale", 1.0f, 0.6f, 1.4f, () -> mode.getValue() == 0);
    public ModeProperty accentColor = new ModeProperty("Color", 0, COLOR_NAMES, () -> mode.getValue() == 1 || mode.getValue() == 2);
    public BooleanProperty saveGuiState = new BooleanProperty("Save GUI State", true, () -> mode.getValue() == 0 || mode.getValue() == 1);
    public BooleanProperty shadow = new BooleanProperty("Shadow", true, () -> mode.getValue() == 1);

    public IntProperty windowWidth = new IntProperty("Window Width", 600, 300, 1200);
    public IntProperty windowHeight = new IntProperty("Window Height", 400, 200, 800);
    public FloatProperty cornerRadius = new FloatProperty("Corner Radius", 8.0f, 0.0f, 20.0f);

    public Color getAccentColor() {
        int idx = accentColor.getValue();
        if (idx < 0 || idx >= COLORS.length) idx = 0;
        return new Color(COLORS[idx], true);
    }

    public ClickGUIModule() {
        super("ClickGUI", false);
        setKey(Keyboard.KEY_RSHIFT);
        mode.setValue(0);
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        if (Minecraft.getMinecraft().theWorld == null) {
            this.setEnabled(false);
            return;
        }
        openSelectedGui();
    }

    @Override
    public void verifyValue(String string) {
        if ("Mode".equalsIgnoreCase(string) && this.isEnabled() && Minecraft.getMinecraft().theWorld != null) {
            openSelectedGui();
        }
    }

    public void openSelectedGui() {
        if (mode.getValue() == 0) {
            Minecraft.getMinecraft().displayGuiScreen(CleanClickGuiScreen.getInstance());
        } else if (mode.getValue() == 1) {
            ClickGuiScreen gui = ClickGuiScreen.getInstance();
            if (gui != null) {
                Minecraft.getMinecraft().displayGuiScreen(gui);
            }
        } else {
            Minecraft.getMinecraft().displayGuiScreen(RiseClickGUI.getInstance());
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        Minecraft.getMinecraft().displayGuiScreen(null);
        if (Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().setIngameFocus();
        }
    }
}