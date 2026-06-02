package myau.ui.impl.clickgui.normal.component;

import lombok.Getter;
import myau.module.Module;
import myau.ui.impl.clickgui.clean.CleanTheme;
import myau.util.KeyBindUtil;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;

public class KeybindComponent extends Component {
    private final Module module;
    @Getter
    private boolean binding;

    public KeybindComponent(Module module, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.module = module;
        this.binding = false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        if (isMouseOver(mouseX, mouseY, scrollOffset)) Gui.drawRect(x, scrolledY, x + width, scrolledY + height, withAlpha(CleanTheme.ROW_HOVER, alpha));
        String bindText;
        if (binding) bindText = "...";
        else bindText = module.getKey() == Keyboard.KEY_NONE ? "None" : KeyBindUtil.getKeyName(module.getKey());
        mc.fontRendererObj.drawStringWithShadow("Keybind", x + 5, scrolledY + 3, withAlpha(CleanTheme.TEXT, alpha));
        mc.fontRendererObj.drawStringWithShadow(bindText, x + width - 5 - mc.fontRendererObj.getStringWidth(bindText), scrolledY + 3, withAlpha(binding ? CleanTheme.ACCENT : CleanTheme.MUTED, alpha));
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOver(mouseX, mouseY, scrollOffset) && mouseButton == 0) {
            this.binding = !this.binding;
            return true;
        }
        if (this.binding && !isMouseOver(mouseX, mouseY, scrollOffset)) this.binding = false;
        return false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.binding) {
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_ESCAPE) module.setKey(Keyboard.KEY_NONE);
            else module.setKey(keyCode);
            this.binding = false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }
}
