package keystrokesmod.script.packets.clientbound;

import net.minecraft.network.play.server.S18PacketEntityTeleport;

public class S18 extends SPacket {
    public int entityId;
    public int posX;
    public int posY;
    public int posZ;
    public byte yaw;
    public byte pitch;
    public boolean onGround;

    public S18(S18PacketEntityTeleport packet) {
        super(packet);

        this.entityId = packet.getEntityId();
        this.posX = packet.getX();
        this.posY = packet.getY();
        this.posZ = packet.getZ();
        this.yaw = packet.getYaw();
        this.pitch = packet.getPitch();
        this.onGround = packet.getOnGround();
    }

    public S18(int i, int j, int k, int l, byte b, byte c, boolean bl) {
        super(new S18PacketEntityTeleport(i, j, k, l, b, c, bl));
        this.entityId = i;
        this.posX = j;
        this.posY = k;
        this.posZ = l;
        this.yaw = b;
        this.pitch = c;
        this.onGround = bl;
    }
}
