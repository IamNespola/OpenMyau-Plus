package myau.ui.impl.clickgui.clean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public final class CleanGuiState {
    private static final File FILE = new File("./config/Myau/clickgui.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, FrameState> STATES = new HashMap<>();
    private static boolean loaded;

    private CleanGuiState() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        STATES.clear();
        if (!FILE.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (parsed == null || !parsed.isJsonObject()) return;
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("frames") || !root.get("frames").isJsonObject()) return;
            JsonObject frames = root.getAsJsonObject("frames");
            for (Map.Entry<String, JsonElement> entry : frames.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject object = entry.getValue().getAsJsonObject();
                FrameState state = new FrameState();
                state.x = object.has("x") ? object.get("x").getAsInt() : 12;
                state.y = object.has("y") ? object.get("y").getAsInt() : 26;
                state.expanded = !object.has("expanded") || object.get("expanded").getAsBoolean();
                STATES.put(entry.getKey(), state);
            }
        } catch (Exception ignored) {
            STATES.clear();
        }
    }

    public static synchronized FrameState get(String name) {
        load();
        FrameState state = STATES.get(name);
        return state == null ? null : state.copy();
    }

    public static synchronized void put(String name, int x, int y, boolean expanded) {
        load();
        FrameState state = new FrameState();
        state.x = x;
        state.y = y;
        state.expanded = expanded;
        STATES.put(name, state);
    }

    public static synchronized void save() {
        load();
        try {
            File parent = FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            JsonObject root = new JsonObject();
            JsonObject frames = new JsonObject();
            for (Map.Entry<String, FrameState> entry : STATES.entrySet()) {
                FrameState state = entry.getValue();
                JsonObject object = new JsonObject();
                object.addProperty("x", state.x);
                object.addProperty("y", state.y);
                object.addProperty("expanded", state.expanded);
                frames.add(entry.getKey(), object);
            }
            root.add("frames", frames);
            try (PrintWriter writer = new PrintWriter(new FileWriter(FILE))) {
                writer.println(GSON.toJson(root));
            }
        } catch (IOException ignored) {
        }
    }

    public static final class FrameState {
        public int x;
        public int y;
        public boolean expanded = true;

        private FrameState copy() {
            FrameState copy = new FrameState();
            copy.x = this.x;
            copy.y = this.y;
            copy.expanded = this.expanded;
            return copy;
        }
    }
}
