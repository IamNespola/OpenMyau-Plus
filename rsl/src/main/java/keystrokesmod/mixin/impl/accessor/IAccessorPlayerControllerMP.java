package keystrokesmod.mixin.impl.accessor;

import net.minecraftforge.fml.relauncher.*;
import org.spongepowered.asm.mixin.*;
import net.minecraft.client.multiplayer.*;
import org.spongepowered.asm.mixin.gen.*;

@SideOnly(Side.CLIENT)
@Mixin(PlayerControllerMP.class)
public interface IAccessorPlayerControllerMP {
    @Accessor
    float getCurBlockDamageMP();

    @Accessor
    void setCurBlockDamageMP(float damage);

    @Accessor
    int getBlockHitDelay();

    @Accessor
    void setBlockHitDelay(int delay);
}
