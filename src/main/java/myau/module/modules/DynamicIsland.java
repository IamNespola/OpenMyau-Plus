package myau.module.modules;

import java.awt.Color;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.font.CFontRenderer;
import myau.module.Module;
import myau.module.modules.Scaffold;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.ModeProperty;
import myau.util.GlowUtils;
import myau.util.RenderUtil;
import myau.util.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;

public class DynamicIsland extends Module { // nah bro i took 2 hour just to did this shit
    private boolean showScaffold = false;
    private long scaffoldTime = 0;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ColorProperty textColor = new ColorProperty("AccentColor", new Color(255, 30, 0).getRGB());
    public final BooleanProperty textShadow = new BooleanProperty("TextShadow", true);
    public final BooleanProperty enableGlow = new BooleanProperty("Glow", true);

    private final int bgAlpha = 130;
    private final float radius = 8f;

    public DynamicIsland() { // we always love ai right(no)? credit: ChatGPT and Horaizion
        super("DynamicIsland", true, false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Scaffold scaffold = (Scaffold) myau.Myau.moduleManager.getModule(Scaffold.class);
        boolean scaffoldEnabled = scaffold != null && scaffold.isEnabled();
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        String username = mc.thePlayer.getName();
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
        RenderUtil.enableRenderState();

        Color accent = new Color(this.textColor.getValue());

        // ── Drop shadow (dark, offset downward) ──
        GlowUtils.drawGlow(
                x + 2f, y + 4f,
                w, h,
                40,
                new Color(0, 0, 0, 120));

        if (this.enableGlow.getValue()) {
            // ── Outer bloom – large, faint ──
            GlowUtils.drawGlow(
                    x, y,
                    w, h,
                    90,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));

            // ── Mid bloom – medium, moderate ──
            GlowUtils.drawGlow(
                    x, y,
                    w, h,
                    55,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));

            // ── Inner bloom – tight, vibrant ──
            GlowUtils.drawGlow(
                    x, y,
                    w, h,
                    25,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));
        }

        RoundedUtils.drawRoundedRect(
                x, y,
                w, h,
                this.radius,
                new Color(0, 0, 0, this.bgAlpha).getRGB());

        RoundedUtils.drawRoundedRect(
                x + 0.5f, y + 0.5f,
                w - 1f, h - 1f,
                this.radius - 0.5f,
                new Color(255, 255, 255, 18).getRGB());

        RenderUtil.disableRenderState();
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