package myau.util;

import net.minecraft.client.gui.Gui;
import org.lwjgl.util.Color;

public class RenderUtils {
    public static void drawRect(float left, float top, float width, float height, Color color) {
        drawRect(left, top, width, height, ((color.getRed() & 0xFF) << 16) | ((color.getGreen() & 0xFF) << 8) | (color.getBlue() & 0xFF) | ((color.getAlpha() & 0xFF) << 24));
    }

    public static void drawRect(float left, float top, float width, float height, int color) {
        float right = left + width, bottom = top + height;
        if (left < right) {
            float i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            float j = top;
            top = bottom;
            bottom = j;
        }

        Gui.drawRect((int) left, (int) top, (int) right, (int) bottom, color);
    }

}