package myau.ui.impl.clickgui.normal.component;

import lombok.Getter;
import myau.property.Property;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.ui.impl.clickgui.clean.CleanTheme;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Mouse;

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
        if (!property.isVisible()) return;
        if (this.dragging) {
            if (Mouse.isButtonDown(0)) updateSliderValue(mouseX);
            else this.dragging = false;
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
        String name = property.getName();
        String valStr = round(value) + (this.property instanceof PercentProperty ? "%" : "");
        mc.fontRendererObj.drawStringWithShadow(name, x + 5, scrolledY + 2, withAlpha(CleanTheme.TEXT, alpha));
        mc.fontRendererObj.drawStringWithShadow(valStr, x + width - 5 - mc.fontRendererObj.getStringWidth(valStr), scrolledY + 2, withAlpha(CleanTheme.MUTED, alpha));

        int trackY = scrolledY + height - 5;
        int trackX = x + 5;
        int trackWidth = width - 10;
        Gui.drawRect(trackX, trackY, trackX + trackWidth, trackY + 2, withAlpha(0xFF303030, alpha));
        Gui.drawRect(trackX, trackY, trackX + (int) (trackWidth * fillProgress), trackY + 2, withAlpha(CleanTheme.ACCENT, alpha));
        int knobX = trackX + (int) (trackWidth * fillProgress);
        Gui.drawRect(knobX - 1, trackY - 2, knobX + 1, trackY + 4, withAlpha(0xFFFFFFFF, alpha));
    }

    private void updateSliderValue(int mouseX) {
        float currentTrackX = x + 5;
        float currentTrackWidth = width - 10;
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
            newValue = Math.max(min, Math.min(max, bd.doubleValue()));
            property.setValue((float) newValue);
        }
    }

    private double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
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
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY + height - 9 && mouseY <= scrolledY + height) {
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
