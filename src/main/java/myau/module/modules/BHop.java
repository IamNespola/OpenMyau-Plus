package myau.module.modules;

import myau.event.EventTarget;
import myau.events.LivingUpdateEvent;
import myau.mixin.IAccessorEntity;
import myau.mixin.IAccessorEntityPlayer;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public class BHop extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final double INTAVE_BOOST_CONSTANT = 0.003D;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Legit", "Intave14"});
    public final BooleanProperty boost = new BooleanProperty("boost", true, this::isIntave14);
    public final FloatProperty initialBoostMultiplier = new FloatProperty("initial-boost-multiplier", 1.0F, 0.01F, 10.0F, this::isIntave14);
    public final BooleanProperty lowHop = new BooleanProperty("low-hop", true, this::isIntave14);
    public final FloatProperty strafeStrength = new FloatProperty("strafe-strength", 0.29F, 0.1F, 0.29F, this::isIntave14);
    public final FloatProperty groundTimer = new FloatProperty("ground-timer", 0.5F, 0.1F, 5.0F, this::isIntave14);
    public final FloatProperty airTimer = new FloatProperty("air-timer", 1.09F, 0.1F, 5.0F, this::isIntave14);

    public BHop() {
        super("BHop", false, false, "Bunny hop movement module");
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (!canHop()) {
            resetTimer();
            return;
        }

        if (isLegit()) {
            handleLegit();
            return;
        }

        mc.thePlayer.setSprinting(true);
        handleIntave14();
    }

    private boolean canHop() {
        EntityPlayerSP player = mc.thePlayer;
        return MoveUtil.isForwardPressed()
                && !player.isSneaking()
                && !player.isInWater()
                && !player.isInLava()
                && !player.isOnLadder()
                && !((IAccessorEntity) player).getIsInWeb();
    }

    private void handleLegit() {
        resetTimer();
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
        }
    }

    private void handleIntave14() {
        EntityPlayerSP player = mc.thePlayer;
        if (player.onGround) {
            player.motionY = 0.42D - (lowHop.getValue() ? 1.7E-14D : 0.0D);
            MoveUtil.setSpeed(strafeStrength.getValue());
            setTimer(groundTimer.getValue());
        } else {
            setTimer(airTimer.getValue());
        }

        if (boost.getValue() && player.motionY > 0.003D) {
            double multiplier = 1.0D + INTAVE_BOOST_CONSTANT * initialBoostMultiplier.getValue();
            player.motionX *= multiplier;
            player.motionZ *= multiplier;
        }
    }

    private boolean isLegit() {
        return mode.getModeString().equalsIgnoreCase("Legit");
    }

    private boolean isIntave14() {
        return mode.getModeString().equalsIgnoreCase("Intave14");
    }

    private void setTimer(float speed) {
        net.minecraft.util.Timer timer = ((IAccessorMinecraft) mc).getTimer();
        if (timer != null) {
            timer.timerSpeed = speed;
        }
    }

    private void resetTimer() {
        setTimer(1.0F);
    }

    @Override
    public void onEnabled() {
        resetTimer();
    }

    @Override
    public void onDisabled() {
        resetTimer();
        if (mc.thePlayer != null) {
            ((IAccessorEntityPlayer) mc.thePlayer).setSpeedInAir(0.02F);
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}
