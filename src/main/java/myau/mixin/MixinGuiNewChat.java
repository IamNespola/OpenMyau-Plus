package myau.mixin;

import myau.module.modules.RenderFixes;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep vanilla anchoring, fading, wrapping, scrolling and click hitboxes. */
@Mixin(value = GuiNewChat.class, priority = 9999)
public abstract class MixinGuiNewChat {
    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"))
    private void myau$drawChatBackground(int left, int top, int right, int bottom, int color) {
        RenderFixes.drawChatBackground(left, top, right, bottom, color);
    }
}
