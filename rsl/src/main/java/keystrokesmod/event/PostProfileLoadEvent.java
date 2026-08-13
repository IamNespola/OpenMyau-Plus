package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class PostProfileLoadEvent extends Event {
    public String profileName;

    public PostProfileLoadEvent(String profileName) {
        this.profileName = profileName;
    }
}
