package keystrokesmod.script.packets.clientbound;

import java.util.Objects;

public class SPacket {
    public String name;
    public net.minecraft.network.Packet packet;

    public SPacket(net.minecraft.network.Packet packet) {
        this.packet = packet;
        this.name = packet.getClass().getSimpleName();
    }

    @Override
    public int hashCode() {
        return packet.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SPacket) {
            return Objects.equals(((SPacket) obj).packet, packet);
        }
        if (obj instanceof net.minecraft.network.Packet) {
            return Objects.equals(packet, obj);
        }
        return false;
    }
}
