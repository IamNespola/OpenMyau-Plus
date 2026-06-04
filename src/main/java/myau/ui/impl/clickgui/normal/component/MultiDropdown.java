package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.MultiModeProperty;
import myau.ui.impl.clickgui.clean.CleanTheme;
import myau.util.AnimationUtil;
import net.minecraft.client.gui.Gui;

public class MultiDropdown extends Component {
    private static final int ITEM_HEIGHT = 12;
    private final MultiModeProperty property;
    private final int headerHeight;
    private boolean expanded;
    private float expandAnim = 0.0F;

    public MultiDropdown(MultiModeProperty property, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.property = property;
        this.headerHeight = height;
    }

    @Override
    public int getHeight() {
        return (int) (headerHeight + expandAnim);
    }

    public MultiModeProperty getProperty() {
        return this.property;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!property.isVisible()) return;
        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        String[] modes = property.getModes();
        float targetAnim = expanded ? modes.length * ITEM_HEIGHT : 0.0F;
        this.expandAnim = AnimationUtil.animateSmooth(targetAnim, this.expandAnim, 16.0F, deltaTime);

        if (isMouseOver(mouseX, mouseY, scrollOffset)) Gui.drawRect(x, scrolledY, x + width, scrolledY + headerHeight, withAlpha(CleanTheme.ROW_HOVER, alpha));
        String name = trimToWidth(property.getName(), Math.max(12, width - 34));
        String value = trimToWidth(property.getSelectedModesString(), Math.max(12, width - 22 - mc.fontRendererObj.getStringWidth(name)));
        int valueX = x + width - 11 - mc.fontRendererObj.getStringWidth(value);
        mc.fontRendererObj.drawStringWithShadow(name, x + 5, scrolledY + 3, withAlpha(CleanTheme.TEXT, alpha));
        mc.fontRendererObj.drawStringWithShadow(value, Math.max(x + 5 + mc.fontRendererObj.getStringWidth(name) + 4, valueX), scrolledY + 3, withAlpha(CleanTheme.MUTED, alpha));
        mc.fontRendererObj.drawStringWithShadow(expanded ? "<" : ">", x + width - 8, scrolledY + 3, withAlpha(CleanTheme.MUTED, alpha));

        if (expandAnim > 0.5F) {
            int dropdownY = scrolledY + headerHeight;
            Gui.drawRect(x + 2, dropdownY, x + width, dropdownY + (int) expandAnim, withAlpha(0xEE0A0A0A, alpha));
            for (int i = 0; i < modes.length; i++) {
                int itemY = dropdownY + i * ITEM_HEIGHT;
                if (itemY - dropdownY >= expandAnim) break;
                boolean selected = property.isSelected(i);
                boolean hovered = mouseX >= x + 2 && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                if (hovered) Gui.drawRect(x + 2, itemY, x + width, itemY + ITEM_HEIGHT, withAlpha(CleanTheme.ROW_HOVER, alpha));
                if (selected) Gui.drawRect(x + 2, itemY + 1, x + 4, itemY + ITEM_HEIGHT - 1, withAlpha(CleanTheme.ACCENT, alpha));
                mc.fontRendererObj.drawStringWithShadow(trimToWidth(modes[i], width - 11), x + 7, itemY + 3, withAlpha(selected ? 0xFFFFFFFF : 0xFFBDBDBD, alpha));
            }
        }
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) return "";
        if (maxWidth <= 0 || mc.fontRendererObj.getStringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = mc.fontRendererObj.getStringWidth(ellipsis);
        if (maxWidth <= ellipsisWidth) return ellipsis;
        String trimmed = text;
        while (!trimmed.isEmpty() && mc.fontRendererObj.getStringWidth(trimmed) + ellipsisWidth > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + headerHeight) {
            if (mouseButton == 0 || mouseButton == 1) {
                expanded = !expanded;
                return true;
            }
        }
        if (expanded && expandAnim > 0.0F) {
            int dropdownY = scrolledY + headerHeight;
            if (mouseY >= dropdownY && mouseY <= dropdownY + expandAnim) {
                String[] modes = property.getModes();
                for (int i = 0; i < modes.length; i++) {
                    int itemY = dropdownY + i * ITEM_HEIGHT;
                    if (mouseX >= x + 2 && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                        if (mouseButton == 0) {
                            property.toggle(i);
                            return true;
                        }
                    }
                }
            }
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
