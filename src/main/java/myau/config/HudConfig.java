package myau.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.module.modules.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("./config/Myau/", "hud.json");

    public static void load() {
        if (!FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
            JsonElement element = new JsonParser().parse(reader);
            if (element == null || !element.isJsonObject()) return;

            JsonObject object = element.getAsJsonObject();
            readTopLeft(object, "ArrayList", HUD::setArrayListPosition);
            readTopLeft(object, "TargetHUD", HUD::setTargetHUDPosition);
            readTopLeft(object, "MurdererDetector", MurderDetector::setTextPosition);
            readTopLeft(object, "Radar", Radar::setPosition);
            readTopLeft(object, "WaterMark", WaterMark::setPosition);
            readTopLeft(object, "WaterMark2", WaterMark2::setPosition);
            readTopLeft(object, "DynamicIsland", DynamicIsland::setPosition);
            readTopLeft(object, "SeasonDisplay", SeasonDisplay::setPosition);
            readTopLeft(object, "FPSCounter", FPScounter::setPosition);
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            if (!FILE.getParentFile().exists()) {
                FILE.getParentFile().mkdirs();
            }

            JsonObject object = new JsonObject();
            write(object, "ArrayList", HUD.arrayListX, HUD.arrayListY);
            write(object, "TargetHUD", HUD.targetHUDX, HUD.targetHUDY);
            write(object, "MurdererDetector", MurderDetector.textX, MurderDetector.textY);
            write(object, "Radar", Radar.x, Radar.y);
            write(object, "WaterMark", WaterMark.x, WaterMark.y);
            write(object, "WaterMark2", WaterMark2.x, WaterMark2.y);
            write(object, "DynamicIsland", DynamicIsland.x, DynamicIsland.y);
            write(object, "SeasonDisplay", SeasonDisplay.x, SeasonDisplay.y);
            write(object, "FPSCounter", FPScounter.x, FPScounter.y);

            try (PrintWriter writer = new PrintWriter(new FileWriter(FILE))) {
                writer.println(GSON.toJson(object));
            }
        } catch (IOException ignored) {
        }
    }

    private static void readTopLeft(JsonObject root, String name, PositionSetter setter) {
        JsonElement element = root.get(name);
        if (element == null || !element.isJsonObject()) return;

        JsonObject object = element.getAsJsonObject();
        if (object.has("x") && object.has("y")) {
            setter.set(object.get("x").getAsInt(), object.get("y").getAsInt());
        }
    }

    private static void write(JsonObject root, String name, int x, int y) {
        JsonObject object = new JsonObject();
        object.addProperty("x", x);
        object.addProperty("y", y);
        root.add(name, object);
    }

    private interface PositionSetter {
        void set(int x, int y);
    }
}
