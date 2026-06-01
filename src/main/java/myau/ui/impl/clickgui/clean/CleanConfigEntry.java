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
        boolean active = configName.equalsIgnoreCase(Config.lastConfig);
        if (isMouseOver(mouseX, mouseY, scrollOffset)) Gui.drawRect(x, scrolledY, x + width, scrolledY + height, CleanTheme.ROW_HOVER);
        mc.fontRendererObj.drawStringWithShadow(configName, x + 5, scrolledY + 3, active ? 0xFFFFFFFF : 0xFFBDBDBD);
    }

    public float getCurrentHeight() {
        return height;
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
