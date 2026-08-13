package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;

public class S2F extends SPacket {
    public int windowId;
    public int slot;
    public ItemStack itemStack;

    public S2F(S2FPacketSetSlot packet) {
        super(packet);
        this.windowId = packet.func_149175_c();
        this.slot = packet.func_149173_d();
        this.itemStack = ItemStack.convert(packet.func_149174_e());
    }

    public S2F(int windowId, int slot, ItemStack itemStack) {
        super(new S2FPacketSetSlot(windowId, slot, itemStack.itemStack));
        this.windowId = windowId;
        this.slot = slot;
        this.itemStack = itemStack;
    }
}
