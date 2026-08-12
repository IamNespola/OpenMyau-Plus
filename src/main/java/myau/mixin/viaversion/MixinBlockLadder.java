package myau.mixin.viaversion;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.block.BlockLadder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlockLadder.class)
public abstract class MixinBlockLadder {

  @ModifyConstant(method = "setBlockBoundsBasedOnState", constant = @Constant(floatValue = 0.125F))
  private float viaversion_LadderBoundingBox(float constant) {
    if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() >= 107) {
      return 0.1875F;
    }
    return 0.125F;
  }
}
