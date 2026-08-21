package keystrokesmod.mixin.impl.network;

import io.netty.channel.ChannelHandlerContext;
import keystrokesmod.event.NoEventPacketEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.utility.PacketUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void sendPacket(Packet p_sendPacket_1_, CallbackInfo ci) {
        if (PacketUtils.skipSendEvent.contains(p_sendPacket_1_)) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new NoEventPacketEvent(p_sendPacket_1_));
            PacketUtils.skipSendEvent.remove(p_sendPacket_1_);
            return;
        }

        SendPacketEvent sendPacketEvent = new SendPacketEvent(p_sendPacket_1_);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(sendPacketEvent);

        if (sendPacketEvent.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void receivePacket(ChannelHandlerContext p_channelRead0_1_, Packet p_channelRead0_2_, CallbackInfo ci) {
        if (PacketUtils.skipReceiveEvent.contains(p_channelRead0_2_)) {
            PacketUtils.skipReceiveEvent.remove(p_channelRead0_2_);
            return;
        }
        ReceivePacketEvent receivePacketEvent = new ReceivePacketEvent(p_channelRead0_2_);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(receivePacketEvent);

        if (receivePacketEvent.isCanceled()) {
            ci.cancel();
        }
    }
}
