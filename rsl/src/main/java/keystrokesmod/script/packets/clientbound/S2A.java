package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.EnumParticleTypes;

public class S2A extends SPacket {
    public String type;
    public Vec3 position;
    public Vec3 offset;
    public float speed;
    public int count;
    public boolean longDistance;
    public int[] args;

    public S2A(S2APacketParticles packet) {
        super(packet);
        this.type = packet.getParticleType().name();
        this.longDistance = packet.isLongDistance();
        this.position = new Vec3(packet.getXCoordinate(), packet.getYCoordinate(), packet.getZCoordinate());
        this.offset = new Vec3(packet.getXOffset(), packet.getYOffset(), packet.getZOffset());
        this.speed = packet.getParticleSpeed();
        this.count = packet.getParticleCount();
        this.args = packet.getParticleArgs();
    }

    public S2A(String type, boolean longDistance, Vec3 position, Vec3 offset, float speed, int count, int[] args) {
        super(new S2APacketParticles(Utils.getEnum(EnumParticleTypes.class, type), longDistance, (float) position.x, (float) position.y, (float) position.z, (float) offset.x, (float) offset.y, (float) offset.z, speed, count, args));
        this.type = type;
        this.longDistance = longDistance;
        this.position = position;
        this.offset = offset;
        this.speed = speed;
        this.count = count;
        this.args = args;
    }
}
