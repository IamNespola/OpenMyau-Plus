package myau.mixin;

import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderChunk.class)
public class MixinRenderChunk {

    @Shadow
    private boolean needsUpdate;

    /**
     * Fix for chunk loading lag on some graphics cards (e.g. Intel HD).        
     * Prevents redundant chunk updates that cause massive FPS drops.
     */
    @Inject(method = "setNeedsUpdate", at = @At("HEAD"), cancellable = true)
    public void onSetNeedsUpdate(boolean immediate, CallbackInfo ci) {
        if (this.needsUpdate && !immediate) {
            ci.cancel();
        }
    }
}
