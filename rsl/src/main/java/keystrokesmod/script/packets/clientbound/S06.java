package keystrokesmod.script.packets.clientbound;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S06PacketUpdateHealth;

public class S06 extends SPacket {
    public float health;
    public float saturation;
    public int food;

    public S06(S06PacketUpdateHealth packet) {
        super(packet);
        this.health = packet.getHealth();
        this.saturation = packet.getSaturationLevel();
        this.food = packet.getFoodLevel();
    }

    public S06(float health, float saturation, int food) {
        super(new S06PacketUpdateHealth(health, food, saturation));
        this.health = health;
        this.saturation = saturation;
        this.food = food;
    }
}
