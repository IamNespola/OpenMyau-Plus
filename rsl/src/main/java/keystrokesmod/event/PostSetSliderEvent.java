package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class PostSetSliderEvent extends Event {
    public double previousVal;
    public double newVal;

    public PostSetSliderEvent(double previousVal, double newVal) {
        this.previousVal = previousVal;
        this.newVal = newVal;
    }
}
