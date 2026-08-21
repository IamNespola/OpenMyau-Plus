package keystrokesmod.script.packets.serverbound;

import net.minecraft.network.Packet;

import java.util.Objects;

public class CPacket {
    public String name;
    public net.minecraft.network.Packet packet;

    public CPacket(net.minecraft.network.Packet packet) {
        if (packet == null) {
            return;
        }
        this.packet = packet;
        this.name = packet.getClass().getSimpleName();
    }

    public Packet convert() {
        return packet;
    }

    @Override
    public int hashCode() {
        return packet.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CPacket) {
            return Objects.equals(((CPacket) obj).packet, packet);
        }
        if (obj instanceof net.minecraft.network.Packet) {
            return Objects.equals(packet, obj);
        }
        return false;
    }
}
