package keystrokesmod.script.packets.serverbound;

import keystrokesmod.script.ScriptDefaults;
import keystrokesmod.script.packets.PacketMappings;
import keystrokesmod.script.packets.clientbound.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;

public class PacketHandler {
    public static CPacket convertServerBound(net.minecraft.network.Packet packet) {
        if (packet == null || packet.getClass().getSimpleName().startsWith("S")) {
            return null;
        }
        Class<? extends CPacket> asClass = PacketMappings.minecraftToScriptC.get(packet.getClass());
        CPacket newPacket;
        if (asClass != null) {
            if (packet instanceof C03PacketPlayer) {
                newPacket = new C03((C03PacketPlayer)packet, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
            }
            else if (packet instanceof C01PacketChatMessage) {
                newPacket = new C01((C01PacketChatMessage)packet, (byte) 0);
            }
            else if (packet instanceof C09PacketHeldItemChange) {
                newPacket = new C09(((C09PacketHeldItemChange)packet), true);
            }
            else {
                try {
                    newPacket = asClass.getConstructor(packet.getClass()).newInstance(packet);
                }
                catch (Exception e) {
                    newPacket = new CPacket(packet);
                }
            }
        }
        else {
            newPacket = new CPacket(packet);
        }
        return newPacket;
    }

    public static SPacket convertClientBound(Packet packet) {
        Class<? extends SPacket> asClass = PacketMappings.minecraftToScriptS.get(packet.getClass());
        SPacket newPacket;
        if (asClass != null) {
            if (packet instanceof S3APacketTabComplete) {
                newPacket = new S3A((S3APacketTabComplete) packet, (byte) 0);
            }
            else if (packet instanceof S23PacketBlockChange) {
                newPacket = new S23((S23PacketBlockChange) packet, (byte) 0);
            }
            else {
                try {
                    newPacket = asClass.getConstructor(packet.getClass()).newInstance(packet);
                }
                catch (Exception e) {
                    newPacket = new SPacket(packet);
                }
            }
        }
        else {
            newPacket = new SPacket(packet);
        }
        return newPacket;
    }

    public static Packet convertCPacket(CPacket cPacket) {
        try {
            if (cPacket instanceof C0A) {
                return new C0APacketAnimation();
            }
            else if (cPacket instanceof C0B) {
                return ((C0B) cPacket).convert();
            }
            else if (cPacket instanceof C0D) {
                return ((C0D) cPacket).convert();
            }
            else if (cPacket instanceof C09) {
                return ((C09) cPacket).convert();
            }
            else if (cPacket instanceof C0E) {
                return ((C0E) cPacket).convert();
            }
            else if (cPacket instanceof C0F) {
                return ((C0F) cPacket).convert();
            }
            else if (cPacket instanceof C08) {
                return ((C08) cPacket).convert();
            }
            else if (cPacket instanceof C07) {
                return ((C07) cPacket).convert();
            }
            else if (cPacket instanceof C01) {
                return ((C01) cPacket).convert();
            }
            else if (cPacket instanceof C02) {
                return ((C02) cPacket).convert();
            }
            else if (cPacket instanceof C03) {
                return cPacket.packet;
            }
            else if (cPacket instanceof C10) {
                return ((C10) cPacket).convert();
            }
            else if (cPacket instanceof C13) {
                return ((C13) cPacket).convert();
            }
            else if (cPacket instanceof C16) {
                return ((C16) cPacket).convert();
            }
        }
        catch (Exception e) {
            if (cPacket != null && cPacket.packet != null && !cPacket.name.startsWith("S")) {
                return cPacket.packet;
            }
            else {
                return null;
            }
        }
        if (cPacket == null && cPacket.packet == null) {
            return null;
        }
        return cPacket.packet;
    }
}