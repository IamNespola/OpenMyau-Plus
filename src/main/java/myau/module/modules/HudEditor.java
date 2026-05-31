package myau.module.modules;

import myau.module.Module;
import myau.ui.hudeditor.HudEditorScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class HudEditor extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public HudEditor() {
        super("HudEditor", false, false, "Edit HUD element positions");
        setKey(Keyboard.KEY_NONE);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        mc.displayGuiScreen(new HudEditorScreen());
    }
}
