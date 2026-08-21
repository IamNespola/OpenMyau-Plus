package keystrokesmod.script.packets.serverbound;

import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;

public class C13 extends CPacket {
    public boolean invulnerable;
    public boolean flying;
    public boolean allowFlying;
    public boolean creativeMode;
    public float flySpeed;
    public float walkSpeed;

    public C13(boolean invulnerable, boolean flying, boolean allowFlying, boolean creativeMode, float flySpeed, float walkSpeed) {
        super(null);
        this.invulnerable = invulnerable;
        this.flying = flying;
        this.allowFlying = allowFlying;
        this.creativeMode = creativeMode;
        this.flySpeed = flySpeed;
        this.walkSpeed = walkSpeed;
    }

    public C13(C13PacketPlayerAbilities packet) {
        super(packet);
    }

    @Override
    public C13PacketPlayerAbilities convert() {
        PlayerCapabilities capabilities = new PlayerCapabilities();
        capabilities.disableDamage = this.invulnerable;
        capabilities.isFlying = this.flying;
        capabilities.allowFlying = this.allowFlying;
        capabilities.isCreativeMode = this.creativeMode;
        capabilities.setFlySpeed(this.flySpeed);
        capabilities.setPlayerWalkSpeed(this.walkSpeed);
        return new C13PacketPlayerAbilities(capabilities);
    }
}
