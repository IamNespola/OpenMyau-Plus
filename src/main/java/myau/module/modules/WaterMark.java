//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package myau.module.modules;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import myau.font.CFontRenderer;
import myau.font.CFont;
import myau.font.FontProcess;
import myau.property.properties.*;


import java.awt.*;

public class WaterMark extends Module {
    CFontRenderer fontRenderer = FontProcess.getFont("sans");
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Color ACCENT_PINK = new Color(255, 100, 150);
    private static final Color ACCENT_PURPLE = new Color(170, 100, 255);
    public final ModeProperty style = new ModeProperty("style", 0, new String[]{"SIMPLE", "BOX"});
    public final BooleanProperty showTime = new BooleanProperty("time", true);
    public final BooleanProperty showFps = new BooleanProperty("fps", false);
    public final BooleanProperty showName = new BooleanProperty("show-name", false);
    public final IntProperty posX = new IntProperty("x", 4, 0, 500);
    public final IntProperty posY = new IntProperty("y", 4, 0, 500);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public WaterMark() {
        super("WaterMark", true, true);
    }

    private ResourceLocation getPlayerSkin() {
        if (mc.thePlayer == null) {
            return null;
        } else {
            try {
                NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getName());
                if (playerInfo != null) {
                    return playerInfo.getLocationSkin();
                }
            } catch (Exception ignored) {
            }

            return null;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && mc.thePlayer != null && !mc.gameSettings.showDebugInfo) {
            ScaledResolution sr = new ScaledResolution(mc);
            String clientName = "OpenMyau+";
            String time = this.timeFormat.format(new Date());
            String fps = Minecraft.getDebugFPS() + " fps";
            StringBuilder display = new StringBuilder(clientName);
            if (this.showTime.getValue()) {
                display.append(" | ").append(time);
            }

            if (this.showFps.getValue()) {
                display.append(" | ").append(fps);
            }

            String text = display.toString();
            float x = (float) this.posX.getValue();
            float y = (float) this.posY.getValue();
            int textWidth = mc.fontRendererObj.getStringWidth(text);

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            if (this.style.getValue() == 1) {
                RenderUtil.enableRenderState();
                float boxWidth = (float) (textWidth + 10);
                float boxHeight = 14.0F;
                int bgColor = (new Color(12, 12, 16, 200)).getRGB();
                RenderUtil.drawRect(x, y, x + boxWidth, y + boxHeight, bgColor);
                int accentColor = ACCENT_PINK.getRGB();
                RenderUtil.drawRect(x, y + 2.0F, x + 2.0F, y + boxHeight - 2.0F, accentColor);
                RenderUtil.disableRenderState();

                mc.fontRendererObj.drawStringWithShadow(text, x + 5.0F, y + 3.0F, (new Color(255, 255, 255)).getRGB());

                if (this.showName.getValue() && mc.thePlayer != null) {
                    String playerName = mc.thePlayer.getName();
                    int nameWidth = mc.fontRendererObj.getStringWidth(playerName);
                    int faceSize = 14;
                    float nameBoxWidth = (float) (nameWidth + faceSize + 12);
                    float nameBoxHeight = (float) (faceSize + 4);
                    float nameY = y + boxHeight + 4.0F;
                    RenderUtil.enableRenderState();
                    RenderUtil.drawRect(x, nameY, x + nameBoxWidth, nameY + nameBoxHeight, bgColor);
                    RenderUtil.drawRect(x, nameY + 2.0F, x + 2.0F, nameY + nameBoxHeight - 2.0F, ACCENT_PURPLE.getRGB());
                    RenderUtil.disableRenderState();
                    ResourceLocation skin = this.getPlayerSkin();
                    if (skin != null) {
                        GlStateManager.pushMatrix();
                        GlStateManager.enableBlend();
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                        mc.getTextureManager().bindTexture(skin);
                        Gui.drawScaledCustomSizeModalRect((int) (x + 4.0F), (int) (nameY + 2.0F), 8.0F, 8.0F, 8, 8, faceSize, faceSize, 64.0F, 64.0F);
                        Gui.drawScaledCustomSizeModalRect((int) (x + 4.0F), (int) (nameY + 2.0F), 40.0F, 8.0F, 8, 8, faceSize, faceSize, 64.0F, 64.0F);
                        GlStateManager.popMatrix();
                    }

                    mc.fontRendererObj.drawStringWithShadow(playerName, x + 6.0F + (float) faceSize, nameY + (nameBoxHeight - 8.0F) / 2.0F, (new Color(255, 255, 255)).getRGB());
                }
            } else {
                // SIMPLE style
                int pinkColor = ACCENT_PINK.getRGB();
                int whiteColor = (new Color(200, 200, 200)).getRGB();
                mc.fontRendererObj.drawStringWithShadow(clientName, x, y, pinkColor);
                float var24 = x + (float) fontRenderer.getStringWidth(clientName);
                if (this.showTime.getValue()) {
                    mc.fontRendererObj.drawStringWithShadow(" | ", var24, y, whiteColor);
                    var24 += (float) mc.fontRendererObj.getStringWidth(" | ");
                    mc.fontRendererObj.drawStringWithShadow(time, var24, y, whiteColor);
                    var24 += (float) mc.fontRendererObj.getStringWidth(time);
                }

                if (this.showFps.getValue()) {
                    mc.fontRendererObj.drawStringWithShadow(" | ", var24, y, whiteColor);
                    var24 += (float) mc.fontRendererObj.getStringWidth(" | ");
                    mc.fontRendererObj.drawStringWithShadow(fps, var24, y, whiteColor);
                }

                if (this.showName.getValue() && mc.thePlayer != null) {
                    String playerName = mc.thePlayer.getName();
                    int faceSize = 10;
                    ResourceLocation skin = this.getPlayerSkin();
                    if (skin != null) {
                        GlStateManager.pushMatrix();
                        GlStateManager.enableBlend();
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                        mc.getTextureManager().bindTexture(skin);
                        Gui.drawScaledCustomSizeModalRect((int) x, (int) (y + 12.0F), 8.0F, 8.0F, 8, 8, faceSize, faceSize, 64.0F, 64.0F);
                        Gui.drawScaledCustomSizeModalRect((int) x, (int) (y + 12.0F), 40.0F, 8.0F, 8, 8, faceSize, faceSize, 64.0F, 64.0F);
                        GlStateManager.popMatrix();
                    }

                    mc.fontRendererObj.drawStringWithShadow(playerName, x + (float) faceSize + 3.0F, y + 13.0F, ACCENT_PURPLE.getRGB());
                }
            }

            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}
