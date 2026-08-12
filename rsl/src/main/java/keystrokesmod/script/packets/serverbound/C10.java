package keystrokesmod.script.packets.serverbound;

import keystrokesmod.script.classes.ItemStack;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;

public class C10 extends CPacket {
    public int slot;
    public ItemStack itemStack;

    public C10(int slot, ItemStack itemStack) {
        super(null);
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public C10(C10PacketCreativeInventoryAction packet) {
        super(packet);
        this.slot = packet.getSlotId();
        this.itemStack = ItemStack.convert(packet.getStack());
    }

    @Override
    public C10PacketCreativeInventoryAction convert() {
        return new C10PacketCreativeInventoryAction(slot, itemStack.itemStack);
    }
}
