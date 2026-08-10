package myau.ui.impl.clickgui.modern.component;

import myau.property.properties.TextProperty;
import myau.ui.impl.clickgui.modern.MaterialTheme;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import org.lwjgl.input.Keyboard;

import java.awt.*;

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
        int bgColor = new Color(30, 30, 35, alpha).getRGB();
        int borderColor = focused ? MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha) :
                new Color(60, 60, 65).getRGB();

        RenderUtil.drawRoundedRect(x + 2, scrolledY, width - 4, height, 4.0f, bgColor, true, true, true, true);
        RenderUtil.drawRoundedRectOutline(x + 2, scrolledY, width - 4, height, 4.0f, 1.0f, borderColor, true, true, true, true);

        if (animationProgress > 0.5f) {
            if (focused) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCursorToggle > 500) {
                    cursorVisible = !cursorVisible;
                    lastCursorToggle = currentTime;
                }
            }

            String displayText = currentText.isEmpty() ? textProperty.getName() : currentText;
            int textColor = currentText.isEmpty() ? new Color(120, 120, 120).getRGB() :
                    MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);

            float textY = scrolledY + (height - 8) / 2f;
            if (FontManager.productSans16 != null) {
                float textX = x + 6;

                FontManager.productSans16.drawString(displayText, textX, textY, textColor);

                if (focused && cursorVisible) {
                    float cursorX = textX + (float) FontManager.productSans16.getStringWidth(displayText.substring(0, Math.min(cursorPos, displayText.length())));
                    RenderUtil.drawLine(cursorX, textY, cursorX, textY + 10, 1.0f, textColor);
                }
            } else {
                float textX = x + 6;
                mc.fontRendererObj.drawStringWithShadow(displayText, textX, textY, textColor);

                if (focused && cursorVisible) {
                    float cursorX = textX + mc.fontRendererObj.getStringWidth(displayText.substring(0, Math.min(cursorPos, displayText.length())));
                    RenderUtil.drawLine(cursorX, textY, cursorX, textY + 10, 1.0f, textColor);
                }
            }
        }
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
