package myau.mixin;

import myau.module.modules.MurderDetector;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {GuiPlayerTabOverlay.class}, priority = 9999)
public abstract class MixinGuiPlayerTabOverlay {
    @Inject(
            method = {"getPlayerName"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void getPlayerName(NetworkPlayerInfo networkPlayerInfo, CallbackInfoReturnable<String> callbackInfoReturnable) {
        String murdererName = MurderDetector.getMurdererTabName(networkPlayerInfo);
        if (murdererName != null) {
            callbackInfoReturnable.setReturnValue(murdererName);
        }
    }
}
