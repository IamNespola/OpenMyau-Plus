package keystrokesmod.script.packets.serverbound;

import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.client.C03PacketPlayer;

public class C03 extends CPacket {
    public Vec3 position;
    public float yaw;
    public float pitch;
    public boolean ground;

    public C03(C03PacketPlayer packet, byte f1, byte f2, byte f3, byte f4, byte f5, byte f6) { // goofy asf but cba to
        super(packet);
        if (packet instanceof C03PacketPlayer.C04PacketPlayerPosition || packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            this.position = new Vec3(packet.getPositionX(), packet.getPositionY(), packet.getPositionZ());
        }
        if (packet instanceof C03PacketPlayer.C05PacketPlayerLook || packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            this.yaw = packet.getYaw();
            this.pitch = packet.getPitch();
        }
        this.ground = packet.isOnGround();
    }

    public C03(boolean ground) {
        super(new C03PacketPlayer(ground));
        this.ground = ground;
    }

    public C03(Vec3 position, boolean ground) {
        super(new C03PacketPlayer.C04PacketPlayerPosition(position.x, position.y, position.z, ground));
        this.position = position;
        this.ground = ground;
    }

    public C03(float yaw, float pitch, boolean ground) {
        super(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, ground));
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
    }

    public C03(Vec3 position, float yaw, float pitch, boolean ground) {
        super(new C03PacketPlayer.C06PacketPlayerPosLook(position.x, position.y, position.z, yaw, pitch, ground));
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ground = ground;
    }
}
