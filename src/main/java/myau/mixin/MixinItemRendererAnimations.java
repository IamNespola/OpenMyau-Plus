// not rat bro XD
// Original logic by syuto/animations-1.6, integrated into Uzi
package myau.mixin;

import myau.config.AnimationConfig;
import myau.config.AnimationMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = ItemRenderer.class, priority = 999)
public abstract class MixinItemRendererAnimations {

    private float spin = 0.0F;

    @Shadow @Final private Minecraft mc;

    @Shadow
    protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Redirect(
        method = "renderItemInFirstPerson",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V",
                 ordinal = 2)
    )
    private void skipTransform(ItemRenderer instance, float f1, float f2) {
        // Suppressed — animations replaces this below
    }

    @Inject(
        method = "renderItemInFirstPerson",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/ItemRenderer;doBlockTransformations()V")
    )
    public void applyAnimTransform(float partialTicks, CallbackInfo ci) {
        if (!AnimationConfig.isEnabled()) return;
        AnimationConfig.sync();

        IAccessorItemRendererAnimations acc = (IAccessorItemRendererAnimations) this;
        float equippedProgress     = acc.getEquippedProgress();
        float prevEquippedProgress = acc.getPrevEquippedProgress();
        float f = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);

        AbstractClientPlayer player = mc.thePlayer;
        float swingProgress = player.getSwingProgress(partialTicks);
        float sine          = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float sqrtSwing     = MathHelper.sqrt_float(swingProgress);
        float sine1         = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);

        // NOTE: using if-else instead of switch to avoid generating a synthetic
        // MixinItemRendererAnimations$1 class that the mixin system incorrectly
        // tries to load as a mixin (causing NoSuchFieldError on $SwitchMap).
        AnimationMode m = AnimationConfig.getMode();
        if (m == AnimationMode.EXHIBITION) {
            GL11.glTranslated(0.0D, -0.1D, 0.0D);
            transformFirstPersonItem(f / 2.0F, 0.0F);
            GL11.glTranslatef(0.1F, 0.4F, -0.1F);
            GL11.glRotated(-sine * 30.0F, sine / 2.0F, 0.0D, 9.0D);
            GL11.glRotated(-sine * 50.0F, 0.8D, sine / 2.0F, 0.0D);
        } else if (m == AnimationMode.SIGMA) {
            transformFirstPersonItem(f * 0.5F, 0.0F);
            GL11.glRotated(-sine * 27.5F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 45.0F, 1.0D, sine / 2.0F, 0.0D);
            GL11.glTranslated(-0.1D, 0.3D, 0.1D);
        } else if (m == AnimationMode.VANILLA) {
            GL11.glTranslated(0.0D, 0.05D, -0.1D);
            transformFirstPersonItem(f, swingProgress);
        } else if (m == AnimationMode.PLAIN) {
            GL11.glTranslated(0.0D, 0.05D, 0.0D);
            transformFirstPersonItem(f, 0.0F);
        } else if (m == AnimationMode.SPIN) {
            GL11.glRotated(spin, 0.0D, 0.0D, -0.1D);
            transformFirstPersonItem(f, 0.0F);
            spin = -(System.currentTimeMillis() / 2L % 360L);
        } else if (m == AnimationMode.ETB) {
            GL11.glTranslated(0.0D, -0.1D, 0.0D);
            transformFirstPersonItem(f, 0.0F);
            GL11.glTranslatef(0.1F, 0.4F, -0.1F);
            GL11.glRotated(-sine * 35.0F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 70.0F, 1.5D, -0.4D, 0.0D);
        } else if (m == AnimationMode.DORTWARE) {
            float alt = MathHelper.sin(sqrtSwing * 3.1415927F - 3.0F);
            transformFirstPersonItem(f, 0.0F);
            GL11.glRotated(-sine * 10.0F, 0.0D, 15.0D, 200.0D);
            GL11.glRotated(-sine * 10.0F, 300.0D, sine / 2.0F, 1.0D);
            GL11.glTranslated(3.4D, 0.3D, -0.4D);
            GL11.glTranslatef(-2.1F, -0.2F, 0.1F);
            GL11.glRotated(alt * 13.0F, -10.0D, -1.4D, -10.0D);
        } else if (m == AnimationMode.AVATAR) {
            GL11.glTranslatef(0.56F, -0.52F, -0.72F);
            GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(sine1 * -20.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(sine * -20.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(sine * -40.0F, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(0.4F, 0.4F, 0.4F);
        } else if (m == AnimationMode.SWONG) {
            transformFirstPersonItem(f / 2.0F, 0.0F);
            GL11.glRotated(-sine * 20.0F, sine / 2.0F, 0.0D, 9.0D);
            GL11.glRotated(-sine * 30.0F, 1.0D, sine / 2.0F, 0.0D);
        } else if (m == AnimationMode.SWANG) {
            transformFirstPersonItem(f / 2.0F, swingProgress);
            GL11.glRotated(sine * 15.0F, -sine, 0.0D, 9.0D);
            GL11.glRotated(sine * 40.0F, 1.0D, -sine / 2.0F, 0.0D);
        } else if (m == AnimationMode.SWANK) {
            transformFirstPersonItem(f / 2.0F, swingProgress);
            GL11.glRotated(sine * 30.0F, -sine, 0.0D, 9.0D);
            GL11.glRotated(sine * 40.0F, 1.0D, -sine, 0.0D);
        } else if (m == AnimationMode.STYLES) {
            transformFirstPersonItem(f, 0.0F);
            GL11.glTranslatef(-0.05F, 0.2F, 0.0F);
            GL11.glRotated(-sine * 35.0F, -8.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 70.0F, 1.0D, -0.4D, 0.0D);
        } else if (m == AnimationMode.NUDGE) {
            GL11.glTranslated(-0.1D, 0.09D, 0.0D);
            GL11.glRotated(0.0D, -320.0D, 320.0D, 0.0D);
            transformFirstPersonItem(0.0F, 1.0F);
            float ns1 = MathHelper.sin(sqrtSwing * 3.0F);
            float ns2 = MathHelper.sin(sqrtSwing * 4.9415927F);
            GL11.glRotated(-ns1 * 60.0F, -90.0D, -ns2, 10.0D);
            GL11.glRotated(-ns1 * 110.0F, 15.0D, ns2, 0.0D);
        } else if (m == AnimationMode.PUNCH) {
            transformFirstPersonItem(f, 0.0F);
            GL11.glTranslatef(0.1F, 0.2F, 0.3F);
            GL11.glRotated(-sine * 30.0F, -5.0D, 0.0D, 9.0D);
            GL11.glRotated(-sine * 10.0F, 1.0D, -0.4D, -0.5D);
        } else if (m == AnimationMode.SLIDE) {
            GL11.glTranslated(-0.1D, 0.15D, 0.0D);
            transformFirstPersonItem(0.0F, 0.0F);
            float ss = MathHelper.sin(sqrtSwing * 2.9415927F);
            GL11.glTranslatef(-0.05F, 0.0F, 0.35F);
            GL11.glRotated(-ss * 30.0F, -15.0D, ss, 10.0D);
            GL11.glRotated(-ss * 70.0D, 5.0D, -ss, 0.0D);
        } else if (m == AnimationMode.JIGSAW) {
            GL11.glTranslatef(0.56F, -0.42F, -0.72F);
            GL11.glTranslatef(0.1F * sine, 0.0F, -0.22F * sine);
            GL11.glTranslatef(0.0F, sine1 * -0.15F, 0.0F);
            GL11.glRotated(sine1 * 45.0F, 0.0D, 1.0D, 0.0D);
            GL11.glRotated(sine1 * -20.0F, 0.0D, 1.0D, 0.0D);
            GL11.glRotated(sine * -20.0F, 0.0D, 0.0D, 1.0D);
            GL11.glRotated(sine * -80.0F, 1.0D, 0.0D, 0.0D);
        }
    }

    @Inject(
        method = "renderItemInFirstPerson",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/ItemRenderer;" +
                          "renderItem(Lnet/minecraft/entity/EntityLivingBase;" +
                          "Lnet/minecraft/item/ItemStack;" +
                          "Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V",
                 shift = At.Shift.BEFORE)
    )
    public void applyScale(float partialTicks, CallbackInfo ci) {
        if (!AnimationConfig.isEnabled()) return;
        AnimationConfig.sync();
        double s = (double) AnimationConfig.getScale() / 100.0D;
        GL11.glScaled(s, s, s);
    }
}
