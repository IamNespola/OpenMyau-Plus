package myau.module.modules.render;

import java.awt.Color;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.util.shader.Shader2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;

public class DynamicIsland extends Module {
    public final ColorProperty textColor = new ColorProperty("AccentColor", new Color(255, 30, 0).getRGB());
    public final BooleanProperty textShadow = new BooleanProperty("TextShadow", true);
    public final BooleanProperty enableGlow = new BooleanProperty("Glow", true);

    private final int bgAlpha = 130;
    private final float radius = 8f;

    public DynamicIsland() {
        super("DynamicIsland", Category.RENDER,true, false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        String username = mc.getSession().getUsername();
        int ping = getPing();
        int fps = Minecraft.getDebugFPS();
        String server = getServerIP();

        String text = "Myau+  ·  " + username + "  ·  " + ping + "ms to " + server + "  ·  " + fps + "fps";

        float width = mc.fontRendererObj.getStringWidth(text) + 24f;
        float height = 26f;

        float x = sr.getScaledWidth() / 2f - width / 2f;
        float y = 8f;

        drawBackground(x, y, width, height);

        float textY = y + (height - mc.fontRendererObj.FONT_HEIGHT) / 2f;
        float startX = x + 12f;

        int accentRGB = new Color(this.textColor.getValue()).getRGB();

        mc.fontRendererObj.drawStringWithShadow(
                "Myau+",
                (int) startX,
                (int) textY,
                accentRGB);

        String part1 = "  ·  " + username + "  ·  ";
        float part1Width = mc.fontRendererObj.getStringWidth("Myau+");
        mc.fontRendererObj.drawStringWithShadow(
                part1,
                (int) (startX + part1Width),
                (int) textY,
                0xFFFFFF);

        String part2 = ping + "ms";
        float part2Width = mc.fontRendererObj.getStringWidth("Myau+" + part1);
        mc.fontRendererObj.drawStringWithShadow(
                part2,
                (int) (startX + part2Width),
                (int) textY,
                accentRGB);

        String rest = " to " + server + "  ·  " + fps + "fps";
        float restWidth = mc.fontRendererObj.getStringWidth("Myau+" + part1 + part2);
        mc.fontRendererObj.drawStringWithShadow(
                rest,
                (int) (startX + restWidth),
                (int) textY,
                0xFFFFFF);
    }

    private void drawBackground(float x, float y, float w, float h) {        
        Color accent = new Color(this.textColor.getValue());
        float rad = this.radius;

        if (this.enableGlow.getValue()) {
            Shader2D.drawGlow(x, y, w, h, rad, 12, 1f, 
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        }

        Shader2D.drawRoundedRect(x, y, w, h, rad, new Color(25, 25, 25, (int) this.bgAlpha));

    }

    private int getPing() {
        try {
            if (mc.thePlayer == null || mc.getNetHandler() == null) {
                return 0;
            }
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getName());
            if (playerInfo != null) {
                return playerInfo.getResponseTime();
            }
        } catch (Exception e) {
        }
        return 0;
    }

    private String getServerIP() {
        try {
            if (mc.theWorld != null) {
                if (mc.isIntegratedServerRunning()) {
                    return "SinglePlayer";
                } else if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
                    return mc.getCurrentServerData().serverIP;
                }
            }
        } catch (Exception e) {
        }
        return "SinglePlayer";
    }
}