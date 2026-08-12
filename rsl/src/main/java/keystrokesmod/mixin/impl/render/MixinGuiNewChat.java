package keystrokesmod.mixin.impl.render;

import keystrokesmod.Raven;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Inject(method = "printChatMessage", at = @At("HEAD"), cancellable = true)
    public void printChatMessage(IChatComponent chatComponent, CallbackInfo ci) {
        if (Utils.cancelChat) {
            Utils.cancelledMsgs.add(Utils.stripColor(chatComponent.getUnformattedText()));
            ci.cancel();
        }
    }

}
