package keystrokesmod.utility;

import keystrokesmod.Raven;
import net.minecraft.network.Packet;

import java.util.ArrayList;
import java.util.List;

public class PacketUtils {
    public static List<Packet> skipSendEvent = new ArrayList<>();
    public static List<Packet> skipReceiveEvent = new ArrayList<>();

    public static void sendPacketNoEvent(Packet packet) {
        if (packet == null || packet.getClass().getSimpleName().startsWith("S")) {
            return;
        }
        skipSendEvent.add(packet);
        Raven.mc.thePlayer.sendQueue.addToSendQueue(packet);
    }

    public static void receivePacketNoEvent(Packet packet) {
        try {
            skipReceiveEvent.add(packet);
            packet.processPacket(Raven.mc.getNetHandler());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
