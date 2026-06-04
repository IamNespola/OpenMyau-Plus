package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

public class BreakProgress extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Percentage", "Second", "Decimal"});
    public final BooleanProperty manual = new BooleanProperty("Show manual", true);
    public final BooleanProperty progressBar = new BooleanProperty("Progress bar", false);

    private double progress;
    private double animatedProgress;
    private BlockPos block;
    private String progressStr = "";

    public BreakProgress() {
        super("BreakProgress", false, false, "Shows block breaking progress.");
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        updateProgress();
        animatedProgress += (progress - animatedProgress) * 0.35D;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || this.progress == 0.0D || this.block == null || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        double x = this.block.getX() + 0.5D - mc.getRenderManager().viewerPosX;
        double y = this.block.getY() + 0.5D - mc.getRenderManager().viewerPosY;
        double z = this.block.getZ() + 0.5D - mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.02266667F, -0.02266667F, -0.02266667F);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        int textWidth = mc.fontRendererObj.getStringWidth(this.progressStr);
        mc.fontRendererObj.drawString(this.progressStr, -textWidth / 2.0F, -8.0F, -1, true);
        if (progressBar.getValue()) {
            drawProgressBar();
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void drawProgressBar() {
        int width = 42;
        int height = 4;
        int filled = (int) Math.round(width * Math.max(0.0D, Math.min(1.0D, animatedProgress)));
        net.minecraft.client.gui.Gui.drawRect(-width / 2, 4, width / 2, 4 + height, 0xAA101018);
        net.minecraft.client.gui.Gui.drawRect(-width / 2, 4, -width / 2 + filled, 4 + height, 0xFFD8FB6D);
    }

    private void updateProgress() {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.capabilities.isCreativeMode || !mc.thePlayer.capabilities.allowEdit) {
            resetVariables();
            return;
        }
        if (!manual.getValue() || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            resetVariables();
            return;
        }

        this.progress = ((IAccessorPlayerControllerMP) mc.playerController).getCurBlockDamageMP();
        if (this.progress == 0.0D) {
            resetVariables();
            return;
        }
        this.block = mc.objectMouseOver.getBlockPos();
        setProgressText();
    }

    private void setProgressText() {
        switch (mode.getValue()) {
            case 0:
                this.progressStr = (int) (100.0D * this.progress) + "%";
                break;
            case 1:
                this.progressStr = getTimeLeftText();
                break;
            case 2:
                this.progressStr = String.format("%.2f", this.progress);
                break;
            default:
                this.progressStr = "";
                break;
        }
    }

    private String getTimeLeftText() {
        if (block == null) return "0s";
        Block targetBlock = mc.theWorld.getBlockState(block).getBlock();
        float hardness = targetBlock.getPlayerRelativeBlockHardness(mc.thePlayer, mc.theWorld, block);
        if (hardness <= 0.0F) return "0s";
        double ticksLeft = Math.max(0.0D, 1.0D - this.progress) / hardness;
        double seconds = Math.round((ticksLeft / 20.0D) * 10.0D) / 10.0D;
        return seconds == 0.0D ? "0" : seconds + "s";
    }

    @Override
    public void onDisabled() {
        resetVariables();
    }

    private void resetVariables() {
        this.progress = 0.0D;
        this.animatedProgress = 0.0D;
        this.block = null;
        this.progressStr = "";
    }
}
