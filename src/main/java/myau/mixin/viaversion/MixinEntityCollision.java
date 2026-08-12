package myau.mixin.viaversion;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntityCollision {

  @Inject(method = "getCollisionBorderSize", at = @At("HEAD"), cancellable = true)
  private void viaversion_collisionBorder(CallbackInfoReturnable<Float> cir) {
    if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() >= 107) {
      cir.setReturnValue(0.0F);
    }
  }
}
