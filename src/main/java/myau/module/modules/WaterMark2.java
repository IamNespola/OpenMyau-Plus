package myau.module.modules;

import myau.Myau;
import net.minecraft.client.Minecraft;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import myau.font.impl.UFontRenderer; // 必须导入自定义字体类

public class WaterMark2 extends Module {
    public static int x = 2;
    public static int y = 2;
    public final IntProperty rectLeft = new IntProperty("RectLeft", 2, 0, 20);
    public final IntProperty rectTop = new IntProperty("RectTop", 2, 0, 20);

    public static void setPosition(int x, int y) {
        WaterMark2.x = x;
        WaterMark2.y = y;
    }

    public WaterMark2() {
        super("WaterMark2", false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        UFontRenderer fr = Myau.fontManagers.getFont(20);
        String text = "Myau+";

        float textWidth = (float) fr.getStringWidth(text);
        float textHeight = (float) fr.getHeight();

        float padX = 6.0F;
        float padY = 4.0F;

        float startX = (float) x;
        float startY = (float) y;

        float rectRight = startX + textWidth + (padX);
        float rectBottom = startY + textHeight + (padY);

        float radius = 4.0f;

        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);

        int fillColor = 0x80000000;
        int hudColor = hud.getColor(System.currentTimeMillis()).getRGB();

        RenderUtil.drawRoundedGradientOutlinedRectangle(
                startX, startY, rectRight, rectBottom,
                radius, fillColor, hudColor, hudColor
        );

        fr.drawString(
                text,
                startX + padX / 2,
                startY,
                hudColor,
                RenderUtil.hudShadow()
        );
    }
}