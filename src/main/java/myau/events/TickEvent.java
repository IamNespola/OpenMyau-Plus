package myau.events;

import myau.event.events.callables.EventCancellable;
import myau.event.types.EventType;

public class TickEvent extends EventCancellable {
    private final EventType type;

    public TickEvent(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return this.type;
    }
}