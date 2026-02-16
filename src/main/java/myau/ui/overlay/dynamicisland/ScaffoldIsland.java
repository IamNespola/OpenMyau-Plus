package myau.ui.overlay.dynamicisland;

import myau.Myau;
import myau.events.Render2DEvent;
import myau.module.modules.Scaffold;
import myau.util.RenderUtil;
import myau.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import java.awt.Color;

public class ScaffoldIsland implements IslandTrigger {
    private static final float PROGRESS_BAR_LEN = 100f;
    private static final float PROGRESS_BAR_H = 3f;
    private static final float ITEM_SIZE = 19f;
    private static final float PAD = 6f;
    private static float scaffoldAnimation = 0f;

    @Override
    public boolean isAvailable() {
        return Myau.moduleManager.getModule(Scaffold.class).isEnabled();
    }

    @Override
    public void renderIsland(Render2DEvent event, float x, float y, float w, float h, float progress) {
        if (progress < 0.8f) return;
        Minecraft mc = Minecraft.getMinecraft();
        Scaffold scaffold = (Scaffold) Myau.moduleManager.getModule(Scaffold.class);
        int blockCount = scaffold.getBlockCount();
        float pitch = mc.thePlayer != null ? mc.thePlayer.rotationPitch : 0f;
        String pitchStr = String.format("%.2f", pitch);

        // Update animation
        scaffoldAnimation += 0.08f;
        float pulseFactor = (float) Math.sin(scaffoldAnimation) * 0.3f + 0.7f;

        float centerY = y + (h - mc.fontRendererObj.FONT_HEIGHT) / 2f + 1f;
        float cx = x + PAD;

        // Held block icon with enhanced rendering
        ItemStack stack = mc.thePlayer != null ? mc.thePlayer.inventory.getCurrentItem() : null;
        boolean isBlock = stack != null && stack.getItem() instanceof ItemBlock;
        if (isBlock) {
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();

            // Add subtle glow effect around item
            Color itemGlow = new Color(100, 200, 255, (int)(50 * pulseFactor));
            RenderUtil.enableRenderState();
            RenderUtil.drawOutlineRect(cx - 1, y + (h - ITEM_SIZE) / 2f - 1, 
                                       cx + ITEM_SIZE + 1, y + (h + ITEM_SIZE) / 2f + 1, 
                                       1f, 0, itemGlow.getRGB());
            RenderUtil.disableRenderState();

            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) cx, (int) (y + (h - ITEM_SIZE) / 2f));
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            cx += ITEM_SIZE + PAD;
        }

        // Enhanced progress bar with gradient effect
        float barY = y + (h - PROGRESS_BAR_H) / 2f;
        float barX = cx;
        float fillW = Math.min(1f, blockCount / 64f) * PROGRESS_BAR_LEN;

        RenderUtil.enableRenderState();

        // Background with rounded corners
        RenderUtil.drawRoundedRect((int) barX, (int) barY, (int) PROGRESS_BAR_LEN, (int) PROGRESS_BAR_H, 2, 0xF2000000);

        // Animated fill with color gradient based on block count
        if (fillW >= 2f) {
            Color barColor = blockCount > 32 ? Color.GREEN : (blockCount > 16 ? Color.YELLOW : Color.RED);
            barColor = new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), (int)(255 * pulseFactor));
            RenderUtil.drawRoundedRect((int) barX, (int) barY, (int) fillW, (int) PROGRESS_BAR_H, 2, barColor.getRGB());

            // Add shimmer effect
            float shimmerPos = (scaffoldAnimation * 20f) % PROGRESS_BAR_LEN;
            if (shimmerPos < fillW) {
                Color shimmerColor = new Color(255, 255, 255, (int)(100 * pulseFactor));
                RenderUtil.drawRect(barX + shimmerPos, barY, barX + shimmerPos + 3f, barY + PROGRESS_BAR_H, shimmerColor.getRGB());
            }
        }

        RenderUtil.disableRenderState();
        cx += PROGRESS_BAR_LEN + PAD;

        // Enhanced text with color coding
        String blocksStr = blockCount + " blocks";
        Color blocksColor = blockCount > 32 ? Color.GREEN : (blockCount > 16 ? Color.YELLOW : Color.RED);
        mc.fontRendererObj.drawStringWithShadow(blocksStr, cx, y + 5, blocksColor.getRGB());

        // Pitch with subtle animation
        Color pitchColor = ColorUtil.interpolate(pulseFactor, new Color(150, 150, 150), Color.WHITE);
        mc.fontRendererObj.drawStringWithShadow(EnumChatFormatting.GRAY + pitchStr + " °", cx, y + h - 5 - mc.fontRendererObj.FONT_HEIGHT, pitchColor.getRGB());
    }

    @Override
    public float getIslandWidth() { return 280; }
    @Override
    public float getIslandHeight() { return 30; }
    @Override
    public int getIslandPriority() { return 10; }
}
