package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.TextProperty;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import org.lwjgl.input.Keyboard;


public class TextField extends Component {
    private final TextProperty textProperty;
    private String currentText;
    private boolean focused = false;
    private long lastCursorToggle = 0;
    private boolean cursorVisible = true;
    private int cursorPos = 0;

    public TextField(TextProperty textProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.textProperty = textProperty;
        this.currentText = textProperty.getValue();
        this.cursorPos = currentText.length();
    }

    public TextProperty getProperty() {
        return this.textProperty;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!textProperty.isVisible()) {
            return;
        }

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        int bgColor = withAlpha(0xFF15151B, alpha);
        int innerColor = withAlpha(0xFF202028, alpha);
        int borderColor = focused ? withAlpha(0xFFE00012, alpha) : withAlpha(0xFF34343C, alpha);
        int accentColor = withAlpha(0xFFE00012, focused ? alpha : Math.min(110, alpha));

        RenderUtil.drawRoundedRect(x + 2, scrolledY + 1, width - 4, height - 2, 4.0f, bgColor, true, true, true, true);
        RenderUtil.drawRoundedRect(x + 4, scrolledY + 3, width - 8, height - 6, 3.0f, innerColor, true, true, true, true);
        RenderUtil.drawRoundedRectOutline(x + 2, scrolledY + 1, width - 4, height - 2, 4.0f, focused ? 1.2f : 0.8f, borderColor, true, true, true, true);
        RenderUtil.drawRoundedRect(x + 4, scrolledY + 4, 2, height - 8, 1.0f, accentColor, true, true, true, true);

        if (animationProgress > 0.5f) {
            if (focused) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCursorToggle > 500) {
                    cursorVisible = !cursorVisible;
                    lastCursorToggle = currentTime;
                }
            }

            String displayText = currentText.isEmpty() ? textProperty.getName() : currentText;
            int textColor = currentText.isEmpty() ? withAlpha(0xFF8A8A8A, alpha) : withAlpha(0xFFE6E6E6, alpha);

            float textY = scrolledY + (height - 8) / 2f;
            if (FontManager.productSans16 != null) {
                float textX = x + 10;

                FontManager.productSans16.drawString(displayText, textX, textY, textColor);

                if (focused && cursorVisible) {
                    float cursorX = textX + (float) FontManager.productSans16.getStringWidth(displayText.substring(0, Math.min(cursorPos, displayText.length())));
                    RenderUtil.drawLine(cursorX, textY, cursorX, textY + 10, 1.0f, withAlpha(0xFFE00012, alpha));
                }
            } else {
                float textX = x + 10;
                mc.fontRendererObj.drawStringWithShadow(displayText, textX, textY, textColor);

                if (focused && cursorVisible) {
                    float cursorX = textX + mc.fontRendererObj.getStringWidth(displayText.substring(0, Math.min(cursorPos, displayText.length())));
                    RenderUtil.drawLine(cursorX, textY, cursorX, textY + 10, 1.0f, withAlpha(0xFFE00012, alpha));
                }
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

        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + height) {
            if (mouseButton == 0) {
                focused = true;
                cursorPos = currentText.length();
                cursorVisible = true;
                lastCursorToggle = System.currentTimeMillis();
                return true;
            }
        } else {
            if (focused) {
                textProperty.setValue(currentText);
                focused = false;
            }
        }
        return false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) return;

        if (keyCode == Keyboard.KEY_BACK) {
            if (cursorPos > 0 && currentText.length() > 0) {
                currentText = currentText.substring(0, cursorPos - 1) + currentText.substring(cursorPos);
                cursorPos--;
            }
        } else if (keyCode == Keyboard.KEY_DELETE) {
            if (cursorPos < currentText.length()) {
                currentText = currentText.substring(0, cursorPos) + currentText.substring(cursorPos + 1);
            }
        } else if (keyCode == Keyboard.KEY_LEFT) {
            if (cursorPos > 0) {
                cursorPos--;
            }
        } else if (keyCode == Keyboard.KEY_RIGHT) {
            if (cursorPos < currentText.length()) {
                cursorPos++;
            }
        } else if (keyCode == Keyboard.KEY_HOME) {
            cursorPos = 0;
        } else if (keyCode == Keyboard.KEY_END) {
            cursorPos = currentText.length();
        } else if (typedChar != '\0' && !Character.isISOControl(typedChar)) {
            currentText = currentText.substring(0, cursorPos) + typedChar + currentText.substring(cursorPos);
            cursorPos++;
        }

        textProperty.setValue(currentText);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
    }
}
