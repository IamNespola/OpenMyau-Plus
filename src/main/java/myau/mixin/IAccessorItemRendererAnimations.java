package myau.mixin;

import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface for ItemRenderer fields
 * Used by MixinItemRendererAnimations to access private fields
 */
@Mixin(ItemRenderer.class)
public interface IAccessorItemRendererAnimations {

    @Accessor("equippedProgress")
    float getEquippedProgress();

    @Accessor("prevEquippedProgress")
    float getPrevEquippedProgress();

    @Accessor("equippedProgress")
    void setEquippedProgress(float progress);

    @Accessor("prevEquippedProgress")
    void setPrevEquippedProgress(float progress);
}
