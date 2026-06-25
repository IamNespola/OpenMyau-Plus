package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.TextProperty;
import myau.util.RenderUtil;
import myau.util.RenderUtils;
import myau.util.font.FontManager;
import myau.util.font.impl.FontRenderer;
import myau.font.CFontRenderer;
import myau.font.CFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static net.minecraft.init.Items.string;

public class WaterMark extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0,
            new String[] { "Exhibition", "Modern", "WeedHack", "Vape" });

    public final TextProperty modernText = new TextProperty("Text", "OpenMyau+", () -> mode.getValue() == 1);
    public final BooleanProperty shadow = new BooleanProperty("Shadow", true, () -> mode.getValue() == 1);
    public final BooleanProperty enableGlow = new BooleanProperty("Glow", true);

    // "VAPE V4" image watermark. The two PNGs live in
    // assets/myau/assets/ and are drawn side-by-side to read as "VAPE V4".
    private static final ResourceLocation VAPE_IMAGE = new ResourceLocation("myau", "assets/textvape.png");
    private static final ResourceLocation V4_IMAGE = new ResourceLocation("myau", "assets/textv4.png");
    private DynamicTexture vapeTexture, v4Texture;
    private int vapeWidth, vapeHeight, v4Width, v4Height;
    private boolean vapeImagesLoaded = false;

    public WaterMark() {
        super("WaterMark", false, false);
    }

    private FontRenderer getCustomFont() {
        HUD hud = (HUD) Myau.moduleManager.getModule("HUD");
        if (hud != null) {
            switch (hud.fontMode.getValue()) {
                case 1:
                    if (FontManager.productSans20 != null)
                        return FontManager.productSans20;
                    break;
                case 2:
                    if (FontManager.regular22 != null)
                        return FontManager.regular22;
                    break;
                case 3:
                    if (FontManager.tenacity20 != null)
                        return FontManager.tenacity20;
                    break;
                case 4:
                    if (FontManager.vision20 != null)
                        return FontManager.vision20;
                    break;
                case 5:
                    if (FontManager.nbpInforma20 != null)
                        return FontManager.nbpInforma20;
                    break;
                case 6:
                    if (FontManager.tahomaBold20 != null)
                        return FontManager.tahomaBold20;
                    break;
            }
        }
        return null;
    }

    private float getStringWidth(String text) {
        FontRenderer fr = getCustomFont();
        if (fr != null) {
            return (float) fr.getStringWidth(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }

    private void drawStringWithShadow(String text, float x, float y, int color) {
        FontRenderer fr = getCustomFont();
        if (fr != null) {
            fr.drawStringWithShadow(text, x, y, color);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled())
            return;

        switch (mode.getValue()) {
            case 0:
                renderExhibition();
                break;
            case 1:
                renderModern();
                break;
            case 2:
                renderWeedhackWatermark(4, 4);
                break;
            case 3:
                renderVape(4.0f, 4.0f);
                break;
        }
    }

    private void loadVapeImages() {
        vapeImagesLoaded = true;
        try {
            java.io.InputStream vapeStream = WaterMark.class.getResourceAsStream("/assets/myau/assets/textvape.png");
            if (vapeStream != null) {
                BufferedImage image = ImageIO.read(vapeStream);
                vapeWidth = image.getWidth();
                vapeHeight = image.getHeight();
                vapeTexture = new DynamicTexture(image);
                vapeStream.close();
            } else {
                System.out.println(
                        "[Myau] Failed to find /assets/myau/assets/textvape.png via getResourceAsStream, trying ResourceLocation...");
                BufferedImage image = ImageIO.read(
                        mc.getResourceManager().getResource(VAPE_IMAGE).getInputStream());
                vapeWidth = image.getWidth();
                vapeHeight = image.getHeight();
                vapeTexture = new DynamicTexture(image);
            }
        } catch (Exception e) {
            System.err.println("[Myau] Failed to load vape watermark image:");
            e.printStackTrace();
            vapeTexture = null;
        }
        try {
            java.io.InputStream v4Stream = WaterMark.class.getResourceAsStream("/assets/myau/assets/textv4.png");
            if (v4Stream != null) {
                BufferedImage image = ImageIO.read(v4Stream);
                v4Width = image.getWidth();
                v4Height = image.getHeight();
                v4Texture = new DynamicTexture(image);
                v4Stream.close();
            } else {
                System.out.println(
                        "[Myau] Failed to find /assets/myau/assets/textv4.png via getResourceAsStream, trying ResourceLocation...");
                BufferedImage image = ImageIO.read(
                        mc.getResourceManager().getResource(V4_IMAGE).getInputStream());
                v4Width = image.getWidth();
                v4Height = image.getHeight();
                v4Texture = new DynamicTexture(image);
            }
        } catch (Exception e) {
            System.err.println("[Myau] oh fuck it doenst wokr :D:");
            e.printStackTrace();
            v4Texture = null;
        }
    }

    /**
     * Renders the "VAPE" and "V4" PNGs next to each other so they read as
     * "VAPE V4". Both images are scaled to a common height so they line up
     * regardless of their native resolution.
     */
    private void renderVape(float x, float y) {
        if (!vapeImagesLoaded) {
            loadVapeImages();
        }

        final float targetHeight = 18.0f;
        final float gap = 0.1f;

        float cursorX = x;
        cursorX = drawVapeImage(vapeTexture, vapeWidth, vapeHeight, cursorX, y, targetHeight);
        cursorX += gap;
        drawVapeImage(v4Texture, v4Width, v4Height, cursorX, y, targetHeight);
    }

    private float drawVapeImage(DynamicTexture texture, int width, int height, float x, float y, float targetHeight) {
        if (texture == null || height <= 0) {
            return x;
        }

        float scale = targetHeight / (float) height;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.translate(x, y, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.bindTexture(texture.getGlTextureId());
        Gui.drawModalRectWithCustomSizedTexture(
                0, 0, 0.0f, 0.0f, width, height, (float) width, (float) height);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        return x + (float) width * scale;
    }

    private void renderModern() {
        FontRenderer fr = FontManager.nunitoBold48;
        boolean customFont = fr != null;

        HUD hud = (HUD) Myau.moduleManager.getModule("HUD");

        String text = modernText.getValue();
        float x = 4.0f;
        float y = 4.0f;
        long time = System.currentTimeMillis();

        GlStateManager.pushMatrix();

        char[] characters = text.toCharArray();
        float currentX = x;

        for (int i = 0; i < characters.length; i++) {
            String charStr = String.valueOf(characters[i]);

            int color = 0xFFFFFFFF;
            if (hud != null) {
                long offset = (long) (i * hud.colorDistance.getValue());
                color = hud.getColor(time, offset).getRGB();
            }

            if (customFont) {
                if (shadow.getValue()) {
                    fr.drawStringWithShadow(charStr, currentX, y, color);
                } else {
                    fr.drawString(charStr, currentX, y, color);
                }
                currentX += (float) fr.getStringWidth(charStr);
            } else {
                mc.fontRendererObj.drawString(charStr, currentX, y, color, shadow.getValue());
                currentX += mc.fontRendererObj.getStringWidth(charStr);
            }
        }

        GlStateManager.popMatrix();
    }

    private void renderExhibition() {
        int fps = Minecraft.getDebugFPS();
        int ping = 0;

        if (mc.thePlayer != null && mc.theWorld != null) {
            if (mc.thePlayer.sendQueue != null
                    && mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()) != null) {
                ping = mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
            }
        }

        String exhibitionText = "O";
        String restText = "PenMyau+ ";
        String fpsValue = fps + "FPS";
        String pingValue = ping + "ms";

        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);

        float x = 2.0f;
        float y = 2.0f;

        if (getCustomFont() != null) {
            y += 1.0f;
        }

        GlStateManager.pushMatrix();

        long time = System.currentTimeMillis();
        int rainbowColor = hud != null ? hud.getColor(time).getRGB() : 0xFFFFFFFF;

        drawStringWithShadow(exhibitionText, x, y, rainbowColor);
        float currentX = x + getStringWidth(exhibitionText);

        int whiteColor = 0xFFFFFFFF;
        drawStringWithShadow(restText, currentX, y, whiteColor);
        currentX += getStringWidth(restText);

        int grayColor = 0xFFAAAAAA;
        drawStringWithShadow("[", currentX, y, grayColor);
        currentX += getStringWidth("[");

        drawStringWithShadow(fpsValue, currentX, y, whiteColor);
        currentX += getStringWidth(fpsValue);

        drawStringWithShadow("]", currentX, y, grayColor);
        currentX += getStringWidth("]");

        String space = " ";
        drawStringWithShadow(space, currentX, y, whiteColor);
        currentX += getStringWidth(space);

        drawStringWithShadow("[", currentX, y, grayColor);
        currentX += getStringWidth("[");

        drawStringWithShadow(pingValue, currentX, y, whiteColor);
        currentX += getStringWidth(pingValue);

        drawStringWithShadow("]", currentX, y, grayColor);

        GlStateManager.popMatrix();
    }

    private void renderWeedhackWatermark(float x, float y) {
        String text = "weedhack premium beta";
        float textWidth = mc.fontRendererObj.getStringWidth(text);
        float boxWidth = textWidth + 4;
        float boxHeight = 12;

        RenderUtils.drawRect(x, y, boxWidth + 8, boxHeight + 8, new Color(60, 60, 60));
        RenderUtils.drawRect(x + 1, y + 1, boxWidth + 6, boxHeight + 6, new Color(40, 40, 40));
        RenderUtils.drawRect(x + 2, y + 2, boxWidth + 4, boxHeight + 4, new Color(60, 60, 60));
        RenderUtils.drawRect(x + 3, y + 3, boxWidth + 2, boxHeight + 2, new Color(22, 22, 22));

        float textY = mc.fontRendererObj.FONT_HEIGHT > 12 ? y + (boxHeight - 12) / 2f + 1
                : y + (boxHeight - mc.fontRendererObj.FONT_HEIGHT) / 2f + 3;
        mc.fontRendererObj.drawStringWithShadow(text, x + 5, textY, 0xFFFFFFFF);

        float gradient = boxWidth + 2;
        for (int i = 0; i < gradient; i++) {
            float ratio = i / gradient;
            int r = (int) (255 + (255 - 255) * ratio);
            int g = (int) (255 + (0 - 255) * ratio);
            int b = (int) (0 + (255 - 0) * ratio);
            RenderUtils.drawRect(x + 3 + i, y + 3, 1, 1, new Color(r, g, b));
        }
    }

}
