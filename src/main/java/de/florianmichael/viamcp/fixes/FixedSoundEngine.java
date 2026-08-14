/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viamcp.fixes;

import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.ViaMCP;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class FixedSoundEngine {
  private static final Minecraft mc = Minecraft.getMinecraft();

  /** Fix for block breaking sounds on protocols above 1.8.x */
  public static boolean destroyBlock(World world, BlockPos pos, boolean dropBlock) {
    IBlockState iblockstate = world.getBlockState(pos);
    Block block = iblockstate.getBlock();

    world.playAuxSFX(2001, pos, Block.getStateId(iblockstate));

    if (block.getMaterial() == Material.air) {
      return false;
    } else {
      if (dropBlock) {
        block.dropBlockAsItem(world, pos, iblockstate, 0);
      }

      return world.setBlockState(pos, Blocks.air.getDefaultState(), 3);
    }
  }

  /** Fix for block placing sounds on protocols above 1.8.x */
  public static boolean onItemUse(
      ItemBlock iblock,
      ItemStack stack,
      EntityPlayer playerIn,
      World worldIn,
      BlockPos pos,
      EnumFacing side,
      float hitX,
      float hitY,
      float hitZ) {
    IBlockState iblockstate = worldIn.getBlockState(pos);
    Block block = iblockstate.getBlock();

    if (!block.isReplaceable(worldIn, pos)) {
      pos = pos.offset(side);
    }

    if (stack.stackSize == 0) {
      return false;
    } else if (!playerIn.canPlayerEdit(pos, side, stack)) {
      return false;
    } else if (worldIn.canBlockBePlaced(
        iblock.getBlock(), pos, false, side, (Entity) null, stack)) {
      int i = iblock.getMetadata(stack.getMetadata());
      IBlockState iblockstate1 =
          iblock.getBlock().onBlockPlaced(worldIn, pos, side, hitX, hitY, hitZ, i, playerIn);

      if (worldIn.setBlockState(pos, iblockstate1, 3)) {
        iblockstate1 = worldIn.getBlockState(pos);

        if (iblockstate1.getBlock() == iblock.getBlock()) {
          ItemBlock.setTileEntityNBT(worldIn, playerIn, pos, stack);
          iblock.getBlock().onBlockPlacedBy(worldIn, pos, iblockstate1, playerIn, stack);
        }

        if (ViaLoadingBase.getInstance().getTargetVersion().getOriginalVersion()
            != ViaMCP.NATIVE_VERSION) {

          mc.theWorld.playSoundAtPos(
              pos.add(0.5, 0.5, 0.5),
              iblock.getBlock().stepSound.getPlaceSound(),
              (iblock.getBlock().stepSound.getVolume() + 1.0F) / 2.0F,
              iblock.getBlock().stepSound.getFrequency() * 0.8F,
              false);
        } else {
          worldIn.playSoundEffect(
              (double) ((float) pos.getX() + 0.5F),
              (double) ((float) pos.getY() + 0.5F),
              (double) ((float) pos.getZ() + 0.5F),
              iblock.getBlock().stepSound.getPlaceSound(),
              (iblock.getBlock().stepSound.getVolume() + 1.0F) / 2.0F,
              iblock.getBlock().stepSound.getFrequency() * 0.8F);
        }

        --stack.stackSize;
      }

      return true;
    } else {
      return false;
    }
  }
}
