package myau.ui.overlay.dynamicisland;

import myau.Myau;
import myau.events.Render2DEvent;
import myau.module.modules.HUD;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.EnumChatFormatting;
import java.awt.Color;

public class DefaultIsland implements IslandTrigger {
    private static float textAnimation = 0f;
    private static float iconAnimation = 0f;
    
    @Override
    public void renderIsland(Render2DEvent event, float posX, float posY, float width, float height, float progress) {
        if (progress < 0.88f) return;

        Minecraft mc = Minecraft.getMinecraft();
        String username = mc.getSession() != null ? mc.getSession().getUsername() : (mc.thePlayer != null ? mc.thePlayer.getName() : "Player");
        int fps = Minecraft.getDebugFPS();
        int ping = 0;
        if (mc.getNetHandler() != null && mc.thePlayer != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            if (info != null) ping = info.getResponseTime();
        }

        int nameColor = -1;
        try {
            HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
            if (hud != null) nameColor = hud.getColor(System.currentTimeMillis()).getRGB();
        } catch (Exception ignored) {
        }
        
        // Update animations
        textAnimation += 0.03f;
        iconAnimation += 0.05f;
        float textPulse = (float) Math.sin(textAnimation) * 0.2f + 0.8f;
        float iconPulse = (float) Math.sin(iconAnimation) * 0.3f + 0.7f;

        float centerY = posY + (height - mc.fontRendererObj.FONT_HEIGHT) / 2f + 1f;
        float pad = 15f;
        
        // Render animated icon before text
        renderAnimatedIcon(mc, posX + pad/2, centerY, iconPulse);
        
        float xOffset = pad + 12f;
        
        // Enhanced "Myau" with rainbow effect
        Color myauColor = new Color(nameColor);
        float rainbowShift = (textAnimation * 0.1f) % 1f;
        Color rainbowMyau = ColorUtil.fromHSB(rainbowShift, 0.8f, 1f);
        myauColor = ColorUtil.interpolate(textPulse, myauColor, rainbowMyau);
        mc.fontRendererObj.drawStringWithShadow("Myau", posX + xOffset, centerY, myauColor.getRGB());
        xOffset += mc.fontRendererObj.getStringWidth("Myau");
        
        // Animated "+" with glow effect
        Color plusGlow = new Color(255, 100 + (int)(50 * iconPulse), 100, (int)(255 * iconPulse));
        mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.RED + "+", posX + xOffset, centerY, plusGlow.getRGB());
        xOffset += mc.fontRendererObj.getStringWidth("+");
        
        // Animated separator
        String separator = EnumChatFormatting.GRAY + " |";
        Color separatorColor = new Color(200, 200, 200, (int)(150 * textPulse));
        mc.fontRendererObj.drawStringWithShadow(separator, posX + xOffset, centerY, separatorColor.getRGB());
        xOffset += mc.fontRendererObj.getStringWidth(separator);
        
        // Username with gradient effect
        Color userColor = ColorUtil.interpolate(textPulse, Color.WHITE, new Color(200, 200, 255));
        mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.WHITE + username, posX + xOffset, centerY, userColor.getRGB());
        xOffset += mc.fontRendererObj.getStringWidth(username);
        
        // FPS with color coding and animation
        String fpsText = fps + " fps";
        Color fpsColor = getFPSColor(fps);
        fpsColor = new Color(fpsColor.getRed(), fpsColor.getGreen(), fpsColor.getBlue(), (int)(255 * textPulse));
        mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + " | " + fpsText, posX + xOffset, centerY, fpsColor.getRGB());
        xOffset += mc.fontRendererObj.getStringWidth(" | " + fpsText);
        
        // Ping with color coding and animation
        String pingText = ping + "ms";
        Color pingColor = getPingColor(ping);
        pingColor = new Color(pingColor.getRed(), pingColor.getGreen(), pingColor.getBlue(), (int)(255 * textPulse));
        mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + " | " + pingText, posX + xOffset, centerY, pingColor.getRGB());
        
        // Add floating particles around text
        renderTextParticles(mc, posX, posY, width, height, textPulse);
    }
    
    private void renderAnimatedIcon(Minecraft mc, float x, float y, float pulse) {
        // Render a small animated icon/circle
        Color iconColor = ColorUtil.fromHSB((iconAnimation * 0.2f) % 1f, 0.7f, 1f);
        iconColor = new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), (int)(200 * pulse));
        
        // Outer glow
        for (int i = 3; i > 0; i--) {
            float size = i * 1.5f * pulse;
            Color glowColor = new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), (int)(50 / i * pulse));
            RenderUtil.fillCircle(x, y, size, 8, glowColor.getRGB());
        }
        
        // Core
        RenderUtil.fillCircle(x, y, 3f * pulse, 12, iconColor.getRGB());
    }
    
    private void renderTextParticles(Minecraft mc, float x, float y, float w, float h, float pulse) {
        // Small floating particles around the text
        for (int i = 0; i < 5; i++) {
            float particleX = x + (textAnimation * 20f + i * 30f) % w;
            float particleY = y + (float) Math.sin(textAnimation * 2f + i) * 5f;
            float particleSize = 0.5f + pulse * 0.5f;
            
            Color particleColor = new Color(150, 200, 255, (int)(80 * pulse));
            RenderUtil.fillCircle(particleX, particleY, particleSize, 4, particleColor.getRGB());
        }
    }
    
    private Color getFPSColor(int fps) {
        if (fps >= 120) return new Color(0, 255, 100); // Bright green
        if (fps >= 60) return Color.GREEN;
        if (fps >= 45) return new Color(255, 255, 0); // Yellow
        if (fps >= 30) return new Color(255, 165, 0); // Orange
        return Color.RED;
    }
    
    private Color getPingColor(int ping) {
        if (ping <= 30) return new Color(0, 255, 150); // Bright green
        if (ping <= 50) return Color.GREEN;
        if (ping <= 100) return new Color(255, 255, 0); // Yellow
        if (ping <= 150) return new Color(255, 165, 0); // Orange
        return Color.RED;
    }

    @Override
    public float getIslandWidth() { return 280; }
    @Override
    public float getIslandHeight() { return 35; }
    @Override
    public int getIslandPriority() { return -5; }
}
