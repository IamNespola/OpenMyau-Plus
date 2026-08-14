package myau.mixin.viaversion;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockLilyPad.class)
public abstract class MixinBlockLilyPad extends BlockBush {

  /**
   * @author FlorianMichael
   * @reason 1.9+ lily pad collision shape
   */
  @Overwrite
  public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
    if (ViaLoadingBase.getInstance().getTargetVersion().getVersion() >= 107) {
      return new AxisAlignedBB(
          pos.getX() + 0.0625D,
          pos.getY(),
          pos.getZ() + 0.0625D,
          pos.getX() + 0.9375D,
          pos.getY() + 0.09375D,
          pos.getZ() + 0.9375D);
    }
    return new AxisAlignedBB(
        pos.getX(),
        pos.getY(),
        pos.getZ(),
        pos.getX() + 1.0D,
        pos.getY() + 0.015625D,
        pos.getZ() + 1.0D);
  }
}
