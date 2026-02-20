package myau.util;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class RoundedUtils {

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8  & 0xFF) / 255f;
        float b = (color       & 0xFF) / 255f;
        float a = (color >> 24 & 0xFF) / 255f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO
        );
        GL11.glColor4f(r, g, b, a);

        // Center fill (excluding corners)
        drawQuad(x + radius, y, x + width - radius, y + height);          // middle vertical strip
        drawQuad(x, y + radius, x + radius, y + height - radius);         // left strip
        drawQuad(x + width - radius, y + radius, x + width, y + height - radius); // right strip

        // Four rounded corners
        drawCorner(x + radius,         y + radius,          radius, 180, 270); // top-left
        drawCorner(x + width - radius, y + radius,          radius, 270, 360); // top-right
        drawCorner(x + radius,         y + height - radius, radius,  90, 180); // bottom-left
        drawCorner(x + width - radius, y + height - radius, radius,   0,  90); // bottom-right

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glColor4f(1f, 1f, 1f, 1f); // ✅ reset color
        GlStateManager.popMatrix();
    }

    private static void drawQuad(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x1, y2);
        GL11.glEnd();
    }

    private static void drawCorner(float cx, float cy, float radius, int startAngle, int endAngle) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = startAngle; i <= endAngle; i += 5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f(cx + (float)(Math.cos(angle) * radius),
                    cy + (float)(Math.sin(angle) * radius));
        }
        GL11.glEnd();
    }

    public static void drawOutlineRect(float v, float v1, float v2, float v3, float v4, int i, int rgb) {

    }
}