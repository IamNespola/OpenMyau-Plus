package myau.module.modules;

import myau.event.EventManager;
import myau.event.EventTarget;
import myau.event.events.EventPlayerKill;
import myau.events.Render2DEvent; // Correct import for Render2DEvent
import myau.module.Module;
import myau.property.properties.LongProperty;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;

public class SeasonDisplay extends Module {
    public final LongProperty seasonStartTime = new LongProperty("Season Start Time", System.currentTimeMillis(), Long.MIN_VALUE, Long.MAX_VALUE);
    public final IntProperty killsCount = new IntProperty("Kills", 0, 0, Integer.MAX_VALUE);
    public final IntProperty posX = new IntProperty("x", 5, 0, 500);
    public final IntProperty posY = new IntProperty("y", 5, 0, 500);

    private final Minecraft mc = Minecraft.getMinecraft();

    public SeasonDisplay() {
        super("SeasonDisplay", false, true); // defaultPersistent should be true
    }

    @Override
    public void onEnabled() {
        EventManager.register(this);
        if (seasonStartTime.getValue() == 0) {
            seasonStartTime.setValue(System.currentTimeMillis());
            killsCount.setValue(0);
        }
    }

    @Override
    public void onDisabled() {
        EventManager.unregister(this);
    }

    @EventTarget
    public void onPlayerKill(EventPlayerKill event) {
        killsCount.setValue(killsCount.getValue() + 1);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) { // Changed to Render2DEvent
        if (!isEnabled()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;

        long currentTime = System.currentTimeMillis();
        long durationMillis = currentTime - seasonStartTime.getValue();

        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        String title = "SeasonDisplay";
        String duration = String.format("%d d %02d h %02d m %02d s", days, hours, minutes, seconds);
        String kills = String.format("%d", killsCount.getValue());

        int x = posX.getValue();
        int y = posY.getValue();

        // Fixed-size rounded box similar to Tenacity Statistics
        int padding = 6;
        int lineSpacing = 4;
        int boxWidth = 145;
        int titleHeight = fr.FONT_HEIGHT;
        int statsCount = 2;
        int boxHeight = padding * 2
                + titleHeight
                + 3 // gap + underline
                + statsCount * fr.FONT_HEIGHT
                + (statsCount - 1) * lineSpacing;

        // Draw background box
        RenderUtil.enableRenderState();
        int bgColor = new Color(18, 18, 22, 215).getRGB();
        int radius = 6;
        // Rounded background box
        RenderUtil.drawRoundedRect(x, y, boxWidth, boxHeight, radius, bgColor);

        // Title underline (like Tenacity Statistics)
        int titleWidth = fr.getStringWidth(title);
        int underlineX = x + padding;
        int underlineY = y + padding + titleHeight + 1;
        int underlineColor = new Color(255, 255, 255, 180).getRGB();
        RenderUtil.drawRect(underlineX, underlineY, underlineX + titleWidth - 1, underlineY + 1, underlineColor);
        RenderUtil.disableRenderState();

        int textX = x + padding;
        int titleY = y + padding;

        // Title on top
        fr.drawStringWithShadow(title, textX, titleY, 0xFFFFFFFF);

        // Stats below, styled like key/value rows
        int statY = underlineY + 3;

        String timeLabel = "Time: ";
        String killsLabel = "Kills: ";
        int labelColor = 0xFFCCCCCC;
        int valueColor = 0xFFFFFFFF;

        // Time row
        fr.drawStringWithShadow(timeLabel, textX, statY, labelColor);
        fr.drawStringWithShadow(duration, textX + fr.getStringWidth(timeLabel), statY, valueColor);

        // Kills row
        statY += fr.FONT_HEIGHT + lineSpacing;
        fr.drawStringWithShadow(killsLabel, textX, statY, labelColor);
        fr.drawStringWithShadow(kills, textX + fr.getStringWidth(killsLabel), statY, valueColor);
    }
}
