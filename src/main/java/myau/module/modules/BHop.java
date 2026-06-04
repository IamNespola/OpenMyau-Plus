package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.LivingUpdateEvent;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.mixin.IAccessorC03PacketPlayer;
import myau.mixin.IAccessorEntity;
import myau.mixin.IAccessorEntityPlayer;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C03PacketPlayer;

public class BHop extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Legit", "Intave14", "Matrix", "NCP", "Vulcan", "Custom"});
    public final BooleanProperty legitRotation = new BooleanProperty("rotation", false, () -> mode.getValue() == 0);
    public final BooleanProperty legitCpuSpeedUpExploit = new BooleanProperty("cpu-speedup-exploit", false, () -> mode.getValue() == 0);

    public final BooleanProperty boost = new BooleanProperty("boost", true, () -> mode.getValue() == 1);
    public final FloatProperty initialBoostMultiplier = new FloatProperty("initial-boost-multiplier", 1.0F, 0.01F, 10.0F, () -> mode.getValue() == 1);
    public final BooleanProperty lowHop = new BooleanProperty("low-hop", true, () -> mode.getValue() == 1 || mode.getValue() == 3);
    public final FloatProperty strafeStrength = new FloatProperty("strafe-strength", 0.29F, 0.1F, 0.29F, () -> mode.getValue() == 1);
    public final FloatProperty groundTimer = new FloatProperty("ground-timer", 0.5F, 0.1F, 5.0F, () -> mode.getValue() == 1);
    public final FloatProperty airTimer = new FloatProperty("air-timer", 1.09F, 0.1F, 5.0F, () -> mode.getValue() == 1);

    public final FloatProperty customMultiplier = new FloatProperty("custom-multiplier", 1.0F, 0.0F, 10.0F, () -> mode.getValue() == 5);
    public final FloatProperty customFriction = new FloatProperty("custom-friction", 1.0F, 0.0F, 10.0F, () -> mode.getValue() == 5);
    public final IntProperty customStrafe = new IntProperty("custom-strafe", 0, 0, 100, () -> mode.getValue() == 5);

    public final BooleanProperty ncpPullDown = new BooleanProperty("ncp-pull-down", true, () -> mode.getValue() == 3);
    public final FloatProperty ncpPullMotionMultiplier = new FloatProperty("ncp-pull-motion", 1.0F, 0.01F, 10.0F, () -> mode.getValue() == 3 && ncpPullDown.getValue());
    public final IntProperty ncpPullOnTick = new IntProperty("ncp-pull-tick", 5, 1, 9, () -> mode.getValue() == 3 && ncpPullDown.getValue());
    public final BooleanProperty ncpPullOnHurt = new BooleanProperty("ncp-pull-hurt", true, () -> mode.getValue() == 3 && ncpPullDown.getValue());
    public final BooleanProperty ncpBoost = new BooleanProperty("ncp-boost", true, () -> mode.getValue() == 3);
    public final FloatProperty ncpBoostMultiplier = new FloatProperty("ncp-boost-multiplier", 1.0F, 0.01F, 10.0F, () -> mode.getValue() == 3 && ncpBoost.getValue());
    public final BooleanProperty ncpTimer = new BooleanProperty("ncp-timer", true, () -> mode.getValue() == 3);
    public final BooleanProperty ncpDamageBoost = new BooleanProperty("ncp-damage-boost", true, () -> mode.getValue() == 3);
    public final BooleanProperty ncpAirStrafe = new BooleanProperty("ncp-air-strafe", true, () -> mode.getValue() == 3);

    private int ncpTicksInAir = 0;
    private int vulcanTicksAfterJump = -1;

    public BHop() {
        super("BHop", false, false, "Bunny hop movement module");
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (!canHop()) {
            resetTimer();
            ncpTicksInAir = 0;
            vulcanTicksAfterJump = -1;
            return;
        }

        switch (mode.getValue()) {
            case 0:
                handleLegit();
                break;
            case 1:
                mc.thePlayer.setSprinting(true);
                handleIntave14();
                break;
            case 2:
                mc.thePlayer.setSprinting(true);
                handleMatrix();
                break;
            case 3:
                mc.thePlayer.setSprinting(true);
                handleNCP();
                break;
            case 4:
                mc.thePlayer.setSprinting(true);
                handleVulcan();
                break;
            case 5:
                mc.thePlayer.setSprinting(true);
                handleCustom();
                break;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mode.getValue() != 4 || mc.thePlayer == null) return;
        if (event.getType() != EventType.SEND || !(event.getPacket() instanceof C03PacketPlayer)) return;

        if (mc.thePlayer.motionY < 0.0D) {
            ((IAccessorC03PacketPlayer) event.getPacket()).setOnGround(true);
        }
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
        EntityPlayerSP player = mc.thePlayer;

        if (player.onGround && MoveUtil.isForwardPressed()) {
            player.jump();
        }

        player.setSprinting(player.movementInput.moveForward > 0.8F);
        resetTimer();
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
            double multiplier = 1.0D + 0.003D * initialBoostMultiplier.getValue();
            player.motionX *= multiplier;
            player.motionZ *= multiplier;
        }
    }

    private void handleMatrix() {
        EntityPlayerSP player = mc.thePlayer;
        resetTimer();
        if (player.onGround) {
            player.motionY = 0.419652D;
            MoveUtil.setSpeed(Math.max(MoveUtil.getSpeed(), MoveUtil.getBaseMoveSpeed()));
        } else if (player.motionX * player.motionX + player.motionZ * player.motionZ < 0.04D) {
            MoveUtil.setSpeed(Math.max(MoveUtil.getSpeed(), MoveUtil.getBaseMoveSpeed()));
        }
    }

    private void handleNCP() {
        EntityPlayerSP player = mc.thePlayer;
        int speedLevel = MoveUtil.getSpeedLevel();

        if (player.onGround) {
            ncpTicksInAir = 0;
            double groundMin = 0.281D + 0.199999999D * speedLevel;
            player.motionY = lowHop.getValue() ? 0.4D : 0.42D;
            MoveUtil.setSpeed(Math.max(MoveUtil.getSpeed(), groundMin));
        } else {
            ncpTicksInAir++;
            if (ncpAirStrafe.getValue()) {
                double airMin = 0.2D + 0.199999999D * speedLevel;
                MoveUtil.setSpeed(Math.max(MoveUtil.getSpeed(), airMin));
            }
            handleNCPPullDown(player);
        }

        if (ncpBoost.getValue() && MoveUtil.isForwardPressed()) {
            double multiplier = 1.0D + 0.00718D * ncpBoostMultiplier.getValue();
            player.motionX *= multiplier;
            player.motionZ *= multiplier;
        }

        if (ncpTimer.getValue()) {
            setTimer(1.08F);
        } else {
            resetTimer();
        }

        if (ncpDamageBoost.getValue() && player.hurtTime >= 1) {
            MoveUtil.setSpeed(Math.max(MoveUtil.getSpeed(), 0.5D));
        }
    }

    private void handleNCPPullDown(EntityPlayerSP player) {
        if (!ncpPullDown.getValue()) return;

        if (ncpTicksInAir == ncpPullOnTick.getValue()) {
            MoveUtil.setSpeed(MoveUtil.getSpeed());
            player.motionY -= 0.1523351824467155D * ncpPullMotionMultiplier.getValue();
        }

        if (ncpPullOnHurt.getValue() && player.hurtTime >= 5 && player.motionY >= 0.0D) {
            player.motionY -= 0.1D;
        }
    }

    private void handleVulcan() {
        EntityPlayerSP player = mc.thePlayer;
        boolean hasSpeed = MoveUtil.getSpeedLevel() > 0;

        if (player.onGround) {
            player.jump();
            vulcanTicksAfterJump = 0;
            return;
        }

        if (vulcanTicksAfterJump >= 0) {
            switch (vulcanTicksAfterJump) {
                case 0:
                    MoveUtil.setSpeed(hasSpeed ? 0.771D : 0.5D);
                    break;
                case 1:
                    MoveUtil.setSpeed(hasSpeed ? 0.605D : 0.31D);
                    break;
                case 2:
                    MoveUtil.setSpeed(hasSpeed ? 0.57D : 0.29D);
                    player.motionY = hasSpeed ? -0.5D : -0.37D;
                    break;
                case 3:
                    MoveUtil.setSpeed(hasSpeed ? 0.595D : 0.27D);
                    break;
                case 4:
                    MoveUtil.setSpeed(hasSpeed ? 0.595D : 0.28D);
                    break;
            }
            vulcanTicksAfterJump++;
            if (vulcanTicksAfterJump > 4) {
                vulcanTicksAfterJump = -1;
            }
        }

        if (Math.abs(player.fallDistance) > 0.0F && hasSpeed) {
            player.motionX *= 1.055D;
            player.motionZ *= 1.055D;
        }
    }

    private void handleCustom() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.42F;
            MoveUtil.setSpeed(MoveUtil.getJumpMotion() * customMultiplier.getValue(), MoveUtil.getMoveYaw());
        }
    }

    @EventTarget
    public void onCustomStrafe(myau.events.StrafeEvent event) {
        if (!this.isEnabled() || mode.getValue() != 5 || mc.thePlayer == null || !canHop()) return;
        if (!mc.thePlayer.onGround) {
            if (customFriction.getValue() != 1.0F) {
                event.setFriction(event.getFriction() * customFriction.getValue());
            }
            if (customStrafe.getValue() > 0) {
                double speed = MoveUtil.getSpeed();
                MoveUtil.setSpeed(speed * (100.0F - customStrafe.getValue()) / 100.0F, MoveUtil.getDirectionYaw());
                MoveUtil.addSpeed(speed * customStrafe.getValue() / 100.0F, MoveUtil.getMoveYaw());
                MoveUtil.setSpeed(speed);
            }
        }
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
        ncpTicksInAir = 0;
        vulcanTicksAfterJump = -1;
    }

    @Override
    public void onDisabled() {
        resetTimer();
        ncpTicksInAir = 0;
        vulcanTicksAfterJump = -1;
        if (mc.thePlayer != null) {
            ((IAccessorEntityPlayer) mc.thePlayer).setSpeedInAir(0.02F);
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}
