package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import keystrokesmod.module.setting.Setting;

import java.awt.Color;

public class ColorSetting extends Setting {
    private int red;
    private int green;
    private int blue;
    private int alpha;
    private final boolean hasAlpha;
    public GroupSetting groupSetting;

    public ColorSetting(String name, int red, int green, int blue) {
        this(null, name, red, green, blue, 255, false);
    }

    public ColorSetting(String name, int red, int green, int blue, int alpha) {
        this(null, name, red, green, blue, alpha, true);
    }

    public ColorSetting(GroupSetting groupSetting, String name, int red, int green, int blue) {
        this(groupSetting, name, red, green, blue, 255, false);
    }

    public ColorSetting(GroupSetting groupSetting, String name, int red, int green, int blue, int alpha) {
        this(groupSetting, name, red, green, blue, alpha, true);
    }

    public ColorSetting(GroupSetting groupSetting, String name, int red, int green, int blue, int alpha, boolean hasAlpha) {
        super(name);
        this.groupSetting = groupSetting;
        this.red = clamp(red);
        this.green = clamp(green);
        this.blue = clamp(blue);
        this.alpha = clamp(alpha);
        this.hasAlpha = hasAlpha;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = clamp(alpha);
    }

    public void setColor(int r, int g, int b) {
        this.red = clamp(r);
        this.green = clamp(g);
        this.blue = clamp(b);
    }

    public void setColor(int r, int g, int b, int a) {
        setColor(r, g, b);
        this.alpha = clamp(a);
    }

    public int getColor() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public int getRGB() {
        return (red << 16) | (green << 8) | blue;
    }

    public float getHue() {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        return hsb[0] * 360f;
    }

    public float getSaturation() {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        return hsb[1];
    }

    public float getBrightness() {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        return hsb[2];
    }

    public void setFromHSB(float hueDegrees, float saturation, float brightness) {
        int rgb = Color.HSBtoRGB(hueDegrees / 360f,
                Math.max(0f, Math.min(1f, saturation)),
                Math.max(0f, Math.min(1f, brightness)));
        this.red = (rgb >> 16) & 0xFF;
        this.green = (rgb >> 8) & 0xFF;
        this.blue = rgb & 0xFF;
    }

    public void setHue(float hueDegrees) {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        setFromHSB(hueDegrees, hsb[1], hsb[2]);
    }

    public void setSaturation(float saturation) {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        setFromHSB(hsb[0] * 360f, saturation, hsb[2]);
    }

    public void setBrightness(float brightness) {
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        setFromHSB(hsb[0] * 360f, hsb[1], brightness);
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }

    @Override
    public void loadProfile(JsonObject data) {
        String key = getName();
        if (data.has(key) && data.get(key).isJsonPrimitive()) {
            try {
                String value = data.getAsJsonPrimitive(key).getAsString();
                String[] parts = value.split(",");
                if (parts.length >= 3) {
                    this.red = clamp(Integer.parseInt(parts[0].trim()));
                    this.green = clamp(Integer.parseInt(parts[1].trim()));
                    this.blue = clamp(Integer.parseInt(parts[2].trim()));
                    if (parts.length >= 4) {
                        this.alpha = clamp(Integer.parseInt(parts[3].trim()));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
