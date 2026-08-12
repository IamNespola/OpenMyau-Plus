package keystrokesmod.mixin.impl.accessor;

import net.minecraft.item.ItemFood;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(ItemFood.class)
public interface IAccessorItemFood {
    @Accessor("alwaysEdible")
    boolean getAlwaysEdible();
}
