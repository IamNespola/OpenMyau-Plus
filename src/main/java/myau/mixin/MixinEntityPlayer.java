package myau.mixin;

import myau.Myau;
import myau.module.modules.Capes;
import myau.module.modules.KeepSprint;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {EntityPlayer.class}, priority = 9999)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {
    @Inject(method = {"isWearing"}, at = @At("HEAD"), cancellable = true)
    private void makeCapeVisible(EnumPlayerModelParts part, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (part != EnumPlayerModelParts.CAPE || Myau.moduleManager == null) {
            return;
        }

        Capes capes = (Capes) Myau.moduleManager.modules.get(Capes.class);
        if (capes == null || !capes.isEnabled() || capes.getCape() == null) {
            return;
        }

        EntityPlayer player = (EntityPlayer) (Object) this;
        if (capes.allPlayer.getValue() || player instanceof EntityPlayerSP) {
            callbackInfo.setReturnValue(true);
        }
    }

    @ModifyConstant(
            method = {"attackTargetEntityWithCurrentItem"},
            constant = {@Constant(
                    doubleValue = 0.6
            )}
    )
    private double attackTargetEntityWithCurrentItem(double speed) {
        if (Myau.moduleManager == null) {
            return speed;
        } else {
            KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
            return keepSprint.isEnabled() && keepSprint.shouldKeepSprint()
                    ? speed + (1.0 - speed) * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0)
                    : speed;
        }
    }

    @Redirect(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"
            )
    )
    private void setSprinnt(EntityPlayer entityPlayer, boolean boolean2) {
        if (Myau.moduleManager != null) {
            KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
            if (!keepSprint.isEnabled() || !keepSprint.shouldKeepSprint()) {
                entityPlayer.setSprinting(boolean2);
            }
        }
    }
}
