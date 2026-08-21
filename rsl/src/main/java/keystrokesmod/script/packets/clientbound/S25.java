package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Block;
import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.util.BlockPos;

public class S25 extends SPacket {
    public int entityId;
    public Block block;
    public int progress;

    public S25(S25PacketBlockBreakAnim packet) {
        super(packet);
        this.entityId = packet.getBreakerId();
        this.block = new Block(Vec3.convert(packet.getPosition()));
        this.progress = packet.getProgress();
    }

    public S25(int entityId, Block block, int progress) {
        super(new S25PacketBlockBreakAnim(entityId, new BlockPos(block.x, block.y, block.z), progress));
        this.entityId = entityId;
        this.block = block;
        this.progress = progress;
    }

    public S25(int entityId, Vec3 position, int progress) {
        super(new S25PacketBlockBreakAnim(entityId, Vec3.getBlockPos(position), progress));
        this.entityId = entityId;
        this.block = new Block(position);
        this.progress = progress;
    }
}