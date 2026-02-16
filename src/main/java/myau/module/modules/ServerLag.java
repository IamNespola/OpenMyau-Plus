package myau.module.modules;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S40PacketDisconnect;

public class ServerLag extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final ConcurrentLinkedQueue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final IntProperty maxBlinkTime = new IntProperty("Lag ms", 1000, 500, 30000);
    private int currentLatency = 0;

    public ServerLag() {
        super("ServerLag", false);
    }

    @Override
    public void onEnabled() {
        currentLatency = maxBlinkTime.getValue();
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE || event.isCancelled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (currentLatency == 0) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof S19PacketEntityStatus || packet instanceof S02PacketChat || packet instanceof S0BPacketAnimation || packet instanceof S06PacketUpdateHealth) return;
        if (packet instanceof S08PacketPlayerPosLook || packet instanceof S40PacketDisconnect) {
            this.releaseAllPackets();
            return;
        }

        @SuppressWarnings("unchecked")
        Packet<INetHandlerPlayClient> playPacket = (Packet<INetHandlerPlayClient>) packet;
        packetQueue.add(new TimedPacket(playPacket, System.currentTimeMillis()));
        event.setCancelled(true);
        while (!packetQueue.isEmpty()) {
            TimedPacket first = packetQueue.peek();
            if (first != null && (System.currentTimeMillis() - first.time >= currentLatency)) {
                packetQueue.poll();
                try {
                    first.packet.processPacket(mc.getNetHandler());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (packetQueue.isEmpty()) {
                    currentLatency = 0;
                }
            } else {
                break;
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.releaseAllPackets();
    }

    private void releaseAllPackets() {
        if (!packetQueue.isEmpty()) {
            for (TimedPacket timedPacket : packetQueue) {
                try {
                    timedPacket.packet.processPacket(mc.getNetHandler());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            packetQueue.clear();
        }
        currentLatency = 0;
    }

    @Override
    public void onDisabled() {
        this.releaseAllPackets();
    }

    private static class TimedPacket {
        private final Packet<INetHandlerPlayClient> packet;
        private final long time;

        public TimedPacket(Packet<INetHandlerPlayClient> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }
}