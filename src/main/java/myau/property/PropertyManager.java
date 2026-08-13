package myau.property;

import myau.module.Module;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PropertyManager {
    public LinkedHashMap<Class<?>, ArrayList<Property<?>>> properties = new LinkedHashMap<>();

    public Property<?> getProperty(Module module, String string) {
        for (Property<?> property : properties.get(module.getClass())) {
            if (property.getName().replace("-", "").equalsIgnoreCase(string.replace("-", ""))) {
                return property;
            }
        }
        return null;
    }
}