package keystrokesmod.mixin.impl.accessor;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(EntityPlayer.class)
public interface IAccessorEntityPlayer {
    @Accessor("itemInUseCount")
    void setItemInUseCount(int count);
}
