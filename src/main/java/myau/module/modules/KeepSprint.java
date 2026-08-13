package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty slowdown = new PercentProperty("slowdown", 0);
    public final BooleanProperty groundOnly = new BooleanProperty("ground-only", false);
    public final BooleanProperty reachOnly = new BooleanProperty("reach-only", false);

    public KeepSprint() {
        super("KeepSprint", false);
    }

    @Override
    public boolean shouldKeepSprint() {

        if (mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }

        if (groundOnly.getValue() && !mc.thePlayer.onGround) {
            return false;
        }

        if (reachOnly.getValue()) {

            if (mc.objectMouseOver == null || mc.objectMouseOver.hitVec == null) {
                return false;
            }

            double distance = mc.objectMouseOver.hitVec.distanceTo(
                    mc.getRenderViewEntity().getPositionEyes(1.0F)
            );

            return distance > 3.0;
        }

        return true;
    }
}
