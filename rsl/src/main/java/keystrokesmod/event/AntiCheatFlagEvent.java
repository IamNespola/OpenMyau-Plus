package keystrokesmod.event;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Event;

public class AntiCheatFlagEvent extends Event {
    public String flag;
    public Entity entity;

    public AntiCheatFlagEvent(String flag, Entity entity) {
        this.flag = flag;
        this.entity = entity;
    }
}
