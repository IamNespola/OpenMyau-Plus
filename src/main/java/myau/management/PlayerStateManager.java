package myau.management;

import myau.event.EventTarget;
import myau.events.TickEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;

public class PlayerStateManager {
    // New fields for combat detection
    private long lastCombatActionTime = 0;
    private static final long COMBAT_TIMEOUT = 5000; // 5 seconds
    private boolean inCombat = false;
    public boolean attacking = false;
    public boolean digging = false;
    public boolean placing = false;
    public boolean swapping = false;
    public boolean swinging = false;

    public void handlePacket(Packet<?> packet) {
        if (packet instanceof C02PacketUseEntity) {
            this.attacking = true;
        }
        if (packet instanceof C07PacketPlayerDigging) {
            this.digging = true;
        }
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            this.placing = true;
        }
        if (packet instanceof C09PacketHeldItemChange) {
            this.swapping = true;
        }
        if (packet instanceof C0APacketAnimation) {
            this.swinging = true;
        }
        if (packet instanceof C03PacketPlayer) {
            this.attacking = false;
            this.digging = false;
            this.placing = false;
            this.swapping = false;
            this.swinging = false;
        }
    }

    // Skiding HourClient Btw :sob:
    private void updateCombatState() {
        this.lastCombatActionTime = System.currentTimeMillis();
        this.inCombat = true;
    }

    // Skiding HourClient Btw :sob:
    public boolean isInCombat() {
        return this.inCombat;
    }

    // Skiding HourClient Btw :sob:
    @EventTarget
    public void onTick(TickEvent event) {
        myau.Myau.notificationManager.add("PlayerStateManager: Tick", 0xADD8E6); // Light blue for general tick
        if (event.getType() == myau.event.types.EventType.POST) { // Check POST tick to ensure all packet handling for the tick is done
            if (this.inCombat && (System.currentTimeMillis() - this.lastCombatActionTime > COMBAT_TIMEOUT)) {
                this.inCombat = false;
            }
        }
    }
}

