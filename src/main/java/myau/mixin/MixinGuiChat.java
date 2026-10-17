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
            Gui.drawRect(left, top, right, bottom, color);
        }
    }
}
