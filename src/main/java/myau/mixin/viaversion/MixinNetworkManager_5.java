package myau.mixin.viaversion;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.MCPVLBPipeline;
import de.florianmichael.viamcp.ViaMCP;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.NetworkManager$5")
public class MixinNetworkManager_5 {

  @Inject(method = "initChannel", at = @At("TAIL"), remap = false)
  private void onInitChannel(Channel channel, CallbackInfo ci) {
    if (channel instanceof SocketChannel
        && ViaLoadingBase.getInstance().getTargetVersion().getVersion() != ViaMCP.NATIVE_VERSION) {
      UserConnection user = new UserConnectionImpl(channel, true);
      new ProtocolPipelineImpl(user);
      channel.pipeline().addLast(new MCPVLBPipeline(user));
    }
  }
}
