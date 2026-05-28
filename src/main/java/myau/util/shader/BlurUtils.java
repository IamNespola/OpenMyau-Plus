package myau.util.shader;

import net.minecraft.client.shader.Framebuffer;
import myau.util.RenderUtil;

public class BlurUtils {
    private static Framebuffer stencilFrameBufferBlur = new Framebuffer(1, 1, false);
    private static Framebuffer stencilFrameBufferBloom = new Framebuffer(1, 1, false);

    public static void prepareBlur() {
        stencilFrameBufferBlur = RenderUtil.createFrameBuffer(stencilFrameBufferBlur);
        stencilFrameBufferBlur.framebufferClear();
        stencilFrameBufferBlur.bindFramebuffer(false);
    }

    public static void prepareBloom() {
        stencilFrameBufferBloom = RenderUtil.createFrameBuffer(stencilFrameBufferBloom);
        stencilFrameBufferBloom.framebufferClear();
        stencilFrameBufferBloom.bindFramebuffer(false);
    }

    public static void blurEnd(int passes, float radius) {
        stencilFrameBufferBlur.unbindFramebuffer();
        KawaseBlur.renderBlur(stencilFrameBufferBlur.framebufferTexture, passes, radius);
    }

    public static void bloomEnd(int passes, float radius) {
        stencilFrameBufferBloom.unbindFramebuffer();
        KawaseBloom.renderBlur(stencilFrameBufferBloom.framebufferTexture, passes, radius);
    }

    public static void prepareRiseBloom() {
        RiseBloomShader.prepareBloom();
    }

    public static void riseBloomEnd(int radius, float compression) {
        RiseBloomShader.bloomEnd(radius, compression);
    }
}
