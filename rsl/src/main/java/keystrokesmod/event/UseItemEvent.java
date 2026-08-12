package keystrokesmod.event;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

public class UseItemEvent extends Event {
    public ItemStack usedItemStack;

    public UseItemEvent(ItemStack usedItemStack) {
        this.usedItemStack = usedItemStack;
    }
}
