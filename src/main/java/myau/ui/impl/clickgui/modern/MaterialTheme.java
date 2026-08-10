package myau.ui.impl.clickgui.modern;

import java.awt.*;

public class MaterialTheme {
    public static final Color PRIMARY_COLOR = new Color(110, 80, 255);
    public static final Color SURFACE_CONTAINER_HIGH = new Color(45, 45, 50, 220);
    public static final Color TEXT_COLOR = new Color(240, 240, 240);
    public static final Color TEXT_COLOR_SECONDARY = new Color(160, 160, 170);
    public static final Color OUTLINE_COLOR = new Color(80, 80, 90, 100);
    public static final float CORNER_RADIUS_FRAME = 8.0f;

    public static int getRGB(Color color) {
        return color.getRGB();
    }

    public static int getRGBWithAlpha(Color color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB();
    }
}
