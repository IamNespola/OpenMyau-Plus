package myau.module.modules.player;

import myau.event.EventTarget;
import myau.events.PacketEvent;
import myau.module.Category;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class FlagDetector extends Module {
    public FlagDetector() {
        super("FlagDetector",Category.PLAYER, false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled())
            return;

        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            ChatUtil.sendFormatted("&7[&cFlagDetector&7] &fServer flag detected (Lagback)!");
        }
    }
}
