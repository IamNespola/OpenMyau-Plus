package myau.module.modules;

import myau.event.EventTarget;
import myau.events.MoveInputEvent;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.MoveUtil;
import net.minecraft.client.Minecraft;

public class MoveFix extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty groundOnly = new BooleanProperty("ground-only", false);

    public MoveFix() {
        super("MoveFix", false, false, "Corrects strafing while silent rotations are active so your movement matches your server rotation.");
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }
        if (this.groundOnly.getValue() && !mc.thePlayer.onGround) {
            return;
        }
        if (RotationState.isActived() && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }
}
