package keystrokesmod.mixin.impl.render;

import keystrokesmod.Raven;
import keystrokesmod.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen
{
    @Shadow
    public Minecraft mc;

    @Inject(method = "sendChatMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = true)
    private void messageSend(String msg, boolean addToChat, final CallbackInfo callbackInfo) {
        if (msg.startsWith(".") && addToChat && ModuleManager.canExecuteChatCommand()) {
            this.mc.ingameGUI.getChatGUI().addToSentMessages(msg);

            Raven.commandManager.executeCommand(msg);
            callbackInfo.cancel();
        }
    }
}
