/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.ScaledResolution
 *  net.minecraft.client.renderer.GlStateManager
 */
package myau.module.modules.render;

import java.awt.Color;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.render.BlurShadowRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class FPScounter
        extends Module {
    public final BooleanProperty enabled = new BooleanProperty("Enabled", true);
    public final ModeProperty posX = new ModeProperty("Position-X", 1, new String[]{"Left", "Center", "Right"});
    public final ModeProperty posY = new ModeProperty("Position-Y", 1, new String[]{"Top", "Center", "Bottom"});
    public final IntProperty offsetX = new IntProperty("X-Offset", 0, -200, 200);
    public final IntProperty offsetY = new IntProperty("Y-Offset", 0, -200, 200);
    public final FloatProperty scale = new FloatProperty("Scale", 1.0f, 0.6f, 2.0f);
    public final IntProperty blurStrength = new IntProperty("Blur Strength", 6, 1, 10);
    public final IntProperty cornerRadius = new IntProperty("Corner Radius", 8, 5, 20);
    public final IntProperty backgroundAlpha = new IntProperty("Background Alpha", 160, 0, 255);
    public final ColorProperty textColor = new ColorProperty("Text Color", Color.WHITE.getRGB());

    public FPScounter() {
        super("Fpscounter", Category.RENDER,false, false);
    }

    @EventTarget
    public void onRender2DEvent(Render2DEvent event) {
        if (!((Boolean)this.enabled.getValue()).booleanValue() || !this.isEnabled()) {
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        float scaleFactor = ((Float)this.scale.getValue()).floatValue();
        float baseX = 0.0f;
        float baseY = 0.0f;
        switch ((int) this.posX.getValue()) {
            case 0: {
                baseX = 10.0f;
                break;
            }
            case 1: {
                baseX = (float) sr.getScaledWidth() / 2.0f;
                break;
            }
            case 2: {
                baseX = sr.getScaledWidth() - 10;
            }
        }
        switch ((int) this.posY.getValue()) {
            case 0: {
                baseY = 10.0f;
                break;
            }
            case 1: {
                baseY = (float) sr.getScaledHeight() / 2.0f;
                break;
            }
            case 2: {
                baseY = sr.getScaledHeight() - 10;
            }
        }
        baseX += (int) this.offsetX.getValue();
        baseY += (int) this.offsetY.getValue();
        int fps = Minecraft.getDebugFPS();
        String text = "FPS " + fps;
        int textWidth = mc.fontRendererObj.getStringWidth(text);
        int textHeight = mc.fontRendererObj.FONT_HEIGHT;
        float w = textWidth + 12;
        float h = textHeight + 6;
        float radius = ((Integer)this.cornerRadius.getValue()).intValue();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFactor, scaleFactor, 1.0f);
        float drawX = baseX / scaleFactor;
        float drawY = baseY / scaleFactor;
        BlurShadowRenderer.renderFrostedGlass(drawX - w / 2.0f, drawY - h / 2.0f, w, h, radius, (Integer)this.blurStrength.getValue(), (Integer)this.backgroundAlpha.getValue());
        int color = (Integer)this.textColor.getValue();
        mc.fontRendererObj.drawString(text, (int)(drawX - (float)textWidth / 2.0f), (int)(drawY - (float)textHeight / 2.0f), color);
        GlStateManager.popMatrix();
    }
}

