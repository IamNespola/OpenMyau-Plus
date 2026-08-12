package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class S08 extends SPacket {
    public Vec3 position;
    public float yaw;
    public float pitch;
    public Set<String> enumFlags;

    public S08(S08PacketPlayerPosLook packet) {
        super(packet);
        this.position = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        this.yaw = packet.getYaw();
        this.pitch = packet.getPitch();

        Set<S08PacketPlayerPosLook.EnumFlags> flags = packet.func_179834_f();
        Set<String> flagsStr = new HashSet<>(flags.size());
        for (S08PacketPlayerPosLook.EnumFlags flag : flags) {
            flagsStr.add(flag.name());
        }
        this.enumFlags = flagsStr;
    }

    public S08(Vec3 position, float yaw, float pitch, Set<String> enumFlags) {
        super(create(position, yaw, pitch, enumFlags));
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.enumFlags = enumFlags;
    }

    private static S08PacketPlayerPosLook create(Vec3 position, float yaw, float pitch, Set<String> enumFlags) {
        Set<S08PacketPlayerPosLook.EnumFlags> enumSet = EnumSet.noneOf(S08PacketPlayerPosLook.EnumFlags.class);
        for (String flag : enumFlags) {
            S08PacketPlayerPosLook.EnumFlags enumFlag = Utils.getEnum(S08PacketPlayerPosLook.EnumFlags.class, flag);
            if (enumFlag != null) {
                enumSet.add(enumFlag);
            }
        }
        return new S08PacketPlayerPosLook(position.x, position.y, position.z, yaw, pitch, enumSet);
    }
}