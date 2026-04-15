package myau.module.modules.render;

import java.awt.Color;
import myau.event.EventTarget;
import myau.event.events.EventPlayerKill;
import myau.events.Render2DEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.LongProperty;
import myau.util.shader.Shader2D;
import net.minecraft.client.gui.FontRenderer;

public class SeasonDisplay extends Module {
    
    public final LongProperty seasonStartTime = new LongProperty("Season Start Time", 0L, Long.MIN_VALUE, Long.MAX_VALUE);
    public final IntProperty killsCount = new IntProperty("Kills", 0, 0, Integer.MAX_VALUE);
    public final IntProperty posX = new IntProperty("x", 5, 0, 1000);
    public final IntProperty posY = new IntProperty("y", 5, 0, 1000);

    public SeasonDisplay() {
        super("SessionDisplay", Category.RENDER, false, false);
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        if (seasonStartTime.getValue() == 0) {
            seasonStartTime.setValue(System.currentTimeMillis());
            killsCount.setValue(0);
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        seasonStartTime.setValue(0L);
        killsCount.setValue(0);
    }

    @EventTarget
    public void onPlayerKill(EventPlayerKill event) {
        killsCount.setValue(killsCount.getValue() + 1);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (seasonStartTime.getValue() == 0) return;

        FontRenderer fr = mc.fontRendererObj;

        long durationMillis = System.currentTimeMillis() - seasonStartTime.getValue();
        long seconds = (durationMillis / 1000) % 60;
        long minutes = (durationMillis / (1000 * 60)) % 60;
        long hours = (durationMillis / (1000 * 60 * 60)) % 24;
        long days = durationMillis / (1000 * 60 * 60 * 24);

        StringBuilder timeBuilder = new StringBuilder();
        if (days > 0) timeBuilder.append(days).append("d ");
        if (hours > 0) timeBuilder.append(hours).append("h ");
        if (minutes > 0) timeBuilder.append(minutes).append("m ");
        
        timeBuilder.append(seconds).append("s");

        String title = "Session Display";
        String duration = timeBuilder.toString().trim();
        String kills = String.valueOf(killsCount.getValue());

        int x = posX.getValue();
        int y = posY.getValue();
        int padding = 8;
        int gap = 6;
        int boxWidth = 150;
        int boxHeight = (padding * 2) + (fr.FONT_HEIGHT * 3) + gap + 2;

        Shader2D.drawRoundedRect(x, y, boxWidth, boxHeight, 5, new Color(15, 15, 18, 120));

        int titleWidth = fr.getStringWidth(title);
        int centeredTitleX = x + (boxWidth - titleWidth) / 2;
        int currentY = y + padding;
        
        fr.drawStringWithShadow(title, centeredTitleX, currentY, 0xFFFFFFFF);
        
        int underlineY = currentY + fr.FONT_HEIGHT + 2;
        int underlineX = x + padding;
        int fullUnderlineWidth = boxWidth - (padding * 2);
        
        Shader2D.drawRoundedRect(underlineX, underlineY, fullUnderlineWidth, 1, 0.5f, new Color(255, 255, 255, 120));

        int statsY = underlineY + gap;
        int contentX = x + padding;
        
        fr.drawStringWithShadow("Time", contentX, statsY, 0xFFBBBBBB);
        int timeValueX = x + boxWidth - fr.getStringWidth(duration) - padding; 
        fr.drawStringWithShadow(duration, timeValueX, statsY, 0xFFFFFFFF);

        statsY += fr.FONT_HEIGHT + 3;
        fr.drawStringWithShadow("Kills", contentX, statsY, 0xFFBBBBBB);
        int killsValueX = x + boxWidth - fr.getStringWidth(kills) - padding;
        fr.drawStringWithShadow(kills, killsValueX, statsY, 0xFFFFFFFF);
    }
}