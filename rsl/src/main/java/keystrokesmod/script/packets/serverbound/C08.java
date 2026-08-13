package keystrokesmod.script.packets.serverbound;

import keystrokesmod.script.classes.ItemStack;
import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

public class C08 extends CPacket {
    public ItemStack itemStack;
    public Vec3 position;
    public int direction;
    public Vec3 offset;

    public C08(ItemStack itemStack, Vec3 position, int direction, Vec3 offset) {
        super(null);
        this.itemStack = itemStack;
        this.position = position;
        this.direction = direction;
        this.offset = offset;
    }

    public C08(C08PacketPlayerBlockPlacement packet) {
        super(packet);
        this.itemStack = ItemStack.convert(packet.getStack());
        this.position = Vec3.convert(packet.getPosition());
        this.direction = packet.getPlacedBlockDirection();
        this.offset = new Vec3(packet.getPlacedBlockOffsetX(), packet.getPlacedBlockOffsetY(), packet.getPlacedBlockOffsetZ());
    }

    @Override
    public C08PacketPlayerBlockPlacement convert() {
        return new C08PacketPlayerBlockPlacement(Vec3.getBlockPos(this.position), this.direction, this.itemStack != null ? this.itemStack.itemStack : null, (float) this.offset.x, (float) this.offset.y, (float) this.offset.z);
    }
}
