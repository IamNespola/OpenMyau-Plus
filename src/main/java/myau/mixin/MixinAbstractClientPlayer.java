package myau.mixin;

import myau.Myau;
import myau.module.modules.Capes;
import myau.module.modules.Sprint;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
@Mixin(value = {AbstractClientPlayer.class}, priority = 9999)
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer {
    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void getCustomCape(CallbackInfoReturnable<ResourceLocation> callbackInfo) {
        if (Myau.moduleManager == null) {
            return;
        }

        Capes capes = (Capes) Myau.moduleManager.modules.get(Capes.class);
        if (capes == null || !capes.isEnabled()) {
            return;
        }

        Entity entity = (Entity) (Object) this;
        if (!capes.allPlayer.getValue() && !(entity instanceof EntityPlayerSP)) {
            return;
        }

        ResourceLocation cape = capes.getCape();
        if (cape != null) {
            callbackInfo.setReturnValue(cape);
        }
    }

    @Redirect(
            method = {"getFovModifier"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getAttributeValue()D"
            )
    )
    private double getFovModifier(IAttributeInstance iAttributeInstance) {
        double attributeValue = iAttributeInstance.getAttributeValue();
        if ((((Entity) (Object) this)) instanceof EntityPlayerSP && Myau.moduleManager != null) {
            Sprint sprint = (Sprint) Myau.moduleManager.modules.get(Sprint.class);
            return sprint.isEnabled() && sprint.shouldApplyFovFix(iAttributeInstance) ? attributeValue * 1.300000011920929 : attributeValue;
        } else {
            return attributeValue;
        }
    }
}
