// not rat bro XD
// Original logic by syuto/animations-1.6, integrated into Uzi
package myau.mixin;

import myau.config.AnimationConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@SideOnly(Side.CLIENT)
@Mixin(value = EntityLivingBase.class, priority = 999)
public abstract class MixinEntityLivingBaseAnimations {

    /**
     * @author animations-1.6 (syuto), integrated by Uzi
     * @reason Custom swing speed
     */
    @Overwrite
    private int getArmSwingAnimationEnd() {
        if (!AnimationConfig.isEnabled()) return 6;
        AnimationConfig.sync();
        int pct = Math.max(0, Math.min(AnimationConfig.getSwingSpeed(), 100));
        return (int)(6.0D + (double) pct / 100.0D * 14.0D);
    }
}
