package keystrokesmod.event;

import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;

public class CollisionEvent extends Event {
    public final BlockPos blockPos;
    public final Block block;
    public AxisAlignedBB boundingBox;

    public CollisionEvent(BlockPos position, Block block, AxisAlignedBB boundingBox) {
        this.blockPos = position;
        this.block = block;
        this.boundingBox = boundingBox;
    }
}
