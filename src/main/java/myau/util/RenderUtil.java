package myau.util;

import myau.Myau;
import myau.enums.ChatColors;
import myau.mixin.IAccessorEntityRenderer;
import myau.mixin.IAccessorMinecraft;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.module.modules.HUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.glu.GLU;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class RenderUtil {
    private static Minecraft mc;
    private static Frustum cameraFrustum;
    private static IntBuffer viewportBuffer;
    private static FloatBuffer modelViewBuffer;
    private static FloatBuffer projectionBuffer;
    private static FloatBuffer vectorBuffer;
    private static Map<Integer, EnchantmentData> enchantmentMap;

    static {
        RenderUtil.mc = Minecraft.getMinecraft();
        RenderUtil.cameraFrustum = new Frustum();
        RenderUtil.viewportBuffer = GLAllocation.createDirectIntBuffer(16);
        RenderUtil.modelViewBuffer = GLAllocation.createDirectFloatBuffer(16);
        RenderUtil.projectionBuffer = GLAllocation.createDirectFloatBuffer(16);
        RenderUtil.vectorBuffer = GLAllocation.createDirectFloatBuffer(4);
        RenderUtil.enchantmentMap = new EnchantmentMap();
    }

    public static HUD getHud() {
        if (Myau.moduleManager == null) {
            return null;
        }
        Module module = Myau.moduleManager.getModule(HUD.class);
        return module instanceof HUD ? (HUD) module : null;
    }

    public static boolean hudShadow() {
        HUD hud = getHud();
        return hud == null || hud.shadow.getValue();
    }

    public static boolean hudBlur() {
        HUD hud = getHud();
        return hud == null || hud.blur.getValue();
    }

    public static boolean hudBloom() {
        HUD hud = getHud();
        return hud == null || hud.bloom.getValue();
    }

    public static boolean customHudEffects() {
        return hudBlur() && hudShadow();
    }

    private static ChatColors getColorForLevel(int currentLevel, int maxLevel) {
        if (currentLevel > maxLevel) {
            return ChatColors.LIGHT_PURPLE;
        }
        if (currentLevel == maxLevel) {
            return ChatColors.RED;
        }
        switch (currentLevel) {
            case 1: {
                return ChatColors.AQUA;
            }
            case 2: {
                return ChatColors.GREEN;
            }
            case 3: {
                return ChatColors.YELLOW;
            }
            case 4: {
                return ChatColors.GOLD;
            }
        }
        return ChatColors.GRAY;
    }

    public static org.lwjgl.util.Color interpolateColorHue(org.lwjgl.util.Color color1, org.lwjgl.util.Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        float[] color1HSB = java.awt.Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = java.awt.Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        java.awt.Color resultAWT = java.awt.Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount),
                interpolateFloat(color1HSB[1], color2HSB[1], amount), interpolateFloat(color1HSB[2], color2HSB[2], amount));

        org.lwjgl.util.Color resultColor = new org.lwjgl.util.Color((byte)resultAWT.getRed(), (byte)resultAWT.getGreen(), (byte)resultAWT.getBlue());

        return applyOpacity(resultColor, interpolateInt(color1.getAlpha(), color2.getAlpha(), amount) / 255f);
    }


    public static org.lwjgl.util.Color interpolateColorsBackAndForth(int speed, int index, org.lwjgl.util.Color start, org.lwjgl.util.Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return trueColor ? interpolateColorHue(start, end, angle / 360f) : interpolateColorC(start, end, angle / 360f);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static float interpolate(float old,
                                    float now,
                                    float partialTicks) {

        return old + (now - old) * partialTicks;
    }

    public static void drawOutlinedString(String text, float x, float y) {
        String string2 = text.replaceAll("(?i)§[\\da-f]", "");
        RenderUtil.mc.fontRendererObj.drawString(string2, x + 1.0f, y, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x - 1.0f, y, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x, y + 1.0f, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x, y - 1.0f, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(text, x, y, -1, false);
    }

    public static void renderEnchantmentText(ItemStack itemStack, float x, float y, float scale) {
        NBTTagList nBTTagList;
        nBTTagList = itemStack.getItem() == Items.enchanted_book ? Items.enchanted_book.getEnchantments(itemStack)
                : itemStack.getEnchantmentTagList();
        if (nBTTagList != null) {
            for (int i = 0; i < nBTTagList.tagCount(); ++i) {
                EnchantmentData enchantmentData = enchantmentMap.get(nBTTagList.getCompoundTagAt(i).getInteger("id"));
                if (enchantmentData == null) {
                    continue;
                }
                short s = nBTTagList.getCompoundTagAt(i).getShort("lvl");
                ChatColors chatColors = RenderUtil.getColorForLevel(s, enchantmentData.maxLevel);
                RenderUtil.drawOutlinedString(
                        ChatColors.formatColor(
                                String.format("&r%s%s%d&r", enchantmentData.shortName, chatColors, (int) s)),
                        x * (1.0f / scale), (y + (float) i * 4.0f) * (1.0f / scale));
            }
        }
    }

    public static void renderItemInGUI(ItemStack itemStack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        RenderUtil.mc.getRenderItem().zLevel = -150.0f;
        mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, x, y);
        mc.getRenderItem().renderItemOverlays(RenderUtil.mc.fontRendererObj, itemStack, x, y);
        RenderUtil.mc.getRenderItem().zLevel = 0.0f;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        GlStateManager.disableDepth();
        RenderUtil.renderEnchantmentText(itemStack, x, y, 0.5f);
        GlStateManager.enableDepth();
        GlStateManager.scale(2.0f, 2.0f, 2.0f);
        GlStateManager.popMatrix();
    }

    public static void renderPotionEffect(PotionEffect potionEffect, int x, int y) {
        int n3 = Potion.potionTypes[potionEffect.getPotionID()].getStatusIconIndex();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));
        Gui.drawModalRectWithCustomSizedTexture(x, y, n3 % 8 * 18, 198 + n3 / 8 * 18, 18, 18, 256.0f, 256.0f);
        GlStateManager.popMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static void glColor(final int n) {
        GL11.glColor4f((float) (n >> 16 & 0xFF) / 255.0f, (float) (n >> 8 & 0xFF) / 255.0f, (float) (n & 0xFF) / 255.0f, (float) (n >> 24 & 0xFF) / 255.0f);
    }


    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        return createFrameBuffer(framebuffer, false);
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
        if (needsNewFramebuffer(framebuffer)) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, depth);
            framebuffer.setFramebufferFilter(GL_LINEAR);
        }
        glBindTexture(GL_TEXTURE_2D, framebuffer.framebufferTexture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
        return framebuffer;
    }

    private static boolean needsNewFramebuffer(Framebuffer framebuffer) {
        return framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight;
    }

    public static void resetColor() {
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void bindTexture(int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    public static void setAlphaLimit(float limit) {
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL_GREATER, (float) (limit * .01));
    }

    public static void drawRoundedGradientOutlinedRectangle(float x, float y, float x2, float y2, final float radius, final int n6, final int n7, final int n8) {
        x *= 2.0f;
        y *= 2.0f;
        x2 *= 2.0f;
        y2 *= 2.0f;
        GL11.glPushAttrib(1);
        GL11.glScaled(0.5, 0.5, 0.5);
        glEnable(3042);
        GL11.glDisable(3553);
        glEnable(2848);
        GL11.glBegin(9);
        setColor(n6);
        for (int i = 0; i <= 90; i += 3) {
            final double n9 = i * 0.017453292f;
            GL11.glVertex2d((double) (x + radius) + Math.sin(n9) * radius * -1.0, (double) (y + radius) + Math.cos(n9) * radius * -1.0);
        }
        for (int j = 90; j <= 180; j += 3) {
            final double n10 = j * 0.017453292f;
            GL11.glVertex2d((double) (x + radius) + Math.sin(n10) * radius * -1.0, (double) (y2 - radius) + Math.cos(n10) * radius * -1.0);
        }
        for (int k = 0; k <= 90; k += 3) {
            final double n11 = k * 0.017453292f;
            GL11.glVertex2d((double) (x2 - radius) + Math.sin(n11) * radius, (double) (y2 - radius) + Math.cos(n11) * radius);
        }
        for (int l = 90; l <= 180; l += 3) {
            final double n12 = l * 0.017453292f;
            GL11.glVertex2d((double) (x2 - radius) + Math.sin(n12) * radius, (double) (y + radius) + Math.cos(n12) * radius);
        }
        GL11.glEnd();
        GL11.glPushMatrix();
        GL11.glShadeModel(7425);
        GL11.glLineWidth(2.0f);
        GL11.glBegin(2);
        if (n7 != 0L) {
            setColor(n7);
        }
        for (int n13 = 0; n13 <= 90; n13 += 3) {
            final double n14 = n13 * 0.017453292f;
            GL11.glVertex2d((double) (x + radius) + Math.sin(n14) * radius * -1.0, (double) (y + radius) + Math.cos(n14) * radius * -1.0);
        }
        for (int n15 = 90; n15 <= 180; n15 += 3) {
            final double n16 = n15 * 0.017453292f;
            GL11.glVertex2d((double) (x + radius) + Math.sin(n16) * radius * -1.0, (double) (y2 - radius) + Math.cos(n16) * radius * -1.0);
        }
        if (n8 != 0) {
            setColor(n8);
        }
        for (int n17 = 0; n17 <= 90; n17 += 3) {
            final double n18 = n17 * 0.017453292f;
            GL11.glVertex2d((double) (x2 - radius) + Math.sin(n18) * radius, (double) (y2 - radius) + Math.cos(n18) * radius);
        }
        for (int n19 = 90; n19 <= 180; n19 += 3) {
            final double n20 = n19 * 0.017453292f;
            GL11.glVertex2d((double) (x2 - radius) + Math.sin(n20) * radius, (double) (y + radius) + Math.cos(n20) * radius);
        }
        GL11.glEnd();
        glPopMatrix();
        glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        glEnable(3553);
        GL11.glScaled(2.0, 2.0, 2.0);
        GL11.glPopAttrib();
        GL11.glLineWidth(1.0f);
        GL11.glShadeModel(7424);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
    public static int mergeAlpha(int color, int alpha) {
        return (color & 0xFFFFFF) | alpha << 24;
    }

    public static void drawRoundedRectangle(float x, float y, float x2, float y2, float radius, final int color) {
        if (x2 <= x) {
            return;
        }

        float width = x2 - x;

        if (width < 3) {
            radius = Math.min(radius, width / 2.0f);
        }

        radius = Math.min(radius, 4.0f);

        x *= 2.0;
        y *= 2.0;
        x2 *= 2.0;
        y2 *= 2.0;
        GL11.glPushAttrib(0);
        GL11.glScaled(0.5, 0.5, 0.5);
        glEnable(3042);
        GL11.glDisable(3553);
        glEnable(2848);
        GL11.glBegin(9);
        glColor(color);
        for (int i = 0; i <= 90; i += 3) {
            final double n7 = i * 0.017453292f;
            GL11.glVertex2d((double) (x + radius) + Math.sin(n7) * radius * -1.0, (double) (y + radius) + Math.cos(n7) * radius * -1.0);
        }
        for (int j = 90; j <= 180; j += 3) {
            final double n8 = j * 0.017453292f;
            GL11.glVertex2d((double) (x + radius) + Math.sin(n8) * radius * -1.0, (double) (y2 - radius) + Math.cos(n8) * radius * -1.0);
        }
        if (x2 - x >= 4.5) {
            for (int k = 0; k <= 90; k += 1) {
                final double n9 = k * 0.017453292f;
                GL11.glVertex2d((double) (x2 - radius) + Math.sin(n9) * radius, (double) (y2 - radius) + Math.cos(n9) * radius);
            }
            for (int l = 90; l <= 180; l += 1) {
                final double n10 = l * 0.017453292f;
                GL11.glVertex2d((double) (x2 - radius) + Math.sin(n10) * radius, (double) (y + radius) + Math.cos(n10) * radius);
            }
        }
        GL11.glEnd();
        glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        glEnable(3553);
        GL11.glScaled(2.0, 2.0, 2.0);
        GL11.glPopAttrib();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawRoundedGradientRect(float x, float y, float x2, float y2, float radius, final int n6, final int n7, final int n8, final int n9) {
        if (x2 <= x) {
            return;
        }

        float width = x2 - x;

        if (width < 3) {
            radius = Math.min(radius, width / 2.0f);
        }

        radius = Math.min(radius, 4.0f);

        glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        glEnable(2848);
        GL11.glShadeModel(7425);
        GL11.glPushAttrib(0);
        GL11.glScaled(0.5, 0.5, 0.5);
        x *= 2.0;
        y *= 2.0;
        x2 *= 2.0;
        y2 *= 2.0;
        glEnable(3042);
        GL11.glDisable(3553);
        glColor(n6);
        glEnable(2848);
        GL11.glShadeModel(7425);
        GL11.glBegin(9);
        for (int i = 0; i <= 90; i += 3) {
            final double n10 = i * 0.017453292f;
            GL11.glVertex2d((double) (x + radius) + Math.sin(n10) * radius * -1.0, (double) (y + radius) + Math.cos(n10) * radius * -1.0);
        }
        glColor(n7);
        for (int j = 90; j <= 180; j += 3) {
            final double n11 = j * 0.017453292f;
            GL11.glVertex2d((double) (x + radius) + Math.sin(n11) * radius * -1.0, (double) (y2 - radius) + Math.cos(n11) * radius * -1.0);
        }
        if (x2 - x >= 4.5) {
            glColor(n8);
            for (int k = 0; k <= 90; k += 3) {
                final double n12 = k * 0.017453292f;
                GL11.glVertex2d((double) (x2 - radius) + Math.sin(n12) * radius, (double) (y2 - radius) + Math.cos(n12) * radius);
            }
            glColor(n9);
            for (int l = 90; l <= 180; l += 3) {
                final double n13 = l * 0.017453292f;
                GL11.glVertex2d((double) (x2 - radius) + Math.sin(n13) * radius, (double) (y + radius) + Math.cos(n13) * radius);
            }
        }
        GL11.glEnd();
        glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glDisable(3042);
        glEnable(3553);
        GL11.glScaled(2.0, 2.0, 2.0);
        GL11.glPopAttrib();
        glEnable(3553);
        GL11.glDisable(3042);
        GL11.glDisable(2848);
        GL11.glShadeModel(7424);
    }

    public static void drawGradientRect(int left, int top, float right, int bottom, int startColor, int endColor) {
        float startAlpha = (startColor >> 24 & 255) / 255.0F;
        float startRed = (startColor >> 16 & 255) / 255.0F;
        float startGreen = (startColor >> 8 & 255) / 255.0F;
        float startBlue = (startColor & 255) / 255.0F;
        float endAlpha = (endColor >> 24 & 255) / 255.0F;
        float endRed = (endColor >> 16 & 255) / 255.0F;
        float endGreen = (endColor >> 8 & 255) / 255.0F;
        float endBlue = (endColor & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos(left, top, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos(left, bottom, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static int clamp(int n) {
        if (n > 255) {
            return 255;
        }
        if (n < 0) {
            return 0;
        }
        return n;
    }

    public static int darkenColor(int color, double percent) {
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        percent = (100 - percent) / 100;

        red = (int) (red * percent);
        green = (int) (green * percent);
        blue = (int) (blue * percent);

        red = clamp(red);
        green = clamp(green);
        blue = clamp(blue);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static void renderItemAndEffectIntoGui3D(ItemStack stack, int xPos, int yPos) {
        if (stack == null) {
            return;
        }

        GlStateManager.pushMatrix();
        prepareGuiItemRenderState();
        GlStateManager.depthMask(true);
        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        mc.getRenderItem().zLevel = -150.0f;
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, xPos, yPos);
        mc.getRenderItem().zLevel = 0.0f;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        prepareGuiTextureRenderState();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void prepareGuiItemRenderState() {
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1, 1, 1, 1);
    }

    private static void prepareGuiTextureRenderState() {
        GlStateManager.disableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
    }


    public static void drawDurabilityBar(int xPos, int yPos, float durabilityRatio) {
        if (durabilityRatio < 0) durabilityRatio = 0;
        if (durabilityRatio > 1) durabilityRatio = 1;

        int barWidth = (int) (durabilityRatio * 13);

        GlStateManager.disableTexture2D();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(xPos + 2, yPos + 15, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
        wr.pos(xPos + 2, yPos + 16, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
        wr.pos(xPos + 15, yPos + 16, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
        wr.pos(xPos + 15, yPos + 15, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
        tess.draw();

        float r, g, b;
        if (durabilityRatio <= 0.3F) { r=1; g=0; b=0; }
        else if (durabilityRatio <= 0.6F) { r=1; g=1; b=0; }
        else { r=0; g=1; b=0; }

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(xPos + 2, yPos + 15, 0).color(r, g, b, 1.0F).endVertex();
        wr.pos(xPos + 2, yPos + 16, 0).color(r, g, b, 1.0F).endVertex();
        wr.pos(xPos + 2 + barWidth, yPos + 16, 0).color(r, g, b, 1.0F).endVertex();
        wr.pos(xPos + 2 + barWidth, yPos + 15, 0).color(r, g, b, 1.0F).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) {
            return;
        }
        RenderUtil.setColor(color);
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glEnd();
        GlStateManager.resetColor();
    }

    public static void drawRect3D(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) {
            return;
        }
        RenderUtil.setColor(color);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < 2; ++i) {
            GL11.glVertex2f(x1, y1);
            GL11.glVertex2f(x1, y2);
            GL11.glVertex2f(x2, y2);
            GL11.glVertex2f(x2, y1);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.resetColor();
    }

    public static void drawOutlineRect(float x1, float y1, float x2, float y2, float lineWidth, int backgroundColor,
                                       int lineColor) {
        RenderUtil.drawRect(0.0f, 0.0f, x2, 27.0f, backgroundColor);
        if (lineColor == 0) {
            return;
        }
        RenderUtil.setColor(lineColor);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawLine3D(Vec3 start, double endX, double endY, double endZ, float red, float green, float blue,
                                  float alpha, float lineWidth) {
        GlStateManager.pushMatrix();
        GlStateManager.color(red, green, blue, alpha);
        boolean bl = RenderUtil.mc.gameSettings.viewBobbing;
        RenderUtil.mc.gameSettings.viewBobbing = false;
        ((IAccessorEntityRenderer) RenderUtil.mc.entityRenderer)
                .callSetupCameraTransform(((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks, 2);
        RenderUtil.mc.gameSettings.viewBobbing = bl;
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(start.xCoord, start.yCoord, start.zCoord);
        GL11.glVertex3d(endX - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                endY - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                endZ - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ());
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    public static void drawArrow(float centerX, float centerY, float angle, float length, float lineWidth, int color) {
        float f6 = angle + (float) Math.toRadians(45.0);
        float f7 = angle - (float) Math.toRadians(45.0);
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f6), centerY + length * (float) Math.sin(f6));
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f7), centerY + length * (float) Math.sin(f7));
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawTriangle(float centerX, float centerY, float angle, float length, int color) {
        float f5 = angle + (float) Math.toRadians(26.25);
        float f6 = angle - (float) Math.toRadians(26.25);
        RenderUtil.setColor(color);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(9);
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f5), centerY + length * (float) Math.sin(f5));
        GL11.glVertex2f(centerX + length * (float) Math.cos(f6), centerY + length * (float) Math.sin(f6));
        GL11.glEnd();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.resetColor();
    }

    public static void drawFramebuffer(Framebuffer framebuffer) {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        GlStateManager.bindTexture(framebuffer.framebufferTexture);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(0.0, 1.0);
        GL11.glVertex2d(0.0, 0.0);
        GL11.glTexCoord2d(0.0, 0.0);
        GL11.glVertex2d(0.0, scaledResolution.getScaledHeight());
        GL11.glTexCoord2d(1.0, 0.0);
        GL11.glVertex2d(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
        GL11.glTexCoord2d(1.0, 1.0);
        GL11.glVertex2d(scaledResolution.getScaledWidth(), 0.0);
        GL11.glEnd();
    }

    public static void fillCircle(double x, double y, double radius, int segments, int color) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        RenderUtil.setColor(color);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        GL11.glVertex2d(x, y);

        for (int i = 0; i <= segments; i++) {
            double angle = i * (Math.PI * 2.0 / segments);
            double px = x + Math.cos(angle) * radius;
            double py = y + Math.sin(angle) * radius;
            GL11.glVertex2d(px, py);
        }

        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }

    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        drawRect(x + radius, y, x + width - radius, y + height, color);
        drawRect(x, y + radius, x + radius, y + height - radius, color);
        drawRect(x + width - radius, y + radius, x + width, y + height - radius, color);

        // Corners
        drawCircle(x + radius, y + radius, radius, color);
        drawCircle(x + width - radius, y + radius, radius, color);
        drawCircle(x + radius, y + height - radius, radius, color);
        drawCircle(x + width - radius, y + height - radius, radius, color);
    }

    public static void drawCircle(int centerX, int centerY, int radius, int color) {
        float alpha = (float) (color >> 24 & 0xFF) / 255.0f;
        float red = (float) (color >> 16 & 0xFF) / 255.0f;
        float green = (float) (color >> 8 & 0xFF) / 255.0f;
        float blue = (float) (color & 0xFF) / 255.0f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        worldRenderer.pos(centerX, centerY, 0).endVertex();

        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin(angle) * radius;
            worldRenderer.pos(x, y, 0).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawCircle(double centerX, double centerY, double centerZ, double radius, int segments,
                                  int color) {
        RenderUtil.setColor(color);
        GL11.glLineWidth(3.0f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= segments; ++i) {
            double d5 = (double) i * (Math.PI * 2 / (double) segments);
            GL11.glVertex3d(centerX + Math.cos(d5) * radius, centerY, centerZ + Math.sin(d5) * radius);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawEntityCircle(Entity entity, double radius, int segments, int color) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY() + entity.getEyeHeight();
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
        RenderUtil.drawCircle(d2, d3, d4, radius, segments, color);
    }

    public static void drawFilledBox(AxisAlignedBB axisAlignedBB, int red, int green, int blue) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63)
                .endVertex();
        tessellator.draw();
    }

    public static void drawBoundingBox(AxisAlignedBB axisAlignedBB, int red, int green, int blue, int alpha,
                                       float lineWidth) {
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderGlobal.drawOutlinedBoundingBox(axisAlignedBB, red, green, blue, alpha);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
    }

    public static void drawEntityBox(Entity entity, int red, int green, int blue) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        RenderUtil.drawFilledBox(entity.getEntityBoundingBox().expand(0.1f, 0.1f, 0.1f)
                        .offset(d2 - entity.posX, d3 - entity.posY, d4 - entity.posZ)
                        .offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()),
                red, green, blue);
    }

    public static void drawEntityDot(Entity entity, int red, int green, int blue) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY() + entity.getEyeHeight();
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

        GL11.glPushMatrix();
        GL11.glTranslated(d2, d3, d4);
        GL11.glPointSize(10.0f);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_POINTS);
        GL11.glColor4f(red / 255.0f, green / 255.0f, blue / 255.0f, 1.0f);
        GL11.glVertex3d(0, 0, 0);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GL11.glPopMatrix();
    }

    public static void drawEntityBoundingBox(Entity entity, int red, int green, int blue, int alpha, float lineWidth,
                                             double expand) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ,
                ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        RenderUtil.drawBoundingBox(
                entity.getEntityBoundingBox().expand(expand, expand, expand)
                        .offset(d2 - entity.posX, d3 - entity.posY, d4 - entity.posZ)
                        .offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()),
                red, green, blue, alpha, lineWidth);
    }

    public static void drawBlockBox(BlockPos blockPos, double height, int red, int green, int blue) {
        RenderUtil.drawFilledBox(new AxisAlignedBB(blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                        (double) blockPos.getX() + 1.0, (double) blockPos.getY() + height, (double) blockPos.getZ() + 1.0)
                        .offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()),
                red, green, blue);
    }

    public static void drawBlockBoundingBox(BlockPos blockPos, double height, int red, int green, int blue, int alpha,
                                            float lineWidth) {
        RenderUtil.drawBoundingBox(
                new AxisAlignedBB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), (double) blockPos.getX() + 1.0,
                        (double) blockPos.getY() + height, (double) blockPos.getZ() + 1.0)
                        .offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()),
                red, green, blue, alpha, lineWidth);
    }

    public static void drawCornerESP(EntityPlayer entity, float red, float green, float blue) {
        float x = (float) (RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX,
                ((IAccessorMinecraft) mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX());
        float y = (float) (RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY,
                ((IAccessorMinecraft) mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY());
        float z = (float) (RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ,
                ((IAccessorMinecraft) mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ());
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height / 2.0F, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(-0.098F, -0.098F, 0.098F);
        float width = (float) (26.6 * entity.width / 2.0);
        float height = 12.0F;
        GlStateManager.color(red, green, blue);
        draw3DRect(width, height - 1.0F, width - 4.0F, height);
        draw3DRect(-width, height - 1.0F, -width + 4.0F, height);
        draw3DRect(-width, height, -width + 1.0F, height - 4.0F);
        draw3DRect(width, height, width - 1.0F, height - 4.0F);
        draw3DRect(width, -height, width - 4.0F, -height + 1.0F);
        draw3DRect(-width, -height, -width + 4.0F, -height + 1.0F);
        draw3DRect(-width, -height + 1.0F, -width + 1.0F, -height + 4.0F);
        draw3DRect(width, -height + 1.0F, width - 1.0F, -height + 4.0F);
        GlStateManager.color(0.0F, 0.0F, 0.0F);
        draw3DRect(width, height, width - 4.0F, height + 0.2F);
        draw3DRect(-width, height, -width + 4.0F, height + 0.2F);
        draw3DRect(-width - 0.2F, height + 0.2F, -width, height - 4.0F);
        draw3DRect(width + 0.2F, height + 0.2F, width, height - 4.0F);
        draw3DRect(width + 0.2F, -height, width - 4.0F, -height - 0.2F);
        draw3DRect(-width - 0.2F, -height, -width + 4.0F, -height - 0.2F);
        draw3DRect(-width - 0.2F, -height, -width, -height + 4.0F);
        draw3DRect(width + 0.2F, -height, width, -height + 4.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void drawFake2DESP(EntityPlayer entity, float red, float green, float blue) {
        float x = (float) (RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX,
                ((IAccessorMinecraft) mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX());
        float y = (float) (RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY,
                ((IAccessorMinecraft) mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY());
        float z = (float) (RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ,
                ((IAccessorMinecraft) mc).getTimer().renderPartialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ());
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entity.height / 2.0F, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(-0.1F, -0.1F, 0.1F);
        GlStateManager.color(red, green, blue);
        float width = (float) (23.3 * entity.width / 2.0);
        float height = 12.0F;
        draw3DRect(width, height, -width, height + 0.4F);
        draw3DRect(width, -height, -width, -height + 0.4F);
        draw3DRect(width, -height + 0.4F, width - 0.4F, height + 0.4F);
        draw3DRect(-width, -height + 0.4F, -width + 0.4F, height + 0.4F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    public static void draw3DRect(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_POLYGON);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }

    public static Vector4d projectToScreen(Entity entity, double screenScale) {
        Vector4d vector4d;
        {
            double d3 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX,
                    ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            double d4 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY,
                    ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            double d5 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ,
                    ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            AxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox().expand(0.1f, 0.1f, 0.1f)
                    .offset(d3 - entity.posX, d4 - entity.posY, d5 - entity.posZ);
            vector4d = null;
            for (Vector3d vector3d : new Vector3d[] {
                    new Vector3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ),
                    new Vector3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                    new Vector3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ),
                    new Vector3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ),
                    new Vector3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                    new Vector3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ),
                    new Vector3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ),
                    new Vector3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ) }) {
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
                GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);
                if (!GLU.gluProject(
                        (float) (vector3d.x - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX()),
                        (float) (vector3d.y - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()),
                        (float) (vector3d.z - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()),
                        modelViewBuffer, projectionBuffer, viewportBuffer, vectorBuffer))
                    continue;
                vector3d = new Vector3d((double) vectorBuffer.get(0) / screenScale,
                        (double) ((float) Display.getHeight() - vectorBuffer.get(1)) / screenScale,
                        vectorBuffer.get(2));
                if (!(vector3d.z >= 0.0) || !(vector3d.z < 1.0))
                    continue;
                if (vector4d == null) {
                    vector4d = new Vector4d(vector3d.x, vector3d.y, vector3d.z, 0.0);
                }
                vector4d.x = Math.min(vector3d.x, vector4d.x);
                vector4d.y = Math.min(vector3d.y, vector4d.y);
                vector4d.z = Math.max(vector3d.x, vector4d.z);
                vector4d.w = Math.max(vector3d.y, vector4d.w);
            }
        }
        return vector4d;
    }

    public static boolean isInViewFrustum(AxisAlignedBB axisAlignedBB, double expand) {
        cameraFrustum.setPosition(RenderUtil.mc.getRenderViewEntity().posX, RenderUtil.mc.getRenderViewEntity().posY,
                RenderUtil.mc.getRenderViewEntity().posZ);
        return cameraFrustum.isBoundingBoxInFrustum(axisAlignedBB.expand(expand, expand, expand));
    }

    public static void enableRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
    }

    public static void disableRenderState() {
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void setColor(int argb) {
        float f = (float) (argb >> 24 & 0xFF) / 255.0f;
        float f2 = (float) (argb >> 16 & 0xFF) / 255.0f;
        float f3 = (float) (argb >> 8 & 0xFF) / 255.0f;
        float f4 = (float) (argb & 0xFF) / 255.0f;
        GlStateManager.color(f2, f3, f4, f);
    }

    public static float lerpFloat(float current, float previous, float t) {
        return previous + (current - previous) * t;
    }

    public static double lerpDouble(double current, double previous, double t) {
        return previous + (current - previous) * t;
    }

    public static final class EnchantmentData {
        public final String shortName;
        public final int maxLevel;

        public EnchantmentData(String shortName, int maxLevel) {
            this.shortName = shortName;
            this.maxLevel = maxLevel;
        }
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }
        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }
        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float j = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(g, h, j, f);
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(left, bottom, 0.0D).endVertex();
        worldRenderer.pos(right, bottom, 0.0D).endVertex();
        worldRenderer.pos(right, top, 0.0D).endVertex();
        worldRenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }


    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color, boolean roundTopLeft, boolean roundTopRight, boolean roundBottomLeft, boolean roundBottomRight) {
        if (width <= 0.0D || height <= 0.0D || (color >>> 24) == 0) {
            return;
        }

        radius = Math.max(0.0D, Math.min(radius, Math.min(width, height) / 2.0D));
        if (radius <= 0.0D || !(roundTopLeft || roundTopRight || roundBottomLeft || roundBottomRight)) {
            drawRect((float) x, (float) y, (float) (x + width), (float) (y + height), color);
            return;
        }

        enableRenderState();
        setColor(color);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

        drawQuadNoState(x + radius, y, x + width - radius, y + height);
        drawQuadNoState(x, y + radius, x + radius, y + height - radius);
        drawQuadNoState(x + width - radius, y + radius, x + width, y + height - radius);

        if (roundTopLeft) {
            drawCornerFan(x + radius, y + radius, radius, 180.0D, 270.0D);
        } else {
            drawQuadNoState(x, y, x + radius, y + radius);
        }

        if (roundTopRight) {
            drawCornerFan(x + width - radius, y + radius, radius, 270.0D, 360.0D);
        } else {
            drawQuadNoState(x + width - radius, y, x + width, y + radius);
        }

        if (roundBottomRight) {
            drawCornerFan(x + width - radius, y + height - radius, radius, 0.0D, 90.0D);
        } else {
            drawQuadNoState(x + width - radius, y + height - radius, x + width, y + height);
        }

        if (roundBottomLeft) {
            drawCornerFan(x + radius, y + height - radius, radius, 90.0D, 180.0D);
        } else {
            drawQuadNoState(x, y + height - radius, x + radius, y + height);
        }

        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        disableRenderState();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color, boolean roundTopLeft, boolean roundTopRight, boolean roundBottomLeft, boolean roundBottomRight) {
        drawRoundedRect((double) x, (double) y, (double) width, (double) height, (double) radius, color, roundTopLeft, roundTopRight, roundBottomLeft, roundBottomRight);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        drawRoundedRect(x, y, width, height, radius, color, true, true, true, true);
    }

    public static void roundedRectangle(double x, double y, double width, double height, double radius, java.awt.Color color) {
        drawRoundedRect(x, y, width, height, radius, color.getRGB(), true, true, true, true);
    }

    public static void roundedRectangle(double x, double y, double width, double height, double radius, java.awt.Color color,
                                        boolean leftTop, boolean rightTop, boolean rightBottom, boolean leftBottom) {
        drawRoundedRect(x, y, width, height, radius, color.getRGB(), leftTop, rightTop, leftBottom, rightBottom);
    }

    public static void rectangle(double x, double y, double width, double height, java.awt.Color color) {
        drawRect((float) x, (float) y, (float) (x + width), (float) (y + height), color.getRGB());
    }

    private static void drawQuadNoState(double x1, double y1, double x2, double y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(x1, y1);
        GL11.glVertex2d(x2, y1);
        GL11.glVertex2d(x2, y2);
        GL11.glVertex2d(x1, y2);
        GL11.glEnd();
    }

    private static void drawCornerFan(double centerX, double centerY, double radius, double start, double end) {
        int segments = Math.max(12, (int) Math.ceil(radius * 3.0D));
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2d(centerX, centerY);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.toRadians(start + (end - start) * i / segments);
            GL11.glVertex2d(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    public static void drawCircleOutline(float x, float y, float radius, float lineWidth, int color) {
        enableRenderState();
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f(x + (float) (Math.cos(angle) * radius), y + (float) (Math.sin(angle) * radius));
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0f);
        disableRenderState();
    }

    public static void drawRectOutline(double x, double y, double width, double height, float lineWidth, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + width, y);
        GL11.glVertex2d(x + width, y + height);
        GL11.glVertex2d(x, y + height);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        RenderUtil.disableRenderState();
    }

    public static void scissor(double x, double y, double width, double height) {
        if (width < 0.5 || height < 0.5) return;
        ScaledResolution sr = new ScaledResolution(mc);
        double scale = sr.getScaleFactor();
        double finalHeight = height * scale;
        double finalY = (sr.getScaledHeight_double() - (y + height)) * scale;
        double finalX = x * scale;
        double finalWidth = width * scale;
        finalX = Math.max(0, finalX);
        finalY = Math.max(0, finalY);
        finalWidth = Math.max(0, finalWidth);
        finalHeight = Math.max(0, finalHeight);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) finalX, (int) finalY, (int) finalWidth, (int) finalHeight);
    }

    public static void releaseScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float lineWidth, int color, boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        radius = Math.min(radius, Math.min(width, height) / 2.0f);
        float f = (float) (color >> 24 & 255) / 255.0F;
        float f1 = (float) (color >> 16 & 255) / 255.0F;
        float f2 = (float) (color >> 8 & 255) / 255.0F;
        float f3 = (float) (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f1, f2, f3, f);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        if (topLeft) {
            for (int i = 180; i <= 270; i += 3) {
                double rad = Math.toRadians(i);
                GL11.glVertex2d(x + radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
            }
        } else {
            GL11.glVertex2f(x, y);
        }

        if (topRight) {
            for (int i = 270; i <= 360; i += 3) {
                double rad = Math.toRadians(i);
                GL11.glVertex2d(x + width - radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
            }
        } else {
            GL11.glVertex2f(x + width, y);
        }

        if (bottomRight) {
            for (int i = 0; i <= 90; i += 3) {
                double rad = Math.toRadians(i);
                GL11.glVertex2d(x + width - radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
            }
        } else {
            GL11.glVertex2f(x + width, y + height);
        }

        if (bottomLeft) {
            for (int i = 90; i <= 180; i += 3) {
                double rad = Math.toRadians(i);
                GL11.glVertex2d(x + radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
            }
        } else {
            GL11.glVertex2f(x, y + height);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    static final class EnchantmentMap extends HashMap<Integer, EnchantmentData> {
        EnchantmentMap() {
            this.put(0, new EnchantmentData("Pr", 4));
            this.put(1, new EnchantmentData("Fp", 4));
            this.put(2, new EnchantmentData("Ff", 4));
            this.put(3, new EnchantmentData("Bp", 4));
            this.put(4, new EnchantmentData("Pp", 4));
            this.put(5, new EnchantmentData("Re", 3));
            this.put(6, new EnchantmentData("Aq", 1));
            this.put(7, new EnchantmentData("Th", 3));
            this.put(8, new EnchantmentData("Ds", 3));
            this.put(16, new EnchantmentData("Sh", 5));
            this.put(17, new EnchantmentData("Sm", 5));
            this.put(18, new EnchantmentData("BoA", 5));
            this.put(19, new EnchantmentData("Kb", 2));
            this.put(20, new EnchantmentData("Fa", 2));
            this.put(21, new EnchantmentData("Lo", 3));
            this.put(32, new EnchantmentData("Ef", 5));
            this.put(33, new EnchantmentData("St", 1));
            this.put(34, new EnchantmentData("Ub", 3));
            this.put(35, new EnchantmentData("Fo", 3));
            this.put(48, new EnchantmentData("Po", 5));
            this.put(49, new EnchantmentData("Pu", 2));
            this.put(50, new EnchantmentData("Fl", 1));
            this.put(51, new EnchantmentData("Inf", 1));
            this.put(61, new EnchantmentData("LoS", 3));
            this.put(62, new EnchantmentData("Lu", 3));
        }
    }
}
