package keystrokesmod.mixin.impl.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.objectweb.asm.Opcodes;
import keystrokesmod.event.GuiUpdateEvent;
import keystrokesmod.event.PreInputEvent;
import keystrokesmod.event.PreSlotScrollEvent;
import keystrokesmod.event.SlotUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 2))
    private void onRunTick(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PreInputEvent());
    }

    @Inject(method = "displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", at = @At("HEAD"))
    public void onDisplayGuiScreen(GuiScreen guiScreen, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        GuiScreen previousGui = mc.currentScreen;
        GuiScreen setGui = guiScreen;
        boolean opened = setGui != null;
        if (!opened) {
            setGui = previousGui;
        }

        GuiUpdateEvent event = new GuiUpdateEvent(setGui, opened);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V"))
    public void changeCurrentItem(InventoryPlayer inventoryPlayer, int slot) {
        PreSlotScrollEvent event = new PreSlotScrollEvent(slot, inventoryPlayer.currentItem);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return;
        }
        inventoryPlayer.changeCurrentItem(slot);
    }

    @Redirect(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I", opcode = Opcodes.PUTFIELD))
    private void onSetCurrentItem(InventoryPlayer inventoryPlayer, int slot) {
        MinecraftForge.EVENT_BUS.post(new SlotUpdateEvent(slot));
        inventoryPlayer.currentItem = slot;
    }
}
