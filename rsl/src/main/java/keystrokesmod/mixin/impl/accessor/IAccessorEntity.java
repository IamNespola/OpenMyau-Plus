package keystrokesmod.mixin.impl.accessor;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IAccessorEntity {
    @Accessor("fire")
    int getFire();

    @Accessor("nextStepDistance")
    int getNextStepDistance();

    @Accessor("isInWeb")
    boolean getIsInWeb();
}
