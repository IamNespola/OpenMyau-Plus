package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class S12 extends SPacket {
    public int entityId;
    public Vec3 motion;

    public S12(S12PacketEntityVelocity packet) {
        super(packet);
        this.entityId = packet.getEntityID();
        this.motion = new Vec3(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
    }

    public S12(int entityId, Vec3 motion) {
        super(new S12PacketEntityVelocity(entityId, motion.x, motion.y, motion.z));
        this.entityId = entityId;
        this.motion = motion;
    }
}
