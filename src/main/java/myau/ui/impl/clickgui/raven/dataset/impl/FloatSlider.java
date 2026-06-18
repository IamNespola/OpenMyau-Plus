package myau.ui.impl.clickgui.raven.dataset.impl;

import myau.enums.ChatColors;
import myau.property.properties.FloatProperty;
import myau.ui.impl.clickgui.raven.dataset.Slider;

public class FloatSlider extends Slider {
    private final FloatProperty property;

    public FloatSlider(FloatProperty property) {
        this.property = property;
    }

    @Override
    public double getInput() {
        return property.getValue();
    }

    @Override
    public double getMin() {
        return property.getMinimum();
    }

    @Override
    public double getMax() {
        return property.getMaximum();
    }

    @Override
    public void setValue(double value) {
        property.setValue(new Double(value).floatValue());
    }

    @Override
    public String getName() {
        return property.getName().replace("-", " ");
    }

    @Override
    public String getValueString() {
        return ChatColors.formatColor(property.formatValue());
    }

    @Override
    public double getIncrement() {
        return 0.1;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }
}
