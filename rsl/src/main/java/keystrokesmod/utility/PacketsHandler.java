package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketsHandler {
    public Minecraft mc = Minecraft.getMinecraft();

    public PacketData C0A = new PacketData();
    public PacketData C08 = new PacketData();
    public PacketData C07 = new PacketData();
    public PacketData C02 = new PacketData();
    public PacketData C02_INTERACT_AT = new PacketData();
    public PacketData C09 = new PacketData();

    public AtomicInteger playerSlot = new AtomicInteger(-1);
    public AtomicInteger serverSlot = new AtomicInteger(-1);
    private final boolean handleSlots = true;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacket(SendPacketEvent e) {
        if (e.isCanceled()) {
            return;
        }
        Packet<?> packet = e.getPacket();
        if (packet instanceof C02PacketUseEntity) {
            if (C07.sentCurrentTick.get()) {
                e.setCanceled(true);
                return;
            }
            if (((C02PacketUseEntity) packet).getAction() == C02PacketUseEntity.Action.INTERACT_AT) {
                C02_INTERACT_AT.sentCurrentTick.set(true);
            }
            C02.sentCurrentTick.set(true);
        }
        else if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08.sentCurrentTick.set(true);
        }
        else if (packet instanceof C07PacketPlayerDigging) {
            C07.sentCurrentTick.set(true);
        }
        else if (packet instanceof C0APacketAnimation) {
            if (C07.sentCurrentTick.get()) {
                e.setCanceled(true);
                return;
            }
            C0A.sentCurrentTick.set(true);
        }
        else if (packet instanceof C09PacketHeldItemChange && handleSlots) {
            C09PacketHeldItemChange slotPacket = (C09PacketHeldItemChange) packet;
            int slotId = slotPacket.getSlotId();
            if (slotId == playerSlot.get() && slotId == serverSlot.get()) {
                if (Raven.debug) {
                    Utils.sendMessage("&7bad packet detected (same slot): &b" + slotId);
                }
                e.setCanceled(true);
                return;
            }
            C09.sentCurrentTick.set(true);
            playerSlot.set(slotId);
            serverSlot.set(slotId);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S09PacketHeldItemChange && handleSlots) {
            S09PacketHeldItemChange packet = (S09PacketHeldItemChange) e.getPacket();
            int index = packet.getHeldItemHotbarIndex();
            if (index >= 0 && index < InventoryPlayer.getHotbarSize()) {
                serverSlot.set(index);
            }
        }
        else if (e.getPacket() instanceof S0CPacketSpawnPlayer && Minecraft.getMinecraft().thePlayer != null && handleSlots) {
            S0CPacketSpawnPlayer packet = (S0CPacketSpawnPlayer) e.getPacket();
            if (packet.getEntityID() != Minecraft.getMinecraft().thePlayer.getEntityId()) {
                return;
            }
            playerSlot.set(-1);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostUpdate(PostUpdateEvent e) {
        C08.updateStatesPostUpdate();
        C07.updateStatesPostUpdate();
        C02.updateStatesPostUpdate();
        C0A.updateStatesPostUpdate();
        C02_INTERACT_AT.updateStatesPostUpdate();
        C09.updateStatesPostUpdate();
    }

    public void handlePacket(Packet<?> packet) {
        if (packet instanceof C09PacketHeldItemChange && handleSlots) {
            int slotId = ((C09PacketHeldItemChange) packet).getSlotId();
            this.playerSlot.set(slotId);
            C09.sentCurrentTick.set(true);
        }
        else if (packet instanceof C02PacketUseEntity) {
            C02.sentCurrentTick.set(true);
            if (((C02PacketUseEntity) packet).getAction() == C02PacketUseEntity.Action.INTERACT_AT) {
                C02_INTERACT_AT.sentCurrentTick.set(true);
            }
        }
        else if (packet instanceof C07PacketPlayerDigging) {
            C07.sentCurrentTick.set(true);
        }
        else if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08.sentCurrentTick.set(true);
        }
        else if (packet instanceof C0APacketAnimation) {
            C0A.sentCurrentTick.set(true);
        }
    }

    public boolean sent() {
        return C02.sentCurrentTick.get() || C08.sentCurrentTick.get() || C09.sentCurrentTick.get() || C07.sentCurrentTick.get() || C0A.sentCurrentTick.get();
    }

    public boolean updateSlot(int slot) {
        if (!handleSlots) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot));
            return true;
        }
        if (playerSlot.get() == slot || slot == -1) {
            return false;
        }
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot));
        playerSlot.set(slot);
        return true;
    }

    public static class PacketData {
        public AtomicBoolean sentLastTick = new AtomicBoolean(false);
        public AtomicBoolean sentCurrentTick = new AtomicBoolean(false);

        public void updateStatesPostUpdate() {
            sentLastTick.set(sentCurrentTick.get());
            sentCurrentTick.set(false);
        }
    }
}