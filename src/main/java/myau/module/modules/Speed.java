package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.events.MoveInputEvent;
import myau.events.StrafeEvent;
import myau.events.UpdateEvent;
import myau.management.RotationState;
import myau.mixin.IAccessorEntity;
import myau.module.Module;
import myau.util.MoveUtil;
import myau.util.RotationUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;

public class Speed extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"default", "legit"});

    public final FloatProperty multiplier = new FloatProperty("multiplier", 1.0F, 0.0F, 10.0F);
    public final FloatProperty friction = new FloatProperty("friction", 1.0F, 0.0F, 10.0F);
    public final PercentProperty strafe = new PercentProperty("strafe", 0);
    public final BooleanProperty onlyJumping = new BooleanProperty("only-jumping", true);
    public final ModeProperty blockPlacements = new ModeProperty("block-placements", 1, new String[]{"LEGIT", "BLATANT"});

    private boolean wasOnGround = false;
    private boolean boosting = false;
    private float cachedSilentYaw = Float.NaN;

    private boolean canBoost() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        return !scaffold.isEnabled() && MoveUtil.isForwardPressed()
                && mc.thePlayer.getFoodStats().getFoodLevel() > 6
                && !mc.thePlayer.isSneaking()
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isInLava()
                && !((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private boolean canBoostLegit() {
        if (!canBoost()) return false;
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.target != null) return false;
        if (this.onlyJumping.getValue()) {
            return mc.gameSettings.keyBindJump.isKeyDown();
        }
        return true;
    }

    private boolean isOnlyForward() {
        return mc.gameSettings.keyBindForward.isKeyDown()
                && !mc.gameSettings.keyBindLeft.isKeyDown()
                && !mc.gameSettings.keyBindRight.isKeyDown()
                && !mc.gameSettings.keyBindBack.isKeyDown();
    }

    public boolean isBoosting() {
        return boosting && mode.getModeString().equals("legit")
                && blockPlacements.getValue() == 0;
    }

    public float getSilentYaw() {
        return cachedSilentYaw;
    }

    public Speed() {
        super("Speed", false);
    }

    @EventTarget(Priority.LOW)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.PRE) return;

        if (this.mode.getModeString().equals("legit")) {
            if (!canBoostLegit()) {
                boosting = false;
                cachedSilentYaw = Float.NaN;
                wasOnGround = mc.thePlayer.onGround;
                return;
            }

            boolean onGround = mc.thePlayer.onGround;

            if (!onGround && wasOnGround && isOnlyForward()) {
                boosting = true;
            }

            if (onGround) {
                boosting = false;
                cachedSilentYaw = Float.NaN;
            }

            if (boosting && isOnlyForward()) {
                float realYaw = event.getNewYaw();
                float realPitch = event.getNewPitch();
                float quantized = RotationUtil.quantizeAngle(realYaw + 45.0F);
                cachedSilentYaw = quantized;
                event.setRotation(quantized, realPitch, 1);
                event.setPervRotation(quantized, 1);
            } else if (boosting) {
                boosting = false;
                cachedSilentYaw = Float.NaN;
            }

            wasOnGround = onGround;
        }
    }

    @EventTarget(Priority.LOW)
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled()) return;
        if (!this.mode.getModeString().equals("legit")) return;
        if (!canBoostLegit()) return;
        if (mc.thePlayer.onGround) return;

        if (boosting && RotationState.isActived() && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @EventTarget(Priority.LOW)
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled()) return;

        if (this.mode.getModeString().equals("default") && canBoost()) {
            runDefault(event);
        } else if (this.mode.getModeString().equals("legit") && canBoostLegit()) {
            if (!this.onlyJumping.getValue() && mc.thePlayer.onGround && MoveUtil.isForwardPressed()) {
                mc.thePlayer.jump();
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled()) return;

        if (this.mode.getModeString().equals("default") && canBoost()) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    private void runDefault(StrafeEvent event) {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.42F;
            MoveUtil.setSpeed(
                    MoveUtil.getJumpMotion() * (double) this.multiplier.getValue().floatValue(),
                    MoveUtil.getMoveYaw()
            );
        } else {
            if (this.friction.getValue() != 1.0F) {
                event.setFriction(event.getFriction() * this.friction.getValue());
            }
            if (this.strafe.getValue() > 0) {
                double speed = MoveUtil.getSpeed();
                MoveUtil.setSpeed(speed * (double) ((float) (100 - this.strafe.getValue()) / 100.0F), MoveUtil.getDirectionYaw());
                MoveUtil.addSpeed(
                        speed * (double) ((float) this.strafe.getValue().intValue() / 100.0F), MoveUtil.getMoveYaw()
                );
                MoveUtil.setSpeed(speed);
            }
        }
    }

    @Override
    public void onDisabled() {
        boosting = false;
        wasOnGround = false;
        cachedSilentYaw = Float.NaN;
    }
}