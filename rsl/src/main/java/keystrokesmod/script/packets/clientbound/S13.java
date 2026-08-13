package keystrokesmod.script.packets.clientbound;

import net.minecraft.network.play.server.S13PacketDestroyEntities;

public class S13 extends SPacket {
    public int[] entityIDs;

    public S13(S13PacketDestroyEntities packet) {
        super(packet);
        this.entityIDs = packet.getEntityIDs();
    }

    public S13(int... is) {
        super(new S13PacketDestroyEntities(is));
        this.entityIDs = is;
    }
}
