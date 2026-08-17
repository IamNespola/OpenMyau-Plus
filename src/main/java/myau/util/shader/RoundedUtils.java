package myau.util.shader;

import myau.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import myau.util.RenderUtil;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class RoundedUtils {
    private static ShaderUtils roundedShader;
    private static ShaderUtils roundedOutlineShader;
    private static ShaderUtils roundedTexturedShader;
    private static ShaderUtils roundedGradientShader;
    private static ShaderUtils roundedRectRiseShader;

    private static void initShaders() {
        if (roundedShader == null) {
            try {
                roundedShader = new ShaderUtils("roundedRect");
            } catch (Exception e) {
                System.err.println("Failed to initialize roundedShader: " + e.getMessage());
            }
        }
        if (roundedOutlineShader == null) {
            try {
                roundedOutlineShader = new ShaderUtils("roundRectOutline");
            } catch (Exception e) {
                System.err.println("Failed to initialize roundedOutlineShader: " + e.getMessage());
            }
        }
        if (roundedTexturedShader == null) {
            try {
                roundedTexturedShader = new ShaderUtils("roundRectTexture");
            } catch (Exception e) {
                System.err.println("Failed to initialize roundedTexturedShader: " + e.getMessage());
            }
        }
        if (roundedGradientShader == null) {
            try {
                roundedGradientShader = new ShaderUtils("roundedRectGradient");
            } catch (Exception e) {
                System.err.println("Failed to initialize roundedGradientShader: " + e.getMessage());
            }
        }
        if (roundedRectRiseShader == null) {
            try {
                roundedRectRiseShader = new ShaderUtils("roundedRectRise");
            } catch (Exception e) {
                System.err.println("Failed to initialize roundedRectRiseShader: " + e.getMessage());
            }
        }
    }


    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        drawRound(x, y, width, height, radius, false, color);
    }

    public static void drawGradientHorizontal(float x, float y, float width, float height, float radius, Color left, Color right) {
        drawGradientRound(x, y, width, height, radius, left, left, right, right);
    }

    public static void drawGradientVertical(float x, float y, float width, float height, float radius, Color top, Color bottom) {
        drawGradientRound(x, y, width, height, radius, bottom, top, bottom, top);
    }

    public static void drawGradientCornerLR(float x, float y, float width, float height, float radius, Color topLeft, Color bottomRight) {
        // Interpolate between two colors
        int r = (int) ((topLeft.getRed() + bottomRight.getRed()) / 2);
        int g = (int) ((topLeft.getGreen() + bottomRight.getGreen()) / 2);
        int b = (int) ((topLeft.getBlue() + bottomRight.getBlue()) / 2);
        Color mixedColor = new Color(r, g, b);
        drawGradientRound(x, y, width, height, radius, mixedColor, topLeft, bottomRight, mixedColor);
    }

    public static void drawGradientCornerRL(float x, float y, float width, float height, float radius, Color bottomLeft, Color topRight) {
        // Interpolate between two colors
        int r = (int) ((topRight.getRed() + bottomLeft.getRed()) / 2);
        int g = (int) ((topRight.getGreen() + bottomLeft.getGreen()) / 2);
        int b = (int) ((topRight.getBlue() + bottomLeft.getBlue()) / 2);
        Color mixedColor = new Color(r, g, b);
        drawGradientRound(x, y, width, height, radius, bottomLeft, mixedColor, mixedColor, topRight);
    }

    public static void drawRound(float x, float y, float width, float height, float radius, int color) {
        drawRound(x, y, width, height, radius, false, color);
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, int color) {
        initShaders();
        if (roundedShader == null) return;

        RenderUtil.resetColor();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.setAlphaLimit(0);

        roundedShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", getRed(color), getGreen(color), getBlue(color), getAlpha(color));

        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius, Color bottomLeft, Color topLeft, Color bottomRight, Color topRight) {
        initShaders();
        if (roundedGradientShader == null) return;

        RenderUtil.setAlphaLimit(0);
        RenderUtil.resetColor();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);
        roundedGradientShader.setUniformf("color1", topLeft.getRed() / 255f, topLeft.getGreen() / 255f, topLeft.getBlue() / 255f, topLeft.getAlpha() / 255f);
        roundedGradientShader.setUniformf("color2", bottomLeft.getRed() / 255f, bottomLeft.getGreen() / 255f, bottomLeft.getBlue() / 255f, bottomLeft.getAlpha() / 255f);
        roundedGradientShader.setUniformf("color3", topRight.getRed() / 255f, topRight.getGreen() / 255f, topRight.getBlue() / 255f, topRight.getAlpha() / 255f);
        roundedGradientShader.setUniformf("color4", bottomRight.getRed() / 255f, bottomRight.getGreen() / 255f, bottomRight.getBlue() / 255f, bottomRight.getAlpha() / 255f);
        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedGradientShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius, int bottomLeft, int topLeft, int bottomRight, int topRight) {
        initShaders();
        if (roundedGradientShader == null) return;

        RenderUtil.setAlphaLimit(0);
        RenderUtil.resetColor();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);

        roundedGradientShader.setUniformf("color1", getRed(topLeft), getGreen(topLeft), getBlue(topLeft), getAlpha(topLeft));
        roundedGradientShader.setUniformf("color2", getRed(bottomLeft), getGreen(bottomLeft), getBlue(bottomLeft), getAlpha(bottomLeft));
        roundedGradientShader.setUniformf("color3", getRed(topRight), getGreen(topRight), getBlue(topRight), getAlpha(topRight));
        roundedGradientShader.setUniformf("color4", getRed(bottomRight), getGreen(bottomRight), getBlue(bottomRight), getAlpha(bottomRight));

        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedGradientShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRound(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        initShaders();
        if (roundedShader == null) return;

        RenderUtil.resetColor();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.setAlphaLimit(0);
        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundTextured(float x, float y, float width, float height, float radius, float alpha) {
        initShaders();
        if (roundedTexturedShader == null) return;

        RenderUtil.resetColor();
        RenderUtil.setAlphaLimit(0);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        roundedTexturedShader.init();
        roundedTexturedShader.setUniformi("textureIn", 0);
        setupRoundedRectUniforms(x, y, width, height, radius, roundedTexturedShader);
        roundedTexturedShader.setUniformf("alpha", alpha);
        ShaderUtils.drawQuads(x - 1, y - 1, width + 2, height + 2);
        roundedTexturedShader.unload();
        GlStateManager.disableBlend();
    }

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtils roundedTexturedShader) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        roundedTexturedShader.setUniformf("location", x * sr.getScaleFactor(),
                (Minecraft.getMinecraft().displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        roundedTexturedShader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        roundedTexturedShader.setUniformf("radius", radius * sr.getScaleFactor());
    }

    public static void drawRoundedRectRise(final float x, final float y, final float width, final float height, final float radius, final int color, boolean leftTop, boolean rightTop, boolean rightBottom, boolean leftBottom) {
        initShaders();
        if (roundedRectRiseShader == null) return;

        GL11.glPushMatrix();
        GlStateManager.pushAttrib();
        final int programId = roundedRectRiseShader.programID;
        GL20.glUseProgram(programId);
        roundedRectRiseShader.setUniformf("u_size", width, height);
        roundedRectRiseShader.setUniformf("u_radius", radius);
        roundedRectRiseShader.setUniformf("u_color", getRed(color), getGreen(color), getBlue(color), getAlpha(color));
        roundedRectRiseShader.setUniformf("u_edges", leftBottom ? 1.0F : 0.0F, rightBottom ? 1.0F : 0.0F, rightTop ? 1.0F : 0.0F, leftTop ? 1.0F : 0.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        ShaderUtils.drawQuads(x, y, width, height);
        GlStateManager.disableBlend();
        GL20.glUseProgram(0);
        GlStateManager.popAttrib();
        GL11.glPopMatrix();
    }

    public static void drawRoundedRectRise(final double x, final double y, final double width, final double height, final double radius, final int color) {
        drawRoundedRectRise((float) x, (float) y, (float) width, (float) height, (float) radius, color, true, true, true, true);
    }

    private static float getRed(int color) {
        return (color >> 16 & 0xFF) / 255.0F;
    }

    public static void drawRoundOutline(
            float x,
            float y,
            float width,
            float height,
            float radius,
            float outlineThickness,
            Color color,
            Color outlineColor) {
        initShaders();
        RenderUtil.resetColor();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.setAlphaLimit(0);

        roundedOutlineShader.init();
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.getScaleFactor());
        roundedOutlineShader.setUniformf(
                "color",
                color.getRed() / 255f,
                color.getGreen() / 255f,
                color.getBlue() / 255f,
                color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf(
                "outlineColor",
                outlineColor.getRed() / 255f,
                outlineColor.getGreen() / 255f,
                outlineColor.getBlue() / 255f,
                outlineColor.getAlpha() / 255f);

        ShaderUtils.drawQuads(
                x - (2 + outlineThickness),
                y - (2 + outlineThickness),
                width + (4 + outlineThickness * 2),
                height + (4 + outlineThickness * 2));
        roundedOutlineShader.unload();
        GlStateManager.disableBlend();
    }

    private static float getGreen(int color) {
        return (color >> 8 & 0xFF) / 255.0F;
    }

    private static float getBlue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    private static float getAlpha(int color) {
        return (color >> 24 & 0xFF) / 255.0F;
    }
}