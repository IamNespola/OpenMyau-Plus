package myau.ui.impl.clickgui.modern.component;

import lombok.Getter;
import myau.property.Property;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.ui.impl.clickgui.modern.MaterialTheme;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Slider extends Component {
    @Getter
    private final Property property;
    private final double min, max;
    private final double step;
    private boolean dragging;

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
            this.min = ((FloatProperty) property).getMinimum();
            this.max = ((FloatProperty) property).getMaximum();
            this.step = 0.05;
        }
        this.dragging = false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!property.isVisible()) {
            return;
        }

        if (this.dragging) {
            if (Mouse.isButtonDown(0)) {
                updateSliderValue(mouseX);
            } else {
                this.dragging = false;
            }
        }

        double value, min, max;
        if (this.property instanceof IntProperty) {
            min = ((IntProperty) this.property).getMinimum();
            max = ((IntProperty) this.property).getMaximum();
            value = (Integer) this.property.getValue();
        } else if (this.property instanceof PercentProperty) {
            min = 0;
            max = 100;
            value = (Integer) this.property.getValue();
        } else {
            min = ((FloatProperty) this.property).getMinimum();
            max = ((FloatProperty) this.property).getMaximum();
            value = (Float) this.property.getValue();
        }

        double fillProgress = (max - min != 0) ? (value - min) / (max - min) : 0;
        fillProgress = Math.max(0, Math.min(1, fillProgress));

        int scrolledY = y - scrollOffset;
        int alpha = (int) (255 * animationProgress);
        if (alpha < 5) return;

        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);

        if (easedProgress > 0.5f) {
            String name = property.getName();
            String valStr = round(value) + (this.property instanceof PercentProperty ? "%" : "");
            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            float textY = scrolledY + 2;
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(name, x + 2, textY, textColor);
                float valW = (float) FontManager.productSans16.getStringWidth(valStr);
                FontManager.productSans16.drawString(valStr, x + width - valW - 2, textY, textColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(name, x + 2, textY, textColor);
                mc.fontRendererObj.drawStringWithShadow(valStr, x + width - mc.fontRendererObj.getStringWidth(valStr) - 2, textY, textColor);
            }
        }

        int trackHeight = 4;
        int trackY = scrolledY + height - 8;
        int trackX = x + 2;
        int trackWidth = width - 4;

        RenderUtil.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, trackHeight / 2f, new Color(40, 40, 45).getRGB(), true, true, true, true);

        float fillWidth = (float) (trackWidth * fillProgress);
        int accentColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha);
        RenderUtil.drawRoundedRect(trackX, trackY, fillWidth, trackHeight, trackHeight / 2f, accentColor, true, false, false, true);

        float knobX = trackX + fillWidth - 2;
        RenderUtil.drawRoundedRect(knobX, trackY - 2, 4, trackHeight + 4, 2, -1, true, true, true, true);
    }

    private void updateSliderValue(int mouseX) {
        float currentTrackX = x + 2;
        float currentTrackWidth = width - 4;

        double progress = (mouseX - currentTrackX) / currentTrackWidth;
        progress = Math.max(0, Math.min(1, progress));

        double newValue = min + (max - min) * progress;

        if (property instanceof IntProperty) {
            property.setValue((int) Math.round(newValue));
        } else if (property instanceof PercentProperty) {
            property.setValue((int) Math.round(newValue));
        } else {
            double steppedValue = Math.round(newValue / step) * step;
            BigDecimal bd = new BigDecimal(steppedValue);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            newValue = bd.doubleValue();
            newValue = Math.max(min, Math.min(max, newValue));
            property.setValue((float) newValue);
        }
    }

    private double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
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

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        this.dragging = false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        this.dragging = false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }
}
