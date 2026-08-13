package keystrokesmod.script.packets.serverbound;

import net.minecraft.network.play.client.C09PacketHeldItemChange;

public class C09 extends CPacket {
    public int slot;

    public C09(int slot) {
        super(null);
        this.slot = slot;
    }

    public C09(C09PacketHeldItemChange packet, boolean identifier) {
        super(packet);
        this.slot = packet.getSlotId();
    }

    @Override
    public C09PacketHeldItemChange convert() {
        return new C09PacketHeldItemChange(this.slot);
    }
}
