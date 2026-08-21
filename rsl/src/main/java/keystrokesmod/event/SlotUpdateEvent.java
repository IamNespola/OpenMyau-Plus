package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class SlotUpdateEvent extends Event {
    public int slot;

    public SlotUpdateEvent(int slot) {
        this.slot = slot;
    }
}
