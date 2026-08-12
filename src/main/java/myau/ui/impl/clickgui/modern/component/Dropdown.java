package myau.ui.impl.clickgui.modern.component;

import myau.property.properties.ModeProperty;
import myau.ui.impl.clickgui.modern.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class Dropdown extends Component {
    private static final int ITEM_HEIGHT = 18;
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
        if (!modeProperty.isVisible()) {
            return;
        }

        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * easedProgress);
        int bgColor = new Color(30, 30, 35, alpha).getRGB();

        RenderUtil.drawRoundedRect(x + 2, scrolledY, width - 4, headerHeight, 4.0f, bgColor, true, true, true, true);

        if (easedProgress > 0.9f) {
            String text = modeProperty.getName() + ": " + modeProperty.getModeString();
            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            float textY = scrolledY + (headerHeight - 8) / 2f;
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(text, x + 6, textY, textColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(text, x + 6, scrolledY + 6, textColor);
            }
        }

        List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
        float targetAnim = expanded ? modes.size() * ITEM_HEIGHT : 0f;
        this.expandAnim = AnimationUtil.animateSmooth(targetAnim, this.expandAnim, 12.0f, deltaTime);

        if (expandAnim > 0.5f && easedProgress >= 1.0f) {
            int dropdownY = scrolledY + headerHeight + 2;

            RenderUtil.drawRoundedRect(x + 2, dropdownY, width - 4, expandAnim, 4.0f, new Color(20, 20, 24, 240).getRGB(), true, true, true, true);

            RenderUtil.scissor(x, dropdownY, width, expandAnim);

            for (int i = 0; i < modes.size(); i++) {
                String mode = modes.get(i);
                int itemY = dropdownY + i * ITEM_HEIGHT;

                boolean hovered = false;
                if (mouseY >= dropdownY && mouseY <= dropdownY + expandAnim) {
                    hovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                }

                int itemColor = hovered ? MaterialTheme.getRGB(MaterialTheme.PRIMARY_COLOR) : MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR_SECONDARY);

                if (FontManager.productSans16 != null) {
                    FontManager.productSans16.drawString(mode, x + 8, itemY + 5, itemColor);
                } else {
                    mc.fontRendererObj.drawStringWithShadow(mode, x + 8, itemY + 5, itemColor);
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

        if (expanded && expandAnim > 0) {
            List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
            int dropdownY = scrolledY + headerHeight + 2;

            if (mouseY >= dropdownY && mouseY <= dropdownY + expandAnim) {
                for (int i = 0; i < modes.size(); i++) {
                    int itemY = dropdownY + i * ITEM_HEIGHT;
                    if (mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
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
