package myau.ui.overlay.dynamicisland;

import myau.Myau;
import myau.events.Render2DEvent;
import myau.module.modules.FPScounter;
import myau.module.modules.TargetHUD;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import javax.vecmath.Vector4d;
import java.awt.Color;

public class TargetHUDIsland implements CustomIslandTrigger {
    private final TargetHUD parent;
    private final Minecraft mc = Minecraft.getMinecraft();
    private static FontRenderer fontRenderer;

    public TargetHUDIsland(TargetHUD parent) {
        this.parent = parent;
        fontRenderer = mc.fontRendererObj;
    }

    @Override
    public boolean isAvailable() {
        return parent != null && parent.isEnabled() && parent.getTarget() != null;
    }

    @Override
    public float getIslandX() {
        EntityLivingBase target = parent == null ? null : parent.getTarget();
        ScaledResolution sr = new ScaledResolution(mc);

        if (target == null || !parent.trackTarget.getValue() || target == mc.thePlayer || mc.thePlayer == null) {
            return (sr.getScaledWidth() - getIslandWidth()) / 2f;
        }

        Vector4d pos = RenderUtil.projectToScreen(target, 1.0);
        if (pos != null) {
            float centerX = (float) pos.x + ((float) (pos.z - pos.x) / 2f);
            return centerX - (getIslandWidth() / 2f);
        }

        return (sr.getScaledWidth() - getIslandWidth()) / 2f;
    }

    @Override
    public float getIslandY() {
        EntityLivingBase target = parent == null ? null : parent.getTarget();
        ScaledResolution sr = new ScaledResolution(mc);

        if (target == null || !parent.trackTarget.getValue() || target == mc.thePlayer || mc.thePlayer == null) {
            return 40;
        }

        Vector4d pos = RenderUtil.projectToScreen(target, 1.0);
        if (pos != null) {
            float y = (float) pos.y - getIslandHeight() - 10; // 10px above head
            // clamp so it doesn't go off-screen
            return Math.max(5, Math.min(y, sr.getScaledHeight() - getIslandHeight() - 5));
        }

        return 40;
    }

    @Override
    public void renderIsland(Render2DEvent event, float x, float y, float w, float h, float progress) {
        if (progress < 0.8f) return;
        EntityLivingBase target = parent == null ? null : parent.getTarget();
        if (target == null) return; // safety

        // Choose skin: try player skin when available
        ResourceLocation skin = new ResourceLocation("textures/entity/steve.png");
        if (target instanceof EntityPlayer) {
            try {
                NetworkPlayerInfo info = mc.getNetHandler() == null ? null : mc.getNetHandler().getPlayerInfo(target.getName());
                if (info != null && info.getLocationSkin() != null) skin = info.getLocationSkin();
            } catch (Exception ignored) {}
        }

        try {
            mc.getTextureManager().bindTexture(skin);
        } catch (Exception ignored) {}

        Gui.drawScaledCustomSizeModalRect((int) x + 5, (int) y + 5, 8, 8, 8, 8, 20, 20, 64, 64);

        String name = target.getName();
        fontRenderer.drawStringWithShadow(name == null ? "Unknown" : name, x + 30, y + 6, -1);

        float maxHealth = target.getMaxHealth() <= 0.0001f ? 1.0f : target.getMaxHealth();
        float healthPercent = Math.max(0f, Math.min(1f, target.getHealth() / maxHealth));
        float barWidth = Math.max(10f, w - 40f);

        // Fetch corner radius from FPScounter
        int cornerRadius = ((FPScounter) Myau.moduleManager.getModule(FPScounter.class)).cornerRadius.getValue();

        // drawRoundedRect expects integer coordinates/sizes — cast to int to match signature
        RenderUtil.drawRoundedRect((int) (x + 30), (int) (y + 18), (int) barWidth, 4, cornerRadius, new Color(0, 0, 0, 100).getRGB());
        RenderUtil.drawRoundedRect((int) (x + 30), (int) (y + 18), (int) (barWidth * healthPercent), 4, cornerRadius, getHealthColor(healthPercent));
    }

    private int getHealthColor(float pct) {
        float hue = Math.max(0f, Math.min(0.33f, pct * 0.33f));
        return Color.HSBtoRGB(hue, 1f, 1f);
    }

    @Override
    public float getIslandWidth() {
        return 120;
    }

    @Override
    public float getIslandHeight() {
        return 30;
    }

    @Override
    public int getIslandPriority() {
        return 100;
    }
}
