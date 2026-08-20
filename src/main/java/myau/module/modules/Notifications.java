package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.font.CFontRenderer;
import myau.management.NotificationManager;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

public class Notifications extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty maxNotifications = new IntProperty("max-notifications", 4, 1, 8);

    public Notifications() {
        super("Notifications", true, false, "Animated module and client notifications");
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.gameSettings.showDebugInfo || Myau.notificationManager == null) {
            return;
        }

        List<NotificationManager.NotificationEntry> entries = Myau.notificationManager.getActive();
        if (entries.isEmpty()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float margin = 8.0F;
        float paddingX = 8.0F;
        float paddingY = 5.0F;
        float spacing = 4.0F;
        float y = sr.getScaledHeight() - margin;
        int rendered = 0;

        for (int i = entries.size() - 1; i >= 0 && rendered < this.maxNotifications.getValue(); i--) {
            NotificationManager.NotificationEntry entry = entries.get(i);
            float alpha = this.notificationAlpha(entry);
            if (alpha <= 0.01F) {
                continue;
            }

            String text = this.formatNotificationText(entry.message);
            float textHeight = this.getTextHeight();
            float boxWidth = Math.max(86.0F, this.getTextWidth(text) + paddingX * 2.0F + 2.0F);
            float boxHeight = textHeight + paddingY * 2.0F + 3.0F;
            float x = sr.getScaledWidth() - margin - boxWidth;
            y -= boxHeight;

            this.drawClassicNotification(entry, text, x, y, boxWidth, boxHeight, paddingX, paddingY, alpha);

            y -= spacing;
            rendered++;
        }
    }

    private void drawClassicNotification(NotificationManager.NotificationEntry entry, String text,
                                         float x, float y, float boxWidth, float boxHeight,
                                         float paddingX, float paddingY, float alpha) {
        float renderX = this.getAnimatedX(entry, x, alpha);
        int statusColor = this.getStatusColor(entry, text, alpha);
        float radius = 6.0F;

        int panel = new Color(10, 12, 16, (int) (92 * alpha)).getRGB();
        int hoverLayer = new Color(255, 255, 255, (int) (9 * alpha)).getRGB();
        int border = new Color(255, 255, 255, (int) (24 * alpha)).getRGB();
        int depth = new Color(0, 0, 0, (int) (28 * alpha)).getRGB();
        int neutralText = new Color(238, 241, 245, (int) (242 * alpha)).getRGB();

        RenderUtil.drawRoundedRect(renderX + 1.0F, y + 1.5F, boxWidth, boxHeight, radius + 1.0F,
                depth, true, true, true, true);
        RenderUtil.drawRoundedRect(renderX, y, boxWidth, boxHeight, radius,
                panel, true, true, true, true);
        RenderUtil.drawRoundedRect(renderX + 1.0F, y + 1.0F, boxWidth - 2.0F, boxHeight - 2.0F, radius - 1.0F,
                hoverLayer, true, true, true, true);
        RenderUtil.drawRoundedRectOutline(renderX + 0.5F, y + 0.5F, boxWidth - 1.0F, boxHeight - 1.0F,
                radius, 1.0F, border, true, true, true, true);

        this.drawProgress(entry, renderX, y, boxWidth, boxHeight, statusColor, alpha, 1.0F);
        this.drawNotificationText(text, renderX + paddingX + 1.0F, y + paddingY + 1.0F,
                new Color(238, 241, 245, (int) (242 * alpha)).getRGB(), statusColor);
    }

    private void drawProgress(NotificationManager.NotificationEntry entry, float x, float y, float width,
                              float height, int statusColor, float alpha, float thickness) {
        float progressWidth = width - 16.0F;
        float progressX = x + 8.0F;
        float progressY = y + height - thickness - 2.0F;
        float progress = this.notificationProgress(entry);

        RenderUtil.drawRoundedRect(progressX, progressY, progressWidth, thickness, thickness / 2.0F,
                new Color(255, 255, 255, (int) (16 * alpha)).getRGB(), true, true, true, true);
        RenderUtil.drawRoundedRect(progressX, progressY, Math.max(thickness, progressWidth * progress), thickness,
                thickness / 2.0F, statusColor, true, true, true, true);
    }

    private float getAnimatedX(NotificationManager.NotificationEntry entry, float x, float alpha) {
        float motion = this.notificationMotion(entry);
        return x + (1.0F - motion) * 14.0F + (1.0F - alpha) * 5.0F;
    }

    private float notificationAlpha(NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) {
            return 1.0F;
        }

        float age = entry.getAge();
        float remaining = entry.durationMillis - age;
        float fade = Math.min(220.0F, entry.durationMillis / 3.0F);
        float alpha = Math.min(1.0F, Math.min(age / fade, remaining / fade));
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        return alpha * alpha * (3.0F - 2.0F * alpha);
    }

    private float notificationProgress(NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, 1.0F - entry.getAge() / (float) entry.durationMillis));
    }

    private float notificationMotion(NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) {
            return 1.0F;
        }

        float age = entry.getAge();
        float remaining = entry.durationMillis - age;
        float in = Math.max(0.0F, Math.min(1.0F, age / 260.0F));
        float out = Math.max(0.0F, Math.min(1.0F, remaining / 220.0F));
        float motion = Math.min(in, out);
        return motion * motion * (3.0F - 2.0F * motion);
    }

    private String formatNotificationText(String message) {
        if (message == null) {
            return "";
        }
        return message
                .replace(" was toggled successfully", " enabled")
                .replace(" was untoggled successfully", " disabled");
    }

    private int getStatusColor(NotificationManager.NotificationEntry entry, String text, float alpha) {
        String lower = text.toLowerCase(Locale.ROOT);
        int color = lower.endsWith(" enabled") ? 0x41D982
                : lower.endsWith(" disabled") ? 0xFF5C6C
                : entry.color & 0xFFFFFF;
        return this.colorWithAlpha(color, (int) (245 * alpha));
    }

    private int colorWithAlpha(int rgb, int alpha) {
        return (rgb & 0xFFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private CFontRenderer getFontRenderer() {
        if (Myau.moduleManager == null) {
            return null;
        }
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        return hud == null || hud.fontMode.getValue() == 1 ? null : hud.fontRenderer;
    }

    private float getTextWidth(String text) {
        CFontRenderer font = this.getFontRenderer();
        return font == null ? mc.fontRendererObj.getStringWidth(text) : font.getStringWidth(text);
    }

    private float getTextHeight() {
        CFontRenderer font = this.getFontRenderer();
        return font == null ? mc.fontRendererObj.FONT_HEIGHT : font.FONT_HEIGHT;
    }

    private void drawText(String text, float x, float y, int color) {
        CFontRenderer font = this.getFontRenderer();
        if (font == null) {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        } else {
            font.drawStringWithShadow(text, x, y, color);
        }
    }

    private void drawNotificationText(String text, float x, float y, int neutralColor, int statusColor) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.endsWith(" enabled")) {
            this.drawSplitNotificationText(text, " enabled", x, y, neutralColor, statusColor);
        } else if (lower.endsWith(" disabled")) {
            this.drawSplitNotificationText(text, " disabled", x, y, neutralColor, statusColor);
        } else {
            this.drawText(text, x, y, neutralColor);
        }
    }

    private void drawSplitNotificationText(String text, String suffix, float x, float y, int neutralColor, int statusColor) {
        String main = text.substring(0, text.length() - suffix.length());
        this.drawText(main, x, y, neutralColor);
        this.drawText(suffix.trim(), x + this.getTextWidth(main + " "), y, statusColor);
    }
}
