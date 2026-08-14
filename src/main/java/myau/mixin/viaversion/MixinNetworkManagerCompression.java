package myau.mixin.viaversion;

import de.florianmichael.vialoadingbase.netty.event.CompressionReorderEvent;
import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManagerCompression {

  @Shadow private Channel channel;

  @Inject(method = "setCompressionTreshold", at = @At("RETURN"))
  private void reorderPipeline(int threshold, CallbackInfo ci) {
    this.channel.pipeline().fireUserEventTriggered(new CompressionReorderEvent());
  }
}
