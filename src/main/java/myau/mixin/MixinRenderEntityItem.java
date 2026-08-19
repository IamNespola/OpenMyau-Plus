package myau.mixin;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import java.util.Random;
import myau.module.modules.ItemPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@SideOnly(Side.CLIENT)
@Mixin(RenderEntityItem.class)
public abstract class MixinRenderEntityItem extends Render<Entity> {
    @Unique
    private static final Random itemPhysics$random = new Random();

    @Unique private static long itemPhysics$lastNano = System.nanoTime();

    @Unique private static double itemPhysics$rotation;

    protected MixinRenderEntityItem(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(
            method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void itemPhysics$doRender(
            EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (ItemPhysics.instance == null || !ItemPhysics.instance.isEnabled()) return;
        ci.cancel();

        Minecraft mc = Minecraft.getMinecraft();
        RenderEntityItem self = (RenderEntityItem) (Object) this;

        double speed = ItemPhysics.instance.getRotationSpeed();
        itemPhysics$rotation = (System.nanoTime() - itemPhysics$lastNano) / 2500000.0 * speed;
        if (!mc.isGamePaused()) {
            itemPhysics$lastNano = System.nanoTime();
        } else {
            itemPhysics$rotation = 0;
        }

        ItemStack stack = entity.getEntityItem();
        if (stack == null || stack.getItem() == null) return;

        int seed = Item.getIdFromItem(stack.getItem()) + stack.getMetadata();
        itemPhysics$random.setSeed(seed);

        self.bindTexture(TextureMap.locationBlocksTexture);
        self.getRenderManager()
                .renderEngine
                .getTexture(TextureMap.locationBlocksTexture)
                .setBlurMipmap(false, false);

        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GlStateManager.pushMatrix();

        IBakedModel model = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
        boolean is3D = model.isGui3d();
        int count = itemPhysics$getModelCount(stack);

        GlStateManager.translate((float) x, (float) y, (float) z);
        if (is3D) {
            GlStateManager.scale(0.5f, 0.5f, 0.5f);
        }

        GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(entity.rotationYaw, 0.0f, 0.0f, 1.0f);

        if (is3D) {
            GlStateManager.translate(0.0, 0.0, -0.08);
        } else {
            GlStateManager.translate(0.0, 0.0, -0.04);
        }

        if (!entity.onGround) {
            double rot = itemPhysics$rotation * 2.0;
            entity.rotationPitch += (float) rot;
        } else if (!is3D) {
            entity.rotationPitch = 0.0f;
        }

        if (is3D || mc.getRenderManager().options != null) {
            GlStateManager.rotate(entity.rotationPitch, 1.0f, 0.0f, 0.0f);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        for (int k = 0; k < count; ++k) {
            GlStateManager.pushMatrix();
            if (is3D) {
                if (k > 0) {
                    float ox = (itemPhysics$random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    float oy = (itemPhysics$random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    float oz = (itemPhysics$random.nextFloat() * 2.0f - 1.0f) * 0.15f;
                    GlStateManager.translate(
                            self.shouldSpreadItems() ? ox : 0.0f, self.shouldSpreadItems() ? oy : 0.0f, oz);
                }
                model =
                        ForgeHooksClient.handleCameraTransforms(
                                model, ItemCameraTransforms.TransformType.GROUND);
                mc.getRenderItem().renderItem(stack, model);
                GlStateManager.popMatrix();
            } else {
                model =
                        ForgeHooksClient.handleCameraTransforms(
                                model, ItemCameraTransforms.TransformType.GROUND);
                mc.getRenderItem().renderItem(stack, model);
                GlStateManager.popMatrix();
                GlStateManager.translate(0.0f, 0.0f, 0.05375f);
            }
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();

        self.bindTexture(TextureMap.locationBlocksTexture);
        self.getRenderManager()
                .renderEngine
                .getTexture(TextureMap.locationBlocksTexture)
                .restoreLastBlurMipmap();
    }

    @Unique
    private static int itemPhysics$getModelCount(ItemStack stack) {
        if (stack.stackSize > 48) return 5;
        if (stack.stackSize > 32) return 4;
        if (stack.stackSize > 16) return 3;
        if (stack.stackSize > 1) return 2;
        return 1;
    }
}