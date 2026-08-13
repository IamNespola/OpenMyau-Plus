package keystrokesmod.script.packets.serverbound;

import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C16PacketClientStatus;

public class C16 extends CPacket {
    public String status;

    public C16(String status) {
        super(null);
        this.status = status;
    }

    public C16(C16PacketClientStatus packet) {
        super(packet);
        this.status = packet.getStatus().name();
    }

    @Override
    public C16PacketClientStatus convert() {
        return new C16PacketClientStatus(Utils.getEnum(C16PacketClientStatus.EnumState.class, this.status));
    }
}
