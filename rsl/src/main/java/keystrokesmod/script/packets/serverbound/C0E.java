package keystrokesmod.script.packets.serverbound;

import keystrokesmod.script.classes.ItemStack;
import net.minecraft.network.play.client.C0EPacketClickWindow;

public class C0E extends CPacket {
    public int windowId;
    public int slot;
    public int button;
    public int mode;
    public short action;
    public ItemStack itemStack;

    public C0E(int windowId, int slot, int button, int mode, ItemStack itemStack) {
        super(null);
        this.windowId = windowId;
        this.slot = slot;
        this.button = button;
        this.mode = mode;
        this.itemStack = itemStack;
    }

    public C0E(C0EPacketClickWindow packet) {
        super(packet);
        this.windowId = packet.getWindowId();
        this.slot = packet.getSlotId();
        this.button = packet.getUsedButton();
        this.mode = packet.getMode();
        this.action = packet.getActionNumber();
        this.itemStack = new ItemStack(packet.getClickedItem(), (byte) 0);
    }

    @Override
    public C0EPacketClickWindow convert() {
        return new C0EPacketClickWindow(this.windowId, this.slot, this.button, this.mode, this.itemStack.itemStack, this.action);
    }
}
