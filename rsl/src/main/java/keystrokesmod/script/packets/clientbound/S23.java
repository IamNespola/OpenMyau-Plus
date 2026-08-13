package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Block;
import keystrokesmod.script.classes.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;

public class S23 extends SPacket {
    public Vec3 position;
    public Block block;

    public S23(S23PacketBlockChange packet, byte f) {
        super(packet);
        this.position = Vec3.convert(packet.getBlockPosition());
        this.block = new Block(packet.getBlockState(), new BlockPos(position.x, position.y, position.z));
    }

    public S23(Vec3 position) {
        super(new S23PacketBlockChange(Minecraft.getMinecraft().theWorld, Vec3.getBlockPos(position)));
        this.position = position;
        this.block = new Block(position);
    }
}
