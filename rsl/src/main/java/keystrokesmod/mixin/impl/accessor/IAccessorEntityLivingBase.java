package keystrokesmod.mixin.impl.accessor;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(EntityLivingBase.class)
public interface IAccessorEntityLivingBase {
    @Accessor("jumpTicks")
    void setJumpTicks(int ticks);
}
