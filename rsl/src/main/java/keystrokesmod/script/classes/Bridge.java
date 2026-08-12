package keystrokesmod.script.classes;

import java.util.HashMap;
import java.util.Map;

public class Bridge {
    private static final Map<String, Object> map = new HashMap<>();

    public void add(String key, Object value) {
        map.put(key, value);
    }

    public void add(String key) {
        map.put(key, null);
    }

    public void remove(String key) {
        map.remove(key);
    }

    public boolean has(String key) {
        return map.containsKey(key);
    }

    public Object get(String key) {
        if (!map.containsKey(key)) {
            return null;
        }
        return map.getOrDefault(key, null);
    }

    public void clear() {
        map.clear();
    }
}
