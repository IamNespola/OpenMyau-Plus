package keystrokesmod.mixin.impl.accessor;

import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderer.class)
public interface IAccessorItemRenderer {
    @Accessor("equippedProgress")
    float getEquippedProgress();
}
