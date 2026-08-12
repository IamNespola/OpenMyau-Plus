package keystrokesmod.script.packets.clientbound;

import net.minecraft.network.play.server.S3APacketTabComplete;

public class S3A extends SPacket {
    public String[] matches;

    public S3A(S3APacketTabComplete packet, byte f) {
        super(packet);
        this.matches = packet.func_149630_c();
    }

    public S3A(String[] matches) {
        super(new S3APacketTabComplete(matches));
        this.matches = matches;
    }
}
