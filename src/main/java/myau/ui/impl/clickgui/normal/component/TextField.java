package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.TextProperty;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.font.FontManager;
import myau.util.shader.Shader2D;
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

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!textProperty.isVisible()) return;

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        if (alpha < 5) return;

        Color bgColor = new Color(25, 25, 30, alpha);

        Shader2D.drawRoundedRect(x + 2, scrolledY + 2, width - 4, height - 4, 3.0f, bgColor);

        if (focused) {
            if (System.currentTimeMillis() - lastCursorToggle > 500) {
                cursorVisible = !cursorVisible;
                lastCursorToggle = System.currentTimeMillis();
            }
        }

        float textX = x + 6;
        float textY = (float) (scrolledY + (height - FontManager.productSans16.getHeight()) / 2f);
        
        if (currentText.isEmpty() && !focused) {
            FontManager.productSans16.drawString(textProperty.getName(), textX, textY, new Color(100, 100, 105, alpha).getRGB());
        } else {
            FontManager.productSans16.drawString(currentText, textX, textY, MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha));

            if (focused && cursorVisible) {
                float cursorOffset = (float) FontManager.productSans16.getStringWidth(currentText.substring(0, Math.min(cursorPos, currentText.length())));
                float cursorWidth = 1.0f;
                float cursorHeight = 10.0f;
                
                Shader2D.drawRoundedRect(textX + cursorOffset, textY + 1, cursorWidth, cursorHeight, 0.5f, 
                        new Color(MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha)));
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + height;

        if (hovered && mouseButton == 0) {
            focused = true;
            cursorPos = currentText.length();
            cursorVisible = true;
            lastCursorToggle = System.currentTimeMillis();
            return true;
        } else if (mouseButton == 0) {
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

        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
            case Keyboard.KEY_RETURN:
                textProperty.setValue(currentText);
                focused = false;
                break;
            case Keyboard.KEY_BACK:
                if (cursorPos > 0 && currentText.length() > 0) {
                    currentText = currentText.substring(0, cursorPos - 1) + currentText.substring(cursorPos);
                    cursorPos--;
                }
                break;
            case Keyboard.KEY_DELETE:
                if (cursorPos < currentText.length()) {
                    currentText = currentText.substring(0, cursorPos) + currentText.substring(cursorPos + 1);
                }
                break;
            case Keyboard.KEY_LEFT:
                if (cursorPos > 0) cursorPos--;
                break;
            case Keyboard.KEY_RIGHT:
                if (cursorPos < currentText.length()) cursorPos++;
                break;
            default:
                if (typedChar != '\0' && !Character.isISOControl(typedChar)) {
                    if (FontManager.productSans16.getStringWidth(currentText + typedChar) < width - 15) {
                        currentText = currentText.substring(0, cursorPos) + typedChar + currentText.substring(cursorPos);
                        cursorPos++;
                    }
                }
                break;
        }
        
        textProperty.setValue(currentText);
        cursorVisible = true;
        lastCursorToggle = System.currentTimeMillis();
    }

    public TextProperty getProperty() {
		return textProperty;
	}

	@Override public boolean mouseClicked(int mx, int my, int mb) { return false; }
    @Override public void mouseReleased(int mx, int my, int mb) {}
    @Override public void mouseReleased(int mx, int my, int mb, int so) {}
}