package myau.util.shader;

import java.awt.Color;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class Shader2D {
    private static int roundedProgram, roundedOutlineProgram, roundedGlowProgram, roundedGradientProgram;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public Shader2D() {
        roundedProgram = createShader(vertex, roundedRect);
        roundedOutlineProgram = createShader(vertex, roundedOutlineRect);
        roundedGlowProgram = createShader(vertex, roundedGlowRect);
        roundedGradientProgram = createShader(vertex, roundedGradient);
    }

    public static final String vertex =
            "#version 120\n" +
                    "void main() {\n" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}";

    public static final String roundedRect =
            "#version 120\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform float radius;\n" +
                    "uniform vec4 color;\n" +
                    "float roundedRectSDF(vec2 p, vec2 b, float r) {\n" +
                    "    return length(max(abs(p) - b + r, 0.0)) - r;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 st = gl_FragCoord.xy - (location + rectSize * 0.5);\n" +
                    "    float alpha = 1.0 - smoothstep(-1.0, 1.0, roundedRectSDF(vec2(st.x, -st.y), rectSize * 0.5, radius));\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
                    "}";

    public static final String roundedOutlineRect =
            "#version 120\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform float radius, thickness;\n" +
                    "uniform vec4 color;\n" +
                    "float roundedRectSDF(vec2 p, vec2 b, float r) {\n" +
                    "    return length(max(abs(p) - b + r, 0.0)) - r;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 st = gl_FragCoord.xy - (location + rectSize * 0.5);\n" +
                    "    float dist = roundedRectSDF(vec2(st.x, -st.y), rectSize * 0.5, radius);\n" +
                    "    float alpha = smoothstep(-1.0, 0.0, dist + thickness) * (1.0 - smoothstep(-1.0, 1.0, dist));\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
                    "}";

    public static final String roundedGlowRect =
            "#version 120\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform float radius, glowRadius, glowIntensity;\n" +
                    "uniform vec4 glowColor;\n" +
                    "float roundedRectSDF(vec2 p, vec2 b, float r) {\n" +
                    "    return length(max(abs(p) - b + r, 0.0)) - r;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 st = gl_FragCoord.xy - (location + rectSize * 0.5);\n" +
                    "    float dist = roundedRectSDF(vec2(st.x, -st.y), rectSize * 0.5, radius);\n" +
                    "    if (dist < 0.0) discard;\n" +
                    "    float alpha = (1.0 - smoothstep(0.0, glowRadius, dist)) * glowIntensity;\n" +
                    "    gl_FragColor = vec4(glowColor.rgb, glowColor.a * alpha);\n" +
                    "}";

    public static final String roundedGradient =
            "#version 120\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform float radius, vertical;\n" +
                    "uniform vec4 color1, color2, color3;\n" +
                    "float roundedRectSDF(vec2 p, vec2 b, float r) {\n" +
                    "    return length(max(abs(p) - b + r, 0.0)) - r;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 st = gl_FragCoord.xy - (location + rectSize * 0.5);\n" +
                    "    float alpha = 1.0 - smoothstep(-1.0, 1.0, roundedRectSDF(vec2(st.x, -st.y), rectSize * 0.5, radius));\n" +
                    "    if (alpha <= 0.0) discard;\n" +
                    "    vec2 uv = (gl_FragCoord.xy - location) / rectSize;\n" +
                    "    float t = vertical > 0.5 ? uv.y : uv.x;\n" +
                    "    vec3 color = t < 0.5 ? mix(color1.rgb, color2.rgb, t * 2.0) : mix(color2.rgb, color3.rgb, (t - 0.5) * 2.0);\n" +
                    "    gl_FragColor = vec4(color, color1.a * alpha);\n" +
                    "}";

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        if (roundedProgram == 0) roundedProgram = createShader(vertex, roundedRect);
        setupRenderState(roundedProgram);
        setupUniforms(roundedProgram, x, y, width, height, Math.max(1.5f, radius));
        setUniformColor(roundedProgram, "color", color);
        drawQuad(x, y, width, height);
        finishRenderState();
    }

    public static void drawGlow(float x, float y, float width, float height, float radius, float glowRadius, float glowIntensity, Color color) {
        if (roundedGlowProgram == 0) roundedGlowProgram = createShader(vertex, roundedGlowRect);
        setupRenderState(roundedGlowProgram);
        setupUniforms(roundedGlowProgram, x, y, width, height, Math.max(1.5f, radius));
        GL20.glUniform1f(GL20.glGetUniformLocation(roundedGlowProgram, "glowRadius"), glowRadius * getScale());
        GL20.glUniform1f(GL20.glGetUniformLocation(roundedGlowProgram, "glowIntensity"), glowIntensity);
        setUniformColor(roundedGlowProgram, "glowColor", color);
        drawQuad(x - glowRadius, y - glowRadius, width + glowRadius * 2, height + glowRadius * 2);
        finishRenderState();
    }

    public static void drawGradient(float x, float y, float width, float height, float radius, Color c1, Color c2, Color c3, boolean vertical) {
        if (roundedGradientProgram == 0) roundedGradientProgram = createShader(vertex, roundedGradient);
        setupRenderState(roundedGradientProgram);
        setupUniforms(roundedGradientProgram, x, y, width, height, Math.max(1.5f, radius));
        GL20.glUniform1f(GL20.glGetUniformLocation(roundedGradientProgram, "vertical"), vertical ? 1.0f : 0.0f);
        setUniformColor(roundedGradientProgram, "color1", c1);
        setUniformColor(roundedGradientProgram, "color2", c2);
        setUniformColor(roundedGradientProgram, "color3", c3);
        drawQuad(x, y, width, height);
        finishRenderState();
    }

    public static void drawOutline(float x, float y, float width, float height, float radius, float thickness, Color color) {
        if (roundedOutlineProgram == 0) roundedOutlineProgram = createShader(vertex, roundedOutlineRect);
        setupRenderState(roundedOutlineProgram);
        setupUniforms(roundedOutlineProgram, x, y, width, height, Math.max(1.5f, radius));
        GL20.glUniform1f(GL20.glGetUniformLocation(roundedOutlineProgram, "thickness"), thickness * getScale());
        setUniformColor(roundedOutlineProgram, "color", color);
        drawQuad(x - thickness, y - thickness, width + thickness * 2, height + thickness * 2);
        finishRenderState();
    }

    private static void setupUniforms(int program, float x, float y, float width, float height, float radius) {
        int f = getScale();
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "location"), x * f, (mc.displayHeight - (y + height) * f));
        GL20.glUniform2f(GL20.glGetUniformLocation(program, "rectSize"), width * f, height * f);
        GL20.glUniform1f(GL20.glGetUniformLocation(program, "radius"), radius * f);
    }

    public static void setupRenderState(int program) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GL20.glUseProgram(program);
    }

    public static void finishRenderState() {
        GL20.glUseProgram(0);
        GlStateManager.enableAlpha();
    }

    public static void setUniformColor(int program, String name, Color c) {
        GL20.glUniform4f(GL20.glGetUniformLocation(program, name), c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, c.getAlpha()/255f);
    }

    public static int getScale() {
        return new ScaledResolution(mc).getScaleFactor();
    }

    public static void drawQuad(float x, float y, float width, float height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + height);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
    }

    public static int createShader(String vertexSource, String fragmentSource) {
        int program = GL20.glCreateProgram();
        int vShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vShader, vertexSource);
        GL20.glCompileShader(vShader);

        if (GL20.glGetShaderi(vShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Vertex Shader Error: " + GL20.glGetShaderInfoLog(vShader, 1024));
        }

        int fShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fShader, fragmentSource);
        GL20.glCompileShader(fShader);

        if (GL20.glGetShaderi(fShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Fragment Shader Error: " + GL20.glGetShaderInfoLog(fShader, 1024));
        }

        GL20.glAttachShader(program, vShader);
        GL20.glAttachShader(program, fShader);
        GL20.glLinkProgram(program);
        return program;
    }
}