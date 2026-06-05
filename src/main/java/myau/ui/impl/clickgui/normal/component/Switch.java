package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.BooleanProperty;
import myau.ui.impl.clickgui.clean.CleanTheme;
import net.minecraft.client.gui.Gui;

public class Switch extends Component {
    private final BooleanProperty booleanProperty;

    public Switch(BooleanProperty booleanProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.booleanProperty = booleanProperty;
    }

    public BooleanProperty getProperty() {
        return this.booleanProperty;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!booleanProperty.isVisible()) return;
        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        boolean enabled = booleanProperty.getValue();
        if (isMouseOver(mouseX, mouseY, scrollOffset)) Gui.drawRect(x, scrolledY, x + width, scrolledY + height, withAlpha(CleanTheme.ROW_HOVER, alpha));
        if (enabled) Gui.drawRect(x + 2, scrolledY + 1, x + 4, scrolledY + height - 1, withAlpha(CleanTheme.ACCENT, alpha));
        String valueText = enabled ? "On" : "Off";
        String name = booleanProperty.getName();
        if (mc.fontRendererObj.getStringWidth(name) > width - 10) {
            name = mc.fontRendererObj.trimStringToWidth(name, Math.max(12, width - 10));
        }
        mc.fontRendererObj.drawStringWithShadow(name, x + 7, scrolledY + 3, withAlpha(enabled ? 0xFFFFFFFF : 0xFFBDBDBD, alpha));
        int valueY = height >= 22 ? scrolledY + 12 : scrolledY + 3;
        mc.fontRendererObj.drawStringWithShadow(valueText, x + width - 5 - mc.fontRendererObj.getStringWidth(valueText), valueY, withAlpha(enabled ? CleanTheme.ACCENT : CleanTheme.MUTED, alpha));
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + height) {
            booleanProperty.setValue(!booleanProperty.getValue());
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
