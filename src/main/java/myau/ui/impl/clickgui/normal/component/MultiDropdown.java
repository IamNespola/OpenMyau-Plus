package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.MultiModeProperty;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

import java.awt.*;

public class MultiDropdown extends Component {
    private static final int ITEM_HEIGHT = 18;
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

        float easedProgress = 1.0F - (float) Math.pow(1.0F - animationProgress, 4);
        if (easedProgress <= 0.0F) return;

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * easedProgress);
        RenderUtil.drawRoundedRect(x + 2, scrolledY, width - 4, headerHeight, 4.0F, new Color(30, 30, 35, alpha).getRGB(), true, true, true, true);

        String text = property.getName() + ": " + property.getSelectedModesString();
        int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
        float textY = scrolledY + (headerHeight - 8) / 2.0F;
        if (FontManager.productSans16 != null) {
            FontManager.productSans16.drawString(text, x + 6, textY, textColor);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, x + 6, scrolledY + 6, textColor);
        }

        String[] modes = property.getModes();
        float targetAnim = expanded ? modes.length * ITEM_HEIGHT : 0.0F;
        this.expandAnim = AnimationUtil.animateSmooth(targetAnim, this.expandAnim, 12.0F, deltaTime);

        if (expandAnim > 0.5F && easedProgress >= 1.0F) {
            int dropdownY = scrolledY + headerHeight + 2;
            RenderUtil.drawRoundedRect(x + 2, dropdownY, width - 4, expandAnim, 4.0F, new Color(20, 20, 24, 240).getRGB(), true, true, true, true);
            RenderUtil.scissor(x, dropdownY, width, expandAnim);
            for (int i = 0; i < modes.length; i++) {
                String mode = modes[i];
                int itemY = dropdownY + i * ITEM_HEIGHT;
                boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                boolean selected = property.isSelected(i);
                int itemColor = selected ? MaterialTheme.getRGB(MaterialTheme.PRIMARY_COLOR) : MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR_SECONDARY);
                if (hovered && !selected) itemColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, 255);
                String prefix = selected ? "✓ " : "  ";
                if (FontManager.productSans16 != null) {
                    FontManager.productSans16.drawString(prefix + mode, x + 8, itemY + 5, itemColor);
                } else {
                    mc.fontRendererObj.drawStringWithShadow(prefix + mode, x + 8, itemY + 5, itemColor);
                }
            }
            RenderUtil.releaseScissor();
        }
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
            int dropdownY = scrolledY + headerHeight + 2;
            if (mouseY >= dropdownY && mouseY <= dropdownY + expandAnim) {
                String[] modes = property.getModes();
                for (int i = 0; i < modes.length; i++) {
                    int itemY = dropdownY + i * ITEM_HEIGHT;
                    if (mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
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
