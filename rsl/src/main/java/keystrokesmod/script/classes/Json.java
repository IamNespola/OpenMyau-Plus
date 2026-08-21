package keystrokesmod.script.classes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import keystrokesmod.utility.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Json {
    private JsonElement json;

    public Json(String jsonString) {
        this.json = (new JsonParser()).parse(jsonString);
    }

    protected Json(JsonElement json, byte s) {
        this.json = json;
    }

    public boolean equals(final Json json) {
        return json != null && (this.json == json.json || (this.exists() && json.exists() && this.string().equals(json.string())));
    }

    public boolean exists() {
        return json != null;
    }

    public String string() {
        if (this.exists()) {
            if (!this.json.isJsonArray()) {
                try {
                    return this.json.getAsString();
                }
                catch (UnsupportedOperationException ex) {}
            }
            return this.json.toString();
        }
        return "";
    }

    public String get(final String member) {
        return Utils.getString((JsonObject)this.json, member);
    }

    public String get(final String member, final String defaultValue) {
        final String value = this.get(member);
        return value.isEmpty() ? defaultValue : value;
    }

    public Json object() {
        return this.object(null);
    }

    public Json object(final String member) {
        return new Json((member == null) ? this.json.getAsJsonObject() : ((JsonObject)this.json).getAsJsonObject(member), (byte) 0);
    }

    public List<Json> array() {
        return this.array(null);
    }

    public List<Json> array(final String member) {
        final List<Json> jsonList = new ArrayList<>();
        for (final JsonElement element : (member == null) ? this.json.getAsJsonArray() : ((JsonObject)this.json).getAsJsonArray(member)) {
            jsonList.add(new Json(element, (byte) 0));
        }
        return jsonList;
    }

    public Map<String, Json> map() {
        final HashMap<String, Json> map = new HashMap<>();
        for (final Map.Entry<String, JsonElement> entry : ((JsonObject)this.json).entrySet()) {
            map.put(entry.getKey(), new Json(entry.getValue(), (byte) 0));
        }
        return map;
    }

    @Override
    public String toString() {
        return this.json.toString();
    }
}
