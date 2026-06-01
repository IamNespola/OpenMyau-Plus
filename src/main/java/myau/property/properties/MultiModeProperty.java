package myau.property.properties;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import myau.property.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class MultiModeProperty extends Property<boolean[]> {
    private final String[] modes;

    public MultiModeProperty(String name, String[] modes) {
        this(name, modes, null);
    }

    public MultiModeProperty(String name, String[] modes, BooleanSupplier check) {
        super(name, new boolean[modes.length], check);
        this.modes = modes;
    }

    @Override
    public String getValuePrompt() {
        return String.join(", ", this.modes);
    }

    public String[] getModes() {
        return this.modes;
    }

    public boolean isSelected(String mode) {
        for (int i = 0; i < this.modes.length; i++) {
            if (mode.replace("_", "").equalsIgnoreCase(this.modes[i].replace("_", ""))) {
                return isSelected(i);
            }
        }
        return false;
    }

    public boolean isSelected(int index) {
        return index >= 0 && index < this.getValue().length && this.getValue()[index];
    }

    public void toggle(int index) {
        if (index < 0 || index >= this.getValue().length) return;
        boolean[] values = this.getValue();
        values[index] = !values[index];
        this.setValue(values);
    }

    public String getSelectedModesString() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < this.modes.length; i++) {
            if (isSelected(i)) selected.add(this.modes[i]);
        }
        return selected.isEmpty() ? "NONE" : String.join(", ", selected);
    }

    @Override
    public String formatValue() {
        return String.format("&9%s", getSelectedModesString());
    }

    @Override
    public boolean parseString(String string) {
        boolean[] values = new boolean[this.modes.length];
        if (string != null && !string.equalsIgnoreCase("NONE")) {
            for (String token : string.split(",")) {
                String valueStr = token.trim().replace("_", "");
                for (int i = 0; i < this.modes.length; i++) {
                    if (valueStr.equalsIgnoreCase(this.modes[i].replace("_", ""))) values[i] = true;
                }
            }
        }
        return this.setValue(values);
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        JsonElement element = jsonObject.get(this.getName());
        if (element == null) return false;
        if (!element.isJsonArray()) return parseString(element.getAsString());

        boolean[] values = new boolean[this.modes.length];
        for (JsonElement item : element.getAsJsonArray()) {
            String valueStr = item.getAsString().replace("_", "");
            for (int i = 0; i < this.modes.length; i++) {
                if (valueStr.equalsIgnoreCase(this.modes[i].replace("_", ""))) values[i] = true;
            }
        }
        return this.setValue(values);
    }

    @Override
    public void write(JsonObject jsonObject) {
        JsonArray array = new JsonArray();
        for (int i = 0; i < this.modes.length; i++) {
            if (isSelected(i)) array.add(new JsonPrimitive(this.modes[i]));
        }
        jsonObject.add(this.getName(), array);
    }
}
