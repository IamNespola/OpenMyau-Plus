package keystrokesmod.mixin.impl.client;

import keystrokesmod.event.UseItemEvent;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {
    @Inject(method = "sendUseItem(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"))
    public void injectUseItemEvent(EntityPlayer p_sendUseItem_1_, World p_sendUseItem_2_, ItemStack p_sendUseItem_3_, CallbackInfoReturnable<Boolean> ci) {
        MinecraftForge.EVENT_BUS.post(new UseItemEvent(p_sendUseItem_3_));
    }
}
