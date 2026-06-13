// not rat bro XD
// Original logic by syuto/animations-1.6, integrated into Uzi
package myau.mixin;

import myau.config.AnimationConfig;
import myau.config.AnimationMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
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
    private float delay = 0.0F;
    private long lastUpdateTime = System.currentTimeMillis();

    @Shadow @Final private Minecraft mc;

    @Shadow
    private ItemStack itemToRender;

    @Shadow
    protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Shadow
    protected abstract void doBlockTransformations();

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

        GL11.glTranslated(
                AnimationConfig.getBlockPosX(),
                AnimationConfig.getBlockPosY(),
                AnimationConfig.getBlockPosZ()
        );

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
        } else if (m == AnimationMode.SWING) {
            transformFirstPersonItem(f, 0.0F);
            doBlockTransformations();
        } else if (m == AnimationMode.OLD) {
            transformFirstPersonItem(f, 0.0F);
            applyOldAnimation();
        } else if (m == AnimationMode.PUSH) {
            transformFirstPersonItem(f, 0.0F);
            applyPushAnimation(swingProgress);
        } else if (m == AnimationMode.DASH) {
            transformFirstPersonItem(f, 0.0F);
            applyDashAnimation(swingProgress);
        } else if (m == AnimationMode.SLASH) {
            transformFirstPersonItem(f, 0.0F);
            applySlashAnimation(swingProgress);
        } else if (m == AnimationMode.SCALE) {
            transformFirstPersonItem(f, 0.0F);
            applyScaleAnimation(swingProgress);
        } else if (m == AnimationMode.SWONK) {
            transformFirstPersonItem(f, 0.0F);
            applySwonkAnimation(swingProgress);
        } else if (m == AnimationMode.STELLA) {
            transformFirstPersonItem(f, 0.0F);
            applyStellaAnimation();
        } else if (m == AnimationMode.SMALL) {
            transformFirstPersonItem(f, 0.0F);
            applySmallAnimation();
        } else if (m == AnimationMode.EDIT) {
            transformFirstPersonItem(f, 0.0F);
            applyEditAnimation(swingProgress);
        } else if (m == AnimationMode.RHYS) {
            transformFirstPersonItem(f, 0.0F);
            applyRhysAnimation(swingProgress);
        } else if (m == AnimationMode.STAB) {
            transformFirstPersonItem(f, 0.0F);
            applyStabAnimation(swingProgress);
        } else if (m == AnimationMode.FLOAT) {
            transformFirstPersonItem(f, 0.0F);
            applyFloatAnimation(swingProgress);
        } else if (m == AnimationMode.REMIX) {
            transformFirstPersonItem(f, 0.0F);
            applyRemixAnimation(swingProgress);
        } else if (m == AnimationMode.XIV) {
            transformFirstPersonItem(f, 0.0F);
            applyXivAnimation(swingProgress);
        } else if (m == AnimationMode.WINTER) {
            transformFirstPersonItem(f, 0.0F);
            applyWinterAnimation();
        } else if (m == AnimationMode.YAMATO) {
            transformFirstPersonItem(f, 0.0F);
            applyYamatoAnimation(swingProgress);
        } else if (m == AnimationMode.SLIDE_SWING) {
            transformFirstPersonItem(f, 0.0F);
            applySlideSwingAnimation(swingProgress);
        } else if (m == AnimationMode.SMALL_PUSH) {
            transformFirstPersonItem(f, 0.0F);
            applySmallPushAnimation(swingProgress);
        } else if (m == AnimationMode.REVERSE) {
            transformFirstPersonItem(f, 0.0F);
            applyReverseAnimation();
        } else if (m == AnimationMode.INVENT) {
            transformFirstPersonItem(f, 0.0F);
            applyInventAnimation(swingProgress);
        } else if (m == AnimationMode.LEAKED) {
            transformFirstPersonItem(f, 0.0F);
            applyLeakedAnimation(swingProgress);
        } else if (m == AnimationMode.AQUA) {
            transformFirstPersonItem(f, 0.0F);
            applyAquaAnimation(swingProgress);
        } else if (m == AnimationMode.ASTRO) {
            transformFirstPersonItem(f, 0.0F);
            applyAstroAnimation(swingProgress);
        } else if (m == AnimationMode.FADEAWAY) {
            transformFirstPersonItem(f, 0.0F);
            applyFadeawayAnimation(swingProgress);
        } else if (m == AnimationMode.ASTOLFO) {
            transformFirstPersonItem(f, 0.0F);
            applyAstolfoAnimation(swingProgress);
        } else if (m == AnimationMode.ASTOLFO_SPIN) {
            transformFirstPersonItem(f, 0.0F);
            applyAstolfoSpinAnimation();
        } else if (m == AnimationMode.MOON) {
            transformFirstPersonItem(f, 0.0F);
            applyMoonAnimation(swingProgress);
        } else if (m == AnimationMode.MOON_PUSH) {
            transformFirstPersonItem(f, 0.0F);
            applyMoonPushAnimation(swingProgress);
        } else if (m == AnimationMode.SMOOTH) {
            transformFirstPersonItem(f, 0.0F);
            applySmoothAnimation(swingProgress);
        } else if (m == AnimationMode.TAP1) {
            transformFirstPersonItem(f, 0.0F);
            applyTap1Animation(swingProgress);
        } else if (m == AnimationMode.TAP2) {
            transformFirstPersonItem(f, 0.0F);
            applyTap2Animation(swingProgress);
        } else if (m == AnimationMode.SIGMA3) {
            transformFirstPersonItem(f, 0.0F);
            applySigma3Animation(swingProgress);
        } else if (m == AnimationMode.SIGMA4) {
            transformFirstPersonItem(f, 0.0F);
            applySigma4Animation(swingProgress);
        } else if (m == AnimationMode.MYAU_1_8) {
            transformFirstPersonItem(f, 0.0F);
        } else if (m == AnimationMode.MYAU_SLIDE) {
            transformFirstPersonItem(f, 0.0F);
            applyMyauSlideAnimation(swingProgress);
        } else if (m == AnimationMode.MYAU_SWANK) {
            transformFirstPersonItem(f, 0.0F);
            applyMyauSwankAnimation(swingProgress);
        } else if (m == AnimationMode.MYAU_SWANG) {
            transformFirstPersonItem(f, 0.0F);
            applyMyauSwangAnimation(swingProgress);
        } else if (m == AnimationMode.MYAU_AVATAR) {
            transformFirstPersonItem(f, 0.0F);
            applyMyauAvatarAnimation(swingProgress);
        } else if (m == AnimationMode.MYAU_JIGSAW) {
            transformFirstPersonItem(f, 0.0F);
            applyMyauJigsawAnimation();
        }
    }

    private void applyOldAnimation() {
        GL11.glTranslated(0.08D, -0.14D, -0.05D);
        GlStateManager.translate(-0.35F, 0.2F, 0.0F);
        doBlockTransformations();
    }

    private void applyPushAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 20.0F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 30.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyDashAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotated(-swing * 22.0F, swing / 2.0F, 0.0F, 9.0F);
        GL11.glRotated(-swing * 50.0F, 0.8F, swing / 2.0F, 0.0F);
    }

    private void applySlashAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, 0.08D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 70.0F, 5.0F, 13.0F, 50.0F);
    }

    private void applyScaleAnimation(float swingProgress) {
        GL11.glTranslated(0.84D, -0.77D, -1.1D);
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(sine * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(sine * -27.0F, 0.0F, 0.0F, 0.0F);
    }

    private void applyMyauSlideAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, -0.11D, -0.07D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.4F, 0.28F, 0.0F);
        GlStateManager.rotate(-swing * 35.0F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 70.0F, 1.0F, -0.4F, -0.0F);
    }

    private void applyMyauSwankAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotatef(swing * 15.0F, -swing, -0.0F, 9.0F);
        GL11.glRotatef(swing * 40.0F, 1.0F, -swing / 2.0F, -0.0F);
    }

    private void applyMyauSwangAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.03D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 37.0F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 52.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyMyauAvatarAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(sine * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(sine * -40.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        doBlockTransformations();
    }

    private void applyMyauJigsawAnimation() {
        GL11.glTranslated(0.0D, -0.18D, -0.1D);
        GlStateManager.translate(-0.5D, 0.0D, 0.0D);
        doBlockTransformations();
    }

    private void applySwonkAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.03D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotated(-swing * -15.0F, swing / 2.0F, 1.0F, 4.0F);
        GL11.glRotated(-swing * 7.5F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyStellaAnimation() {
        GlStateManager.translate(-0.5F, 0.3F, -0.2F);
        GlStateManager.rotate(32.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-70.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(40.0F, 0.0F, 1.0F, 0.0F);
        doBlockTransformations();
    }

    private void applySmallAnimation() {
        GL11.glTranslated(-0.01D, 0.03D, -0.24D);
        doBlockTransformations();
    }

    private void applyEditAnimation(float swingProgress) {
        GL11.glTranslated(-0.04D, 0.06D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 8.0F, -swing, -0.0F, 2.0F);
        GlStateManager.rotate(swing * 22.0F, 1.0F, -swing / 3.0F, -0.0F);
    }

    private void applyRhysAnimation(float swingProgress) {
        GL11.glTranslated(0.0D, 0.19D, 0.0D);
        GlStateManager.translate(0.41F, -0.25F, -0.5555557F);
        GlStateManager.rotate(35.0F, 0.0F, 1.5F, 0.0F);
        float slowSwing = MathHelper.sin(swingProgress * swingProgress / 64.0F * 3.1415927F);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(slowSwing * -5.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(swing * -12.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(swing * -65.0F, 1.0F, 0.0F, 0.0F);
    }

    private void applyStabAnimation(float swingProgress) {
        GL11.glTranslated(-0.25D, 0.45D, 0.8D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(0.6F, 0.3F, -0.6F + -swing * 0.7F);
        GlStateManager.rotate(6090.0F, 0.0F, 0.0F, 0.1F);
        GlStateManager.rotate(6085.0F, 0.0F, 0.1F, 0.0F);
        GlStateManager.rotate(6110.0F, 0.1F, 0.0F, 0.0F);
    }

    private void applyFloatAnimation(float swingProgress) {
        float swing = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-swing * 20.0F, swing / 2.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 30.0F, 1.0F, swing / 2.0F, -0.0F);
    }

    private void applyRemixAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(0.0F, -2.0F, 0.0F, 10.0F);
        GlStateManager.rotate(-swing * 25.0F, 0.5F, 0.0F, 1.0F);
    }

    private void applyXivAnimation(float swingProgress) {
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-sine1 * 20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-sine * 20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-sine * 80.0F, 1.0F, 0.0F, 0.0F);
    }

    private void applyWinterAnimation() {
        GL11.glTranslated(0.0D, -0.16D, 0.0D);
        GL11.glTranslatef(-0.35F, 0.1F, 0.0F);
        GL11.glTranslatef(-0.05F, -0.1F, 0.1F);
        doBlockTransformations();
    }

    private void applyYamatoAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GL11.glRotatef(-swing * 100.0F, -9.0F, 5.0F, 9.0F);
    }

    private void applySlideSwingAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        doBlockTransformations();
    }

    private void applySmallPushAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        float sine = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(sine1 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(sine * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(sine * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        doBlockTransformations();
    }

    private void applyReverseAnimation() {
        GL11.glTranslated(0.0D, 0.1D, -0.12D);
        GL11.glTranslated(0.08D, -0.1D, -0.3D);
        doBlockTransformations();
    }

    private void applyInventAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 30.0F, -8.0F, -0.2F, 9.0F);
    }

    private void applyLeakedAnimation(float swingProgress) {
        GL11.glTranslated(0.08D, 0.02D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 41.0F, 1.1F, 0.8F, -0.3F);
    }

    private void applyAquaAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 8.5F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 6.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyAstroAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 50.0F / 9.0F, -swing, -0.0F, 90.0F);
        GlStateManager.rotate(swing * 50.0F, 200.0F, -swing / 2.0F, -0.0F);
    }

    private void applyFadeawayAnimation(float swingProgress) {
        float sine1 = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        GlStateManager.rotate(-sine1 * 45.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.5F, 0.0F, 0.0F);
    }

    private void applyAstolfoAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 29.0F, swing / 2.0F, 1.0F, 0.5F);
        GlStateManager.rotate(-swing * 43.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyAstolfoSpinAnimation() {
        GlStateManager.rotate(this.delay, 0.0F, 0.0F, -0.1F);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastUpdateTime;
        this.delay += elapsedTime * 360.0F / 850.0F;
        lastUpdateTime = currentTime;
        if (this.delay > 360.0F) {
            this.delay = 0.0F;
        }
        doBlockTransformations();
    }

    private void applyMoonAnimation(float swingProgress) {
        GL11.glTranslated(-0.08D, 0.12D, 0.0D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 32.5F, swing / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-swing * 60.0F, 1.0F, swing / 3.0F, -0.0F);
    }

    private void applyMoonPushAnimation(float swingProgress) {
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.2F, 0.45F, 0.25F);
        GlStateManager.rotate(-swing * 20.0F, -5.0F, -5.0F, 9.0F);
    }

    private void applySmoothAnimation(float swingProgress) {
        GL11.glTranslated(0.14D, -0.1D, -0.24D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.translate(-0.36F, 0.25F, -0.06F);
        GlStateManager.rotate(-swing * 35.0F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 70.0F, 1.0F, 0.4F, -0.0F);
    }

    private void applyTap1Animation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((swingProgress * 0.8F - swingProgress * swingProgress * 0.8F) * -90.0F,
                0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.37F, 0.37F, 0.37F);
    }

    private void applyTap2Animation(float swingProgress) {
        GL11.glTranslated(0.0D, -0.1D, 0.0D);
        GlStateManager.translate(0.56F, -0.42F, -0.71999997F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F) * -30.0F,
                0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private void applySigma3Animation(float swingProgress) {
        GL11.glTranslated(0.02D, 0.02D, 0.0D);
        GL11.glTranslated(0.4D, -0.06D, -0.46D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(swing * 12.5F, -swing, -0.0F, 9.0F);
        GlStateManager.rotate(swing * 15.0F, 1.0F, -swing / 2.0F, -0.0F);
    }

    private void applySigma4Animation(float swingProgress) {
        GL11.glTranslated(-0.6D, 0.2D, 0.11D);
        float swing = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        GlStateManager.rotate(-swing * 27.5F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-swing * 45.0F, 1.0F, swing / 2.0F, 0.0F);
        doBlockTransformations();
        GL11.glTranslated(-0.08D, -1.25D, 1.25D);
    }

    @Redirect(
            method = "renderItemInFirstPerson",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V",
                    ordinal = 4)
    )
    private void skipNormalTransform(ItemRenderer instance, float f1, float f2) {
        if (!AnimationConfig.isEnabled()) {
            transformFirstPersonItem(f1, f2);
            return;
        }

        AnimationConfig.sync();
        if (!shouldApplyNormalAnimations()) {
            transformFirstPersonItem(f1, f2);
        }
    }

    private boolean shouldApplyNormalAnimations() {
        return AnimationConfig.getRenderMode() == 1
                && mc.thePlayer != null
                && this.itemToRender != null
                && this.itemToRender.getItem() instanceof ItemSword
                && mc.thePlayer.getItemInUseCount() <= 0
                && !mc.thePlayer.isBlocking();
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
        if (shouldApplyNormalAnimations()) {
            applyAnimTransform(partialTicks, ci);
        }
        double s = (double) AnimationConfig.getScale() / 100.0D * (1.0D + AnimationConfig.getItemSize());
        GL11.glScaled(s, s, s);
    }
}
