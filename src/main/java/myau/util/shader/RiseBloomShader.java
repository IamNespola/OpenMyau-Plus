package myau.util.shader;

import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public final class RiseBloomShader {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int MAX_RADIUS = 23;
    private static final String VERTEX_SHADER = "#version 120\n"
            + "\n"
            + "void main() {\n"
            + "    gl_TexCoord[0] = gl_MultiTexCoord0;\n"
            + "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n"
            + "}\n";
    private static final String BLOOM_SHADER = "#version 120\n"
            + "\n"
            + "uniform sampler2D u_diffuse_sampler, u_other_sampler;\n"
            + "uniform vec2 u_texel_size, u_direction;\n"
            + "uniform float u_radius, u_kernel[24];\n"
            + "\n"
            + "void main(void) {\n"
            + "    vec2 uv = gl_TexCoord[0].st;\n"
            + "\n"
            + "    if (u_direction.x == 0.0 && texture2D(u_other_sampler, uv).a > 0.0) {\n"
            + "        discard;\n"
            + "    }\n"
            + "\n"
            + "    vec4 source = texture2D(u_diffuse_sampler, uv);\n"
            + "    vec4 pixel_color = source * u_kernel[0];\n"
            + "    pixel_color.rgb *= pixel_color.a;\n"
            + "\n"
            + "    for (int f = 1; f <= int(u_radius); f++) {\n"
            + "        vec2 offset = (u_texel_size * u_direction) * f;\n"
            + "        vec4 left = texture2D(u_diffuse_sampler, uv - offset);\n"
            + "        vec4 right = texture2D(u_diffuse_sampler, uv + offset);\n"
            + "        left.rgb *= left.a;\n"
            + "        right.rgb *= right.a;\n"
            + "        pixel_color += (left + right) * u_kernel[f];\n"
            + "    }\n"
            + "\n"
            + "    if (pixel_color.a > 0.0) {\n"
            + "        pixel_color.rgb /= pixel_color.a;\n"
            + "    }\n"
            + "\n"
            + "    gl_FragColor = pixel_color;\n"
            + "}\n";

    private static Framebuffer inputFramebuffer = new Framebuffer(1, 1, false);
    private static Framebuffer outputFramebuffer = new Framebuffer(1, 1, false);
    private static int programId;
    private static int lastRadius = -1;

    private RiseBloomShader() {
    }

    public static void prepareBloom() {
        inputFramebuffer = RenderUtil.createFrameBuffer(inputFramebuffer, false);
        inputFramebuffer.framebufferClear();
        inputFramebuffer.bindFramebuffer(false);
    }

    public static boolean bloomEnd(int radius, float compression) {
        inputFramebuffer.unbindFramebuffer();
        return renderBloom(inputFramebuffer.framebufferTexture, radius, compression);
    }

    public static boolean renderBloom(int framebufferTexture, int radius, float compression) {
        if (!Display.isVisible() || mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            return false;
        }

        if (!ensureProgram()) {
            return false;
        }

        radius = Math.max(1, Math.min(MAX_RADIUS, radius));
        compression = Math.max(0.25F, compression);
        outputFramebuffer = RenderUtil.createFrameBuffer(outputFramebuffer, false);
        outputFramebuffer.framebufferClear();

        GL20.glUseProgram(programId);
        updateKernel(radius);
        GL20.glUniform1i(getUniform("u_diffuse_sampler"), 0);
        GL20.glUniform1i(getUniform("u_other_sampler"), 16);
        GL20.glUniform2f(getUniform("u_texel_size"), 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);

        outputFramebuffer.bindFramebuffer(true);
        GL20.glUniform2f(getUniform("u_direction"), compression, 0.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
        RenderUtil.bindTexture(framebufferTexture);
        drawQuad();

        mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL20.glUniform2f(getUniform("u_direction"), 0.0F, compression);
        outputFramebuffer.bindFramebufferTexture();
        GL13.glActiveTexture(GL13.GL_TEXTURE16);
        RenderUtil.bindTexture(framebufferTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        drawQuad();

        GlStateManager.disableBlend();
        GL20.glUseProgram(0);
        GlStateManager.bindTexture(0);
        return true;
    }

    private static boolean ensureProgram() {
        if (programId != 0) {
            return true;
        }

        int fragment = compileShader(BLOOM_SHADER, GL20.GL_FRAGMENT_SHADER);
        int vertex = compileShader(VERTEX_SHADER, GL20.GL_VERTEX_SHADER);
        if (fragment == 0 || vertex == 0) {
            return false;
        }

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, fragment);
        GL20.glAttachShader(programId, vertex);
        GL20.glLinkProgram(programId);
        GL20.glDeleteShader(fragment);
        GL20.glDeleteShader(vertex);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            System.err.println("Rise bloom shader failed to link: " + GL20.glGetProgramInfoLog(programId, 4096));
            GL20.glDeleteProgram(programId);
            programId = 0;
            return false;
        }
        return true;
    }

    private static int compileShader(String source, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            System.err.println("Rise bloom shader failed to compile: " + GL20.glGetShaderInfoLog(shader, 4096));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static void updateKernel(int radius) {
        if (lastRadius == radius) {
            return;
        }

        float[] kernel = RiseGaussianKernel.compute(radius);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(kernel.length);
        buffer.put(kernel);
        buffer.flip();
        GL20.glUniform1f(getUniform("u_radius"), radius);
        GL20.glUniform1(getUniform("u_kernel"), buffer);
        lastRadius = radius;
    }

    private static int getUniform(String name) {
        return GL20.glGetUniformLocation(programId, name);
    }

    private static void drawQuad() {
        ScaledResolution sr = new ScaledResolution(mc);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(0.0F, 0.0F);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(0.0F, (float) sr.getScaledHeight_double());
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f((float) sr.getScaledWidth_double(), (float) sr.getScaledHeight_double());
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f((float) sr.getScaledWidth_double(), 0.0F);
        GL11.glEnd();
    }
}
