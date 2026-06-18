package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.ui.ClickGui;
import myau.ui.impl.clickgui.normal.ClickGuiScreen;
import myau.ui.impl.clickgui.raven.RavenClickGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class ClickGUIModule extends Module {
    private boolean switchingGuiStyle;

    // ── Color palette (same as TargetESP) ────────────────────────────────────
    private static final int[] COLORS = {
            0xFF4FC3F7, // Sky Blue
            0xFF81C784, // Green
            0xFFFF8A65, // Orange
            0xFFBA68C8, // Purple
            0xFFFFD54F, // Yellow
            0xFFFF6B6B, // Red
            0xFF4DB6AC, // Teal
            0xFFFFFFFF, // White
    };
    private static final String[] COLOR_NAMES = {
            "Sky Blue", "Green", "Orange", "Purple", "Yellow", "Red", "Teal", "White"
    };

    public ModeProperty accentColor = new ModeProperty("Color", 0, COLOR_NAMES);
    public ModeProperty style = new ModeProperty("Style", 0, new String[]{"Normal", "Raven B3", "Raven B4"});
    public BooleanProperty saveGuiState = new BooleanProperty("Save GUI State", true);
    public BooleanProperty shadow = new BooleanProperty("Shadow", true);

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
    }

    public void openSelectedGui() {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = getSelectedGui();
        this.switchingGuiStyle = mc.currentScreen instanceof ClickGui
                || mc.currentScreen instanceof ClickGuiScreen
                || mc.currentScreen instanceof RavenClickGui;
        try {
            mc.displayGuiScreen(screen);
        } finally {
            this.switchingGuiStyle = false;
        }
    }

    public boolean isSwitchingGuiStyle() {
        return switchingGuiStyle;
    }

    public GuiScreen getSelectedGui() {
        if (style.getValue() == 1) {
            return ClickGui.getInstance();
        }
        if (style.getValue() == 2) {
            RavenClickGui raven = RavenClickGui.getInstance();
            return raven != null ? raven : new RavenClickGui();
        }
        return ClickGuiScreen.getInstance();
    }

    @Override
    public void verifyValue(String name) {
        if ("Style".equalsIgnoreCase(name)) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof ClickGui || mc.currentScreen instanceof ClickGuiScreen) {
                openSelectedGui();
            }
        }
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
    public void onDisabled() {
        super.onDisabled();
        Minecraft.getMinecraft().displayGuiScreen(null);
        if (Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().setIngameFocus();
        }
    }
}
