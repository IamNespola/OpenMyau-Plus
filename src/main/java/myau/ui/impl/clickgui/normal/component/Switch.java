package myau.ui.impl.clickgui.normal.component;

import myau.property.properties.BooleanProperty;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.font.FontManager;
import myau.util.shader.Shader2D;

import java.awt.*;

public class Switch extends Component {
    private final BooleanProperty booleanProperty;
    private float toggleAnim;

    public BooleanProperty getProperty() {
        return booleanProperty;
    }

    public Switch(BooleanProperty booleanProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.booleanProperty = booleanProperty;
        this.toggleAnim = booleanProperty.getValue() ? 1.0f : 0.0f;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!booleanProperty.isVisible()) return;

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        if (alpha < 5) return;

        float target = booleanProperty.getValue() ? 1.0f : 0.0f;
        this.toggleAnim = AnimationUtil.animateSmooth(target, this.toggleAnim, 14.0f, deltaTime);

        if (FontManager.productSans16 != null) {
            String nameStr = booleanProperty.getName().replace("-", " ");
            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            float textY = (float) (scrolledY + (height - FontManager.productSans16.getHeight()) / 2f);
            FontManager.productSans16.drawString(nameStr, x + 4, textY, textColor);
        }

        float switchW = 18;
        float switchH = 9;
        float rightPadding = 5; 
        float switchX = (x + width) - switchW - rightPadding;
        float switchY = scrolledY + (height - switchH) / 2f;

        Color disabledColor = new Color(55, 55, 60, alpha);
        Color enabledColor = new Color(MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha));
        int backgroundRGB = AnimationUtil.interpolateColor(disabledColor.getRGB(), enabledColor.getRGB(), toggleAnim);
        
        Shader2D.drawRoundedRect(switchX, switchY, switchW, switchH, switchH / 2f, new Color(backgroundRGB, true));

        float knobSize = switchH + 2f; 
        float maxTravel = switchW - knobSize + 2f;
        float knobX = (switchX - 1f) + (toggleAnim * maxTravel);
        float knobY = switchY + (switchH / 2f) - (knobSize / 2f);

        Shader2D.drawRoundedRect(knobX, knobY, knobSize, knobSize, knobSize / 2f, new Color(255, 255, 255, alpha));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (!booleanProperty.isVisible()) return false;
        
        int scrolledY = y - scrollOffset;
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + height) {
            if (mouseButton == 0) {
                booleanProperty.setValue(!booleanProperty.getValue());
                return true;
            }
        }
        return false;
    }

    @Override public boolean mouseClicked(int mx, int my, int mb) { return false; }
    @Override public void mouseReleased(int mx, int my, int mb) {}
    @Override public void keyTyped(char tc, int kc) {}
}