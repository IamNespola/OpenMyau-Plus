package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.events.Render3DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class BlockOverlay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Box", "OtherBox", "Outline"});
    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[]{"HUD", "Custom"});
    public final BooleanProperty depth = new BooleanProperty("depth", false);
    public final BooleanProperty info = new BooleanProperty("info", false);
    public final IntProperty thickness = new IntProperty("thickness", 2, 1, 5);
    public final IntProperty red = new IntProperty("red", 68, 0, 255, () -> this.colorMode.getValue() == 1);
    public final IntProperty green = new IntProperty("green", 117, 0, 255, () -> this.colorMode.getValue() == 1);
    public final IntProperty blue = new IntProperty("blue", 255, 0, 255, () -> this.colorMode.getValue() == 1);
    public final IntProperty alpha = new IntProperty("alpha", 100, 0, 255);

    public BlockOverlay() {
        super("BlockOverlay", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null) return;
        BlockPos pos = getCurrentBlock();
        if (pos == null) return;

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        block.setBlockBoundsBasedOnState(mc.theWorld, pos);

        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;
        AxisAlignedBB bb = block.getSelectedBoundingBox(mc.theWorld, pos)
                .expand(0.002D, 0.002D, 0.002D)
                .offset(-renderX, -renderY, -renderZ);

        Color color = getOverlayColor();
        GlStateManager.pushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(this.thickness.getValue());
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (!this.depth.getValue()) GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GlStateManager.color(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);

        String mode = this.mode.getModeString().toLowerCase();
        if (mode.equals("box") || mode.equals("otherbox")) {
            drawFilledBox(bb);
        }
        if (mode.equals("box") || mode.equals("outline")) {
            RenderGlobal.drawSelectionBoundingBox(bb);
        }

        GL11.glDepthMask(true);
        if (!this.depth.getValue()) GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || !this.info.getValue() || mc.theWorld == null) return;
        BlockPos pos = getCurrentBlock();
        if (pos == null) return;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        String text = block.getLocalizedName() + " \u00A77ID: " + Block.getIdFromBlock(block);
        ScaledResolution sr = new ScaledResolution(mc);
        int x = sr.getScaledWidth() / 2;
        int y = sr.getScaledHeight() / 2 + 7;
        int width = mc.fontRendererObj.getStringWidth(text);
        Gui.drawRect(x - 3, y - 3, x + width + 3, y + 9, 0xAA000000);
        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
    }

    private BlockPos getCurrentBlock() {
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null || !mc.theWorld.getWorldBorder().contains(pos)) return null;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block != Blocks.air && block != Blocks.water && block != Blocks.lava ? pos : null;
    }

    private Color getOverlayColor() {
        if (this.colorMode.getValue() == 0) {
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            Color hudColor = hud != null ? hud.getColor(System.currentTimeMillis()) : Color.WHITE;
            return new Color(hudColor.getRed(), hudColor.getGreen(), hudColor.getBlue(), this.alpha.getValue());
        }
        return new Color(this.red.getValue(), this.green.getValue(), this.blue.getValue(), this.alpha.getValue());
    }

    private void drawFilledBox(AxisAlignedBB bb) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glEnd();
    }
}