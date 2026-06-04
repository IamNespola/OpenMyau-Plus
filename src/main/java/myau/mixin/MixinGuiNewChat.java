package myau.mixin;

import myau.module.modules.RenderFixes;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(value = {GuiNewChat.class}, priority = 9999)
public abstract class MixinGuiNewChat {
    @Shadow
    @Final
    private List<ChatLine> drawnChatLines;

    @Shadow
    private int scrollPos;

    @Shadow
    private boolean isScrolled;

    @Shadow
    public abstract void deleteChatLine(int id);

    @Shadow
    public abstract void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId);

    private static final int MYAU_DEDUP_START_ID = 900000;
    private static int myau$nextDedupId = MYAU_DEDUP_START_ID;
    private static String myau$lastMessage;
    private static int myau$lastMessageCount;
    private static int myau$lastMessageId;
    private static boolean myau$updatingDuplicate;

    @Inject(method = {"printChatMessageWithOptionalDeletion"}, at = @At("HEAD"), cancellable = true)
    private void myau$collapseDuplicateChat(IChatComponent chatComponent, int chatLineId, CallbackInfo callbackInfo) {
        if (myau$updatingDuplicate || chatComponent == null) {
            return;
        }

        String formatted = chatComponent.getFormattedText();
        if (formatted == null || formatted.isEmpty()) {
            return;
        }

        callbackInfo.cancel();
        if (formatted.equals(myau$lastMessage) && myau$lastMessageId != 0) {
            myau$lastMessageCount++;
            this.deleteChatLine(myau$lastMessageId);
            myau$printDedupedMessage(formatted + " §7[ x" + myau$lastMessageCount + " ]", myau$lastMessageId);
            return;
        }

        myau$lastMessage = formatted;
        myau$lastMessageCount = 1;
        myau$lastMessageId = chatLineId != 0 ? chatLineId : myau$nextDedupId++;
        if (myau$nextDedupId > MYAU_DEDUP_START_ID + 100000) {
            myau$nextDedupId = MYAU_DEDUP_START_ID;
        }
        myau$printDedupedMessage(formatted, myau$lastMessageId);
    }

    private void myau$printDedupedMessage(String formatted, int id) {
        myau$updatingDuplicate = true;
        try {
            this.printChatMessageWithOptionalDeletion(new ChatComponentText(formatted), id);
        } finally {
            myau$updatingDuplicate = false;
        }
    }

    @Inject(method = {"drawChat"}, at = @At("HEAD"), cancellable = true)
    private void myau$renderModernChat(int updateCounter, CallbackInfo callbackInfo) {
        if (RenderFixes.renderChat((GuiNewChat) (Object) this, updateCounter, this.drawnChatLines, this.scrollPos, this.isScrolled)) {
            callbackInfo.cancel();
        }
    }

    @ModifyVariable(method = {"getChatComponent"}, at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int myau$adjustChatComponentMouseX(int mouseX) {
        return RenderFixes.adjustChatMouseX(mouseX);
    }

    @ModifyVariable(method = {"getChatComponent"}, at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int myau$adjustChatComponentMouseY(int mouseY) {
        return RenderFixes.adjustChatMouseY(mouseY);
    }
}
