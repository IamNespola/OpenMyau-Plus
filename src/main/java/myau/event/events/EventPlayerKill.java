package myau.event.events;

import net.minecraft.entity.EntityLivingBase;

public class EventPlayerKill implements Event {
    private final EntityLivingBase killedEntity;

    public EventPlayerKill(EntityLivingBase killedEntity) {
        this.killedEntity = killedEntity;
    }

    public EntityLivingBase getKilledEntity() {
        return killedEntity;
    }
}
