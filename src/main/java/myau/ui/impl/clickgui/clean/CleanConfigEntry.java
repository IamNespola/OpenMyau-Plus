package myau.ui.impl.clickgui.clean;

import myau.config.Config;
import myau.ui.impl.clickgui.normal.component.Component;
import net.minecraft.client.gui.Gui;

public class CleanConfigEntry extends Component {
    private final String configName;

    public CleanConfigEntry(String configName, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.configName = configName;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        boolean active = configName.equalsIgnoreCase(Config.lastConfig);
        if (isMouseOver(mouseX, mouseY, scrollOffset)) Gui.drawRect(x, scrolledY, x + width, scrolledY + height, withAlpha(CleanTheme.ROW_HOVER, alpha));
        if (active) Gui.drawRect(x, scrolledY + 1, x + 2, scrolledY + height - 1, withAlpha(CleanTheme.ACCENT, alpha));
        mc.fontRendererObj.drawStringWithShadow(configName, x + 5, scrolledY + 3, active ? withAlpha(0xFFFFFFFF, alpha) : withAlpha(0xFFBDBDBD, alpha));
    }

    public float getCurrentHeight() {
        return height;
    }

    public boolean matches(String searchQuery) {
        return searchQuery == null || searchQuery.trim().isEmpty() || configName.toLowerCase().contains(searchQuery.trim().toLowerCase());
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOver(mouseX, mouseY, scrollOffset) && mouseButton == 0) {
            new Config(configName, false).load();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }
}
