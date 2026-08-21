package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PreSlotScrollEvent extends Event {
    public int slot;
    public int previousSlot;

    public PreSlotScrollEvent(int slot, int previousSlot) {
        this.slot = slot;
        this.previousSlot = previousSlot;
    }
}
