package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.MoveInputEvent;
import myau.events.UpdateEvent;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.ItemUtil;
import myau.util.KeyBindUtil;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class CombatHelper extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty publicSearchRange = new FloatProperty("public-search-range", 6.0F, 0.1F, 12.0F);

    public final BooleanProperty comboBreaker = new BooleanProperty("combo-breaker", true);
    public final FloatProperty breakerAttackRange = new FloatProperty("breaker-attack-range", 3.0F, 0.1F, 8.0F, comboBreaker::getValue);

    public final BooleanProperty keepCombo = new BooleanProperty("keep-combo", true);
    public final FloatProperty keepComboAttackRange = new FloatProperty("keep-combo-attack-range", 3.0F, 0.1F, 8.0F, keepCombo::getValue);

    public final BooleanProperty smartBlocking = new BooleanProperty("smart-blocking", true);
    public final FloatProperty blockRange = new FloatProperty("block-range", 2.0F, 0.1F, 8.0F, smartBlocking::getValue);

    public final BooleanProperty adaptiveStrafe = new BooleanProperty("adaptive-strafe", false);
    public final BooleanProperty forceStrafe = new BooleanProperty("force-strafe", false, adaptiveStrafe::getValue);
    public final FloatProperty strafeDistance = new FloatProperty("target-strafe-distance", 3.0F, 2.5F, 6.0F, adaptiveStrafe::getValue);
    public final FloatProperty maxStrafeDistance = new FloatProperty("max-strafe-distance", 8.0F, 4.5F, 15.0F, adaptiveStrafe::getValue);
    public final IntProperty predictionTicks = new IntProperty("target-prediction-ticks", 5, 1, 20, adaptiveStrafe::getValue);
    public final BooleanProperty lagCheck = new BooleanProperty("lag-check", false, adaptiveStrafe::getValue);

    private boolean isBlocking;
    private EntityLivingBase target;
    private Vec3 lastTargetPos;
    private double lastStrafeYaw;

    public CombatHelper() {
        super("CombatHelper", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.getType() != EventType.PRE) {
            return;
        }

        this.target = this.resolveTarget();
        this.handleSmartBlocking();
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null || this.target == null) {
            return;
        }

        double distance = RotationUtil.distanceToEntity(this.target);

        if (this.comboBreaker.getValue()
                && distance >= this.breakerAttackRange.getValue()
                && !mc.thePlayer.onGround
                && mc.thePlayer.hurtTime != 0
                && this.target.hurtTime == 0) {
            this.holdS();
        }

        if (this.keepCombo.getValue()
                && distance < this.keepComboAttackRange.getValue()
                && !this.target.onGround
                && this.target.hurtTime != 0
                && mc.thePlayer.hurtTime == 0) {
            this.holdS();
        }

        if (this.adaptiveStrafe.getValue()
                && distance < this.maxStrafeDistance.getValue()
                && (this.attemptingToStrafe() || this.forceStrafe.getValue())) {
            if (!(this.target instanceof EntityPlayer)) {
                return;
            }

            float strafeYaw = this.calculateStrafeYaw((EntityPlayer) this.target);
            float referenceYaw = RotationState.isActived() ? RotationState.getSmoothedYaw() : mc.thePlayer.rotationYaw;
            this.fixMovement(referenceYaw, strafeYaw);
        }
    }

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura != null && killAura.isEnabled()) {
            return killAura.getTarget();
        }

        return this.findPublicTarget(this.publicSearchRange.getValue() * 2.0D);
    }

    private EntityPlayer findPublicTarget(double range) {
        EntityPlayer target = null;
        float bestDistance = (float) range;
        AntiBot antiBot = (AntiBot) Myau.moduleManager.modules.get(AntiBot.class);

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) {
                continue;
            }

            if (antiBot != null && antiBot.isEnabled() && antiBot.isBotPlayer(player)) {
                continue;
            }

            float distance = mc.thePlayer.getDistanceToEntity(player) - 0.5657F;
            if (distance <= bestDistance) {
                target = player;
                bestDistance = distance;
            }
        }

        return target;
    }

    private void handleSmartBlocking() {
        if (!this.smartBlocking.getValue()) {
            return;
        }

        if (this.target == null) {
            if (this.isBlocking) {
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                this.isBlocking = false;
            }
            return;
        }

        if (ItemUtil.isHoldingSword()) {
            boolean shouldBlock = this.target.hurtTime > 3 && RotationUtil.distanceToEntity(this.target) <= this.blockRange.getValue();
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), shouldBlock);
            this.isBlocking = this.target.hurtTime > 3;
        } else if (this.isBlocking) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            this.isBlocking = false;
        }
    }

    private float calculateStrafeYaw(EntityPlayer target) {
        if (!this.adaptiveStrafe.getValue()
                || target == null
                || (this.lagCheck.getValue() && (Myau.blinkManager == null || !Myau.blinkManager.isBlinking()))
                || mc.thePlayer.hurtTime != 0) {
            return mc.thePlayer.rotationYaw;
        }

        double playerX = mc.thePlayer.posX;
        double playerZ = mc.thePlayer.posZ;

        double targetMotionX = 0.0D;
        double targetMotionZ = 0.0D;
        if (this.lastTargetPos != null) {
            targetMotionX = target.posX - this.lastTargetPos.xCoord;
            targetMotionZ = target.posZ - this.lastTargetPos.zCoord;
        }
        this.lastTargetPos = new Vec3(target.posX, target.posY, target.posZ);

        double predictedTargetX = target.posX + targetMotionX * this.predictionTicks.getValue();
        double predictedTargetZ = target.posZ + targetMotionZ * this.predictionTicks.getValue();

        double relX = playerX - predictedTargetX;
        double relZ = playerZ - predictedTargetZ;
        double distanceToPredicted = Math.sqrt(relX * relX + relZ * relZ);

        double idealDistance = Math.max(this.strafeDistance.getValue(), 2.5D);
        if (distanceToPredicted < 2.5D) {
            idealDistance = 4.0D;
        }

        double angleToPredicted = Math.atan2(relZ, relX);
        double angleOffset = distanceToPredicted > idealDistance ? -45.0D : 45.0D;
        angleToPredicted += Math.toRadians(angleOffset);

        double strafeX = predictedTargetX + Math.cos(angleToPredicted) * idealDistance;
        double strafeZ = predictedTargetZ + Math.sin(angleToPredicted) * idealDistance;

        float targetYaw = (float) Math.toDegrees(Math.atan2(strafeZ - playerZ, strafeX - playerX));
        float deltaYaw = MathHelper.wrapAngleTo180_float(targetYaw - (float) this.lastStrafeYaw);

        targetYaw = (float) this.lastStrafeYaw + MathHelper.clamp_float(deltaYaw, -20.0F, 20.0F);
        this.lastStrafeYaw = targetYaw;

        return targetYaw - 90.0F;
    }

    private boolean attemptingToStrafe() {
        return mc.thePlayer.movementInput.moveStrafe != 0.0F
                || (this.target != null && RotationUtil.angleToEntity(this.target) > 25.0F);
    }

    private void holdS() {
        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;
        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        float referenceYaw = RotationState.isActived() ? RotationState.getSmoothedYaw() : mc.thePlayer.rotationYaw;
        double angle = MathHelper.wrapAngleTo180_double(referenceYaw - 180.0F);

        float closestForward = 0.0F;
        float closestStrafe = 0.0F;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
                if (predictedStrafe == 0.0F && predictedForward == 0.0F) {
                    continue;
                }

                double predictedAngle = MathHelper.wrapAngleTo180_double(
                        Math.toDegrees(this.getDirection(predictedForward, predictedStrafe, mc.thePlayer.rotationYaw))
                );
                float difference = (float) Math.abs(MathHelper.wrapAngleTo180_double(angle - predictedAngle));

                if (difference < closestDifference) {
                    closestDifference = difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        mc.thePlayer.movementInput.moveForward = closestForward;
        mc.thePlayer.movementInput.moveStrafe = closestStrafe;
    }

    private void fixMovement(float yaw, float playerYaw) {
        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe = mc.thePlayer.movementInput.moveStrafe;

        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(this.direction(playerYaw, forward, strafe)));

        float closestForward = 0.0F;
        float closestStrafe = 0.0F;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
                if (predictedForward == 0.0F && predictedStrafe == 0.0F) {
                    continue;
                }

                double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(this.direction(yaw, predictedForward, predictedStrafe)));
                float difference = (float) Math.abs(MathHelper.wrapAngleTo180_double(angle - predictedAngle));

                if (difference < closestDifference) {
                    closestDifference = difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        mc.thePlayer.movementInput.moveForward = closestForward;
        mc.thePlayer.movementInput.moveStrafe = closestStrafe;
    }

    private double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0D) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (moveForward < 0.0D) {
            forward = -0.5F;
        } else if (moveForward > 0.0D) {
            forward = 0.5F;
        }

        if (moveStrafing > 0.0D) {
            rotationYaw -= 90.0F * forward;
        }
        if (moveStrafing < 0.0D) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    private double getDirection(float moveForward, float moveStrafing, float rotationYaw) {
        if (moveForward < 0.0F) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (moveForward < 0.0F) {
            forward = -0.5F;
        } else if (moveForward > 0.0F) {
            forward = 0.5F;
        }

        if (moveStrafing > 0.0F) {
            rotationYaw -= 70.0F * forward;
        }
        if (moveStrafing < 0.0F) {
            rotationYaw += 70.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    @Override
    public void onDisabled() {
        if (this.isBlocking) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            this.isBlocking = false;
        }

        this.target = null;
        this.lastTargetPos = null;
        this.lastStrafeYaw = 0.0D;
    }
}
