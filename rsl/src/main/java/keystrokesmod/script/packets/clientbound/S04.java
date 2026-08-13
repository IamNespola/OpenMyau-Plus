package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.ItemStack;
import net.minecraft.network.play.server.S04PacketEntityEquipment;

public class S04 extends SPacket {
    public int entityId;
    public int slot;
    public ItemStack item;

    public S04(S04PacketEntityEquipment e) {
        super(e);
        this.entityId = e.getEntityID();
        this.slot = e.getEquipmentSlot();
        this.item = ItemStack.convert(e.getItemStack());
    }

    public S04(int entityId, int slot, keystrokesmod.script.classes.ItemStack item) {
        super(new S04PacketEntityEquipment(entityId, slot, item.itemStack));
        this.entityId = entityId;
        this.slot = slot;
        this.item = item;
    }
}
