package myau.ui.impl.clickgui.normal.component;

import lombok.Getter;
import myau.property.Property;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.ui.impl.clickgui.normal.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.font.FontManager;
import myau.util.shader.Shader2D;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Slider extends Component {
    @Getter
    private final Property property;
    private final double min, max, step;
    private boolean dragging;
    private float visualProgress = 0f;

    public Slider(Property property, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.property = property;
        
        if (property instanceof IntProperty) {
            this.min = ((IntProperty) property).getMinimum();
            this.max = ((IntProperty) property).getMaximum();
            this.step = 1.0;
        } else if (property instanceof PercentProperty) {
            this.min = 0;
            this.max = 100;
            this.step = 1.0;
        } else {
            FloatProperty fp = (FloatProperty) property;
            this.min = fp.getMinimum();
            this.max = fp.getMaximum();
            this.step = 0.05;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!property.isVisible()) return;

        if (this.dragging) {
            if (Mouse.isButtonDown(0)) {
                updateSliderValue(mouseX);
            } else {
                this.dragging = false;
            }
        }

        double currentValue = ((Number) property.getValue()).doubleValue();
        float targetProgress = (float) ((currentValue - min) / (max - min));
        
        this.visualProgress = AnimationUtil.animateSmooth(targetProgress, visualProgress, 14f, deltaTime);

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        if (alpha < 10) return;

        String valStr = round(currentValue) + (this.property instanceof PercentProperty ? "%" : "");
        int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
        
        if (FontManager.productSans16 != null) {
            FontManager.productSans16.drawString(property.getName(), x + 2, scrolledY + 2, textColor);
            float valW = (float) FontManager.productSans16.getStringWidth(valStr);
            FontManager.productSans16.drawString(valStr, x + width - valW - 2, scrolledY + 2, textColor);
        }

        float trackHeight = 3.0f;
        float trackY = scrolledY + height - 6;
        float trackX = x + 2;
        float trackWidth = width - 4;

        Shader2D.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, 1.5f, new Color(45, 45, 50, alpha));

        float fillWidth = trackWidth * visualProgress;
        if (fillWidth > 0) {
            Shader2D.drawRoundedRect(trackX, trackY, fillWidth, trackHeight, 1.5f, 
            		MaterialTheme.PRIMARY_COLOR.darker());
            
            Shader2D.drawRoundedRect(trackX, trackY, fillWidth, trackHeight, 1.5f, 
                    new Color(MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha)));
        }

        float knobRadius = 5.0f;
        float knobX = trackX + (trackWidth * visualProgress) - (knobRadius / 2f);
        float knobY = trackY + (trackHeight / 2f) - (knobRadius / 2f);
        
        Color knobColor = dragging ? Color.WHITE : new Color(200, 200, 200, alpha);
        Shader2D.drawRoundedRect(knobX, knobY, knobRadius, knobRadius, knobRadius / 2f, knobColor);
    }

    private void updateSliderValue(int mouseX) {
        float trackX = x + 2;
        float trackWidth = width - 4;
        double progress = Math.max(0, Math.min(1, (mouseX - trackX) / trackWidth));
        double newValue = min + (max - min) * progress;

        if (property instanceof IntProperty || property instanceof PercentProperty) {
            property.setValue((int) Math.round(newValue));
        } else {
            double steppedValue = Math.round(newValue / step) * step;
            property.setValue((float) Math.max(min, Math.min(max, steppedValue)));
        }
    }

    private double round(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (!property.isVisible()) return false;
        
        int scrolledY = y - scrollOffset;
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY + height - 12 && mouseY <= scrolledY + height) {
            if (mouseButton == 0) {
                this.dragging = true;
                updateSliderValue(mouseX);
                return true;
            }
        }
        return false;
    }

    @Override public void mouseReleased(int mx, int my, int mb) { this.dragging = false; }
    @Override public boolean mouseClicked(int mx, int my, int mb) { return false; }
    @Override public void keyTyped(char tc, int kc) {}
}