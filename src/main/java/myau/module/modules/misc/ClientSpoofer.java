package myau.module.modules.misc;

import myau.event.EventTarget;
import myau.events.PacketEvent;
import myau.mixin.IAccessorC17PacketCustomPayload;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.TextProperty;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import io.netty.buffer.Unpooled;

public class ClientSpoofer extends Module {
    public final TextProperty brand = new TextProperty("brand", "vanilla");

    public ClientSpoofer() {
        super("ClientSpoofer",Category.MISC,false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled())
            return;

        if (event.getPacket() instanceof C17PacketCustomPayload) {
            C17PacketCustomPayload packet = (C17PacketCustomPayload) event.getPacket();
            if (packet.getChannelName().equals("MC|Brand")) {
                IAccessorC17PacketCustomPayload accessor = (IAccessorC17PacketCustomPayload) packet;
                accessor.setData(new PacketBuffer(Unpooled.buffer()).writeString(brand.getValue()));
            }
        }
    }
}
