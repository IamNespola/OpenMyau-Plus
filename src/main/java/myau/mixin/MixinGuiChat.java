package myau.mixin;

import myau.module.modules.RenderFixes;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {GuiChat.class}, priority = 9999)
public abstract class MixinGuiChat extends GuiScreen {
    @Shadow
    protected GuiTextField inputField;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void myau$adjustChatInputWidth(CallbackInfo callbackInfo) {
        if (this.inputField != null) {
            float hotbarStartX = (this.width / 2.0f) - 91.0f;
            float maxBoxRight = hotbarStartX - 2.0f;
            int boxWidth = Math.max(160, (int) (maxBoxRight - 2.0f));
            this.inputField.width = boxWidth - 6;
        }
    }

    @Inject(method = {"drawScreen"}, at = @At("HEAD"))
    private void myau$beginModernInput(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (RenderFixes.isChatActive()) {
            RenderFixes.renderChatInputBackground(this.width, this.height);
        }
    }

    @Redirect(
            method = {"drawScreen"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiChat;drawRect(IIIII)V"
            )
    )
    private void myau$drawInputRect(int left, int top, int right, int bottom, int color) {
        if (!RenderFixes.isChatActive()) {
            float hotbarStartX = (this.width / 2.0f) - 91.0f;
            int maxRight = (int) Math.max(160, hotbarStartX - 2.0f);
            Gui.drawRect(left, top, maxRight, bottom, color);
        }
    }
}
