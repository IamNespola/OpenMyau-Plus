package keystrokesmod.mixin.impl.invoker;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NetworkManager.class)
public interface IInvokerNetworkManager {

    @Invoker("channelRead0")
    void rvn$channelRead0(ChannelHandlerContext p_channelRead0_1_, Packet p_channelRead0_2_);

}