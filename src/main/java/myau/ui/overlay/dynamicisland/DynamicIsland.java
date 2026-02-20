package myau.ui.overlay.dynamicisland;

import myau.Myau;
import myau.events.Render2DEvent;
import myau.module.modules.TargetHUD;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import myau.util.RoundedUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class DynamicIsland {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final List<IslandTrigger> TRIGGERS = new ArrayList<>();

    // Animation states
    private static float animX, animY, animW, animH;
    private static boolean initialized = false;
    private static boolean targetHudRegistered = false;

    static {
        TRIGGERS.add(new ScaffoldIsland());
        TRIGGERS.add(new DefaultIsland());
    }

    public static void render(Render2DEvent event, float hudScale) {
        if (!targetHudRegistered && Myau.moduleManager != null) {
            try {
                TargetHUD targetHUD = (TargetHUD) Myau.moduleManager.getModule(TargetHUD.class);
                if (targetHUD != null) {
                    TRIGGERS.add(new TargetHUDIsland(targetHUD));
                }
            } catch (Exception ignored) {
            }
            targetHudRegistered = true;
        }

        if (TRIGGERS.isEmpty()) return;

        TRIGGERS.sort((a, b) -> Integer.compare(b.getIslandPriority(), a.getIslandPriority()));

        IslandTrigger trigger = TRIGGERS.stream().filter(i -> i.isAvailable()).findFirst().orElse(TRIGGERS.get(0));

        ScaledResolution sr = new ScaledResolution(mc);
        float targetW = trigger.getIslandWidth();
        float targetH = trigger.getIslandHeight();
        float targetX = (sr.getScaledWidth() - targetW) / 2f;
        float targetY = 10;

        if (trigger instanceof CustomIslandTrigger) {
            targetX = ((CustomIslandTrigger) trigger).getIslandX();
            targetY = ((CustomIslandTrigger) trigger).getIslandY();
        }

        float speed = 0.15f;
        if (!initialized) {
            animX = targetX; animY = targetY; animW = targetW; animH = targetH;
            initialized = true;
        }
        animX += (targetX - animX) * speed;
        animY += (targetY - animY) * speed;
        animW += (targetW - animW) * speed;
        animH += (targetH - animH) * speed;

        float x = animX / hudScale;
        float y = animY / hudScale;
        float w = animW / hudScale;
        float h = animH / hudScale;

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x, y, x + w, y + h, new Color(10, 10, 10, 200).getRGB());

        RenderUtil.drawRect(x, y, x + w, y + 1.5f, new Color(255, 255, 255, 100).getRGB());

        RoundedUtils.drawOutlineRect(x - 0.5f, y - 0.5f, x + w + 0.5f, y + h + 0.5f, 0.5f, 0, new Color(0, 0, 0, 100).getRGB());
        RenderUtil.disableRenderState();

        float progress = Math.min(1.0f, animW / targetW);
        trigger.renderIsland(event, x, y, w, h, progress);
    }
}