package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.ModeProperty;
import myau.ui.impl.clickgui.clean.CleanTheme;
import myau.util.AnimationUtil;
import net.minecraft.client.gui.Gui;

import java.util.Arrays;
import java.util.List;

public class Dropdown extends Component {
    private static final int ITEM_HEIGHT = 12;
    private final ModeProperty modeProperty;
    private final int headerHeight;
    private boolean expanded;
    private float expandAnim = 0.0f;

    public Dropdown(ModeProperty modeProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.modeProperty = modeProperty;
        this.expanded = false;
        this.headerHeight = height;
    }

    @Override
    public int getHeight() {
        return (int) (headerHeight + expandAnim);
    }

    public ModeProperty getProperty() {
        return this.modeProperty;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!modeProperty.isVisible()) return;
        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
        float targetAnim = expanded ? modes.size() * ITEM_HEIGHT : 0f;
        this.expandAnim = AnimationUtil.animateSmooth(targetAnim, this.expandAnim, 16.0f, deltaTime);

        if (isMouseOver(mouseX, mouseY, scrollOffset)) Gui.drawRect(x, scrolledY, x + width, scrolledY + headerHeight, withAlpha(CleanTheme.ROW_HOVER, alpha));
        String name = modeProperty.getName();
        String value = modeProperty.getModeString();
        mc.fontRendererObj.drawStringWithShadow(name, x + 5, scrolledY + 3, withAlpha(CleanTheme.TEXT, alpha));
        mc.fontRendererObj.drawStringWithShadow(value, x + width - 11 - mc.fontRendererObj.getStringWidth(value), scrolledY + 3, withAlpha(CleanTheme.MUTED, alpha));
        mc.fontRendererObj.drawStringWithShadow(expanded ? "<" : ">", x + width - 8, scrolledY + 3, withAlpha(CleanTheme.MUTED, alpha));

        if (expandAnim > 0.5f) {
            int dropdownY = scrolledY + headerHeight;
            Gui.drawRect(x + 2, dropdownY, x + width, dropdownY + (int) expandAnim, withAlpha(0xEE0A0A0A, alpha));
            for (int i = 0; i < modes.size(); i++) {
                int itemY = dropdownY + i * ITEM_HEIGHT;
                if (itemY - dropdownY >= expandAnim) break;
                String mode = modes.get(i);
                boolean selected = mode.equalsIgnoreCase(value);
                boolean hovered = mouseX >= x + 2 && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                if (hovered) Gui.drawRect(x + 2, itemY, x + width, itemY + ITEM_HEIGHT, withAlpha(CleanTheme.ROW_HOVER, alpha));
                if (selected) Gui.drawRect(x + 2, itemY + 1, x + 4, itemY + ITEM_HEIGHT - 1, withAlpha(CleanTheme.ACCENT, alpha));
                mc.fontRendererObj.drawStringWithShadow(mode, x + 7, itemY + 3, withAlpha(selected ? 0xFFFFFFFF : 0xFFBDBDBD, alpha));
            }
        }
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
        int scrolledY = y - scrollOffset;
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + headerHeight) {
            if (mouseButton == 0 || mouseButton == 1) {
                expanded = !expanded;
                return true;
            }
        }
        if (expanded && expandAnim > 0) {
            List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
            int dropdownY = scrolledY + headerHeight;
            if (mouseY >= dropdownY && mouseY <= dropdownY + expandAnim) {
                for (int i = 0; i < modes.size(); i++) {
                    int itemY = dropdownY + i * ITEM_HEIGHT;
                    if (mouseX >= x + 2 && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                        if (mouseButton == 0) {
                            modeProperty.setValue(i);
                            expanded = false;
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
