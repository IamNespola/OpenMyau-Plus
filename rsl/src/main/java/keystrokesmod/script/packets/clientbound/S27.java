package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class S27 extends SPacket {
    public float strength;
    public Vec3 position;
    public Vec3 motion;
    public List<Vec3> affectedBlockPositions;

    public S27(S27PacketExplosion packet) {
        super(packet);
        this.strength = packet.getStrength();
        this.position = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        this.motion = new Vec3(packet.func_149149_c(), packet.func_149144_d(), packet.func_149147_e());

        List<Vec3> affectedBlockPositions = new ArrayList<>();
        for (BlockPos blockPos : packet.getAffectedBlockPositions()) {
            affectedBlockPositions.add(Vec3.convert(blockPos));
        }
        this.affectedBlockPositions = affectedBlockPositions;
    }

    public S27(float strength, Vec3 position, Vec3 motion, List<Vec3> affectedBlockPositions) {
        super(create(strength, position, motion, affectedBlockPositions));
        this.strength = strength;
        this.position = position;
        this.motion = motion;
        this.affectedBlockPositions = affectedBlockPositions;
    }

    private static S27PacketExplosion create(float strength, Vec3 position, Vec3 motion, List<Vec3> affectedBlockPositions) {
        List<BlockPos> list = new ArrayList<>();
        for (Vec3 vec3 : affectedBlockPositions) {
            list.add(Vec3.getBlockPos(vec3));
        }
        return new S27PacketExplosion(position.x, position.y, position.z, strength, list, Vec3.getVec3(motion));
    }
}
