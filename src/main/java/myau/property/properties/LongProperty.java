package myau.property.properties;

import com.google.gson.JsonObject;
import myau.property.Property;

import java.util.function.BooleanSupplier;

public class LongProperty extends Property<Long> {
    private final Long minimum;
    private final Long maximum;

    public LongProperty(String name, Long value, Long minimum, Long maximum) {
        this(name, value, minimum, maximum, null);
    }

    public LongProperty(
            String name, Long value, Long minimum, Long maximum, BooleanSupplier check
    ) {
        super(name, value, v -> v >= minimum && v <= maximum, check);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public String getValuePrompt() {
        return String.format("%d-%d", this.minimum, this.maximum);
    }

    @Override
    public String formatValue() {
        return String.format("&e%s", this.getValue());
    }

    @Override
    public boolean parseString(String string) {
        try {
            return this.setValue(Long.parseLong(string));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return this.setValue(jsonObject.get(this.getName()).getAsNumber().longValue());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getValue());
    }

    public Long getMinimum() {
        return minimum;
    }

    public Long getMaximum() {
        return maximum;
    }
}
