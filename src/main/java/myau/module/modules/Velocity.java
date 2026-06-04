package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.mixin.*;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.RandomUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import myau.util.rotation.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{
            "Simple", "AAC", "AACPush", "AACZero", "AACv4",
            "Reverse", "SmoothReverse", "Jump", "Glitch", "Legit",
            "Vulcan", "MatrixReduce", "Matrix", "Intave",
            "GrimC03", "Hypixel", "HypixelAir", "BlockSMC", "GrimCombat",
            "Polar", "MatrixNoXZ", "Intave13", "JumpReset", "Intave14",
            "HypixelPrediction", "GrimReduce", "IntaveReduce"
    });

    public final FloatProperty horizontal = new FloatProperty("Horizontal", 0.0f, -2.0f, 2.0f, () -> mode.getValue() == 0 || mode.getValue() == 9);
    public final FloatProperty vertical = new FloatProperty("Vertical", 0.0f, -2.0f, 2.0f, () -> mode.getValue() == 0 || mode.getValue() == 9);

    public final IntProperty predictionChance = new IntProperty("PredChance", 100, 0, 100, () -> mode.getValue() == 24);
    public final FloatProperty predictionHorizontal = new FloatProperty("PredHorizontal", 0.0f, 0.0f, 1.0f, () -> mode.getValue() == 24);
    public final FloatProperty predictionVertical = new FloatProperty("PredVertical", 1.0f, 0.0f, 1.0f, () -> mode.getValue() == 24);
    public final BooleanProperty predictionFakeCheck = new BooleanProperty("PredFakeCheck", false, () -> mode.getValue() == 24);
    public final BooleanProperty predictionDebug = new BooleanProperty("PredDebug", false, () -> mode.getValue() == 24);

    public final FloatProperty reverseStrength = new FloatProperty("ReverseStrength", 1.0f, 0.1f, 1.0f, () -> mode.getValue() == 5);
    public final FloatProperty smoothReverseStrength = new FloatProperty("SmoothRevStrength", 0.05f, 0.02f, 0.1f, () -> mode.getValue() == 6);
    public final BooleanProperty onLook = new BooleanProperty("OnLook", false, () -> mode.getValue() == 5 || mode.getValue() == 6);
    public final FloatProperty maxAngleDiff = new FloatProperty("MaxAngle", 45.0f, 5.0f, 90.0f, () -> (mode.getValue() == 5 || mode.getValue() == 6) && onLook.getValue());

    public final FloatProperty aacPushXZ = new FloatProperty("AACPushXZ", 2.0f, 1.0f, 3.0f, () -> mode.getValue() == 2);
    public final BooleanProperty aacPushY = new BooleanProperty("AACPushY", true, () -> mode.getValue() == 2);
    public final FloatProperty aacv4Reduce = new FloatProperty("AACv4Reduce", 0.62f, 0.0f, 1.0f, () -> mode.getValue() == 4);

    public final IntProperty chance = new IntProperty("Chance", 100, 0, 100, () -> mode.getValue() == 7);
    public final IntProperty ticksUntilJump = new IntProperty("JumpTicks", 4, 0, 20, () -> mode.getValue() == 7);

    public final BooleanProperty legitDisableInAir = new BooleanProperty("DisableInAir", true, () -> mode.getValue() == 9);
    public final BooleanProperty legitJumpInInv = new BooleanProperty("Jump in inv", false, () -> mode.getValue() == 9);
    public final ModeProperty legitJumpDelayMode = new ModeProperty("Jump delay mode", 1, new String[]{"Delay", "Chance"}, () -> false);
    public final IntProperty legitMinDelay = new IntProperty("Min delay", 0, 0, 150, () -> false);
    public final IntProperty legitMaxDelay = new IntProperty("Max delay", 0, 0, 150, () -> false);
    public final IntProperty legitChance = new IntProperty("Chance", 80, 0, 100, () -> mode.getValue() == 9);
    public final BooleanProperty legitTargetNearbyCheck = new BooleanProperty("Target nearby check", false, () -> false);
    public final BooleanProperty legitIgnoreLiquid = new BooleanProperty("Ignore liquid", true, () -> false);
    public final BooleanProperty legitSkipJumpWithBoost = new BooleanProperty("Skip jump if Jump Boost", true, () -> false);

    public final BooleanProperty matrixDebug = new BooleanProperty("Debug", false, () -> mode.getValue() == 12);

    public final FloatProperty intaveXZOnHit = new FloatProperty("XZ on hit", 0.6f, 0.0f, 1.0f, () -> mode.getValue() == 13);
    public final FloatProperty intaveXZOnSprintHit = new FloatProperty("XZ on sprint hit", 0.6f, 0.0f, 1.0f, () -> mode.getValue() == 13);
    public final BooleanProperty intaveReduceUnnecessarySlowdown = new BooleanProperty("Reduce unnecessary slowdown", false, () -> mode.getValue() == 13);
    public final IntProperty intaveChance = new IntProperty("Chance", 100, 0, 100, () -> mode.getValue() == 13);
    public final BooleanProperty intaveJump = new BooleanProperty("Jump", false, () -> mode.getValue() == 13);
    public final BooleanProperty intaveJumpInInv = new BooleanProperty("Jump in inv", false, () -> mode.getValue() == 13 && intaveJump.getValue());
    public final IntProperty intaveJumpChance = new IntProperty("Jump chance", 80, 0, 100, () -> mode.getValue() == 13 && intaveJump.getValue());
    public final BooleanProperty intaveNotWhileSpeed = new BooleanProperty("Not while speed", false, () -> mode.getValue() == 13);
    public final BooleanProperty intaveNotWhileJumpBoost = new BooleanProperty("Not while jump boost", false, () -> mode.getValue() == 13);
    public final BooleanProperty intaveDebug = new BooleanProperty("Debug", false, () -> mode.getValue() == 13);
    public final FloatProperty intaveReduceFactor = new FloatProperty("Factor", 0.6f, 0.6f, 1.0f, () -> mode.getValue() == 26);
    public final IntProperty intaveReduceHurtTime = new IntProperty("HurtTime", 9, 1, 10, () -> mode.getValue() == 26);

    public final IntProperty jumpResetChance = new IntProperty("JRChance", 100, 0, 100, () -> mode.getValue() == 22);
    public final BooleanProperty jumpByReceivedHits = new BooleanProperty("JumpByHits", false, () -> mode.getValue() == 22);
    public final IntProperty hitsUntilJump = new IntProperty("HitsUntilJump", 2, 0, 10, () -> mode.getValue() == 22 && jumpByReceivedHits.getValue());
    public final BooleanProperty jumpByDelay = new BooleanProperty("JumpByDelay", true, () -> mode.getValue() == 22);
    public final IntProperty jumpResetTicks = new IntProperty("UntilJump", 2, 0, 20, () -> mode.getValue() == 22 && jumpByDelay.getValue());

    public final FloatProperty grimRange = new FloatProperty("GrimRange", 3.5f, 0.0f, 6.0f, () -> mode.getValue() == 18);
    public final IntProperty grimAttacks = new IntProperty("GrimAttacks", 12, 1, 16, () -> mode.getValue() == 18);

    public final FloatProperty intave14Timer1 = new FloatProperty("Intave14-T1", 0.3f, 0.1f, 2.0f, () -> mode.getValue() == 23);
    public final FloatProperty intave14Timer2 = new FloatProperty("Intave14-T2", 5.0f, 1.0f, 10.0f, () -> mode.getValue() == 23);
    public final BooleanProperty grimReduceRequireSwing = new BooleanProperty("GrimReqSwing", false, () -> mode.getValue() == 25);

    private final TimerUtil velocityTimer = new TimerUtil();
    private boolean hasReceivedVelocity = false;
    private boolean jump = false;
    private int limitUntilJump = 0;
    private int intaveTick = 0;
    private int intaveDamageTick = 0;
    private long lastAttackTime = 0;
    private boolean vulcanTrans = false;
    private boolean hypixelAbsorbed = false;
    private boolean matrixAbsorbed = false;
    private boolean attacked = false;
    private boolean matrixReduced = false;
    private boolean intaveReduced = false;
    private int timerTicks = 0;

    private int chanceCounter = 0;
    private boolean allowNext = true;
    private float reduceYaw = 0;
    private boolean shouldRotate = false;
    private int attackTimer = -1;
    private int lastHurtTime = 0;
    private boolean jumpFlag = false;
    private boolean jumpResetFallDamage = false;
    private int grimReduceTicks = 0;

    public Velocity() {
        super("Velocity", false, false, "We Use Ur Dih to Remove KnockBack :D");
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer != null) {
            ((IAccessorEntityPlayer) mc.thePlayer).setSpeedInAir(0.02F);
        }
        ((IAccessorTimer) ((IAccessorMinecraft) mc).getTimer()).setTimerSpeed(1.0f);
        timerTicks = 0;
        limitUntilJump = 0;
        reset();

        chanceCounter = 0;
        allowNext = true;
        matrixReduced = false;
        intaveReduced = false;
        shouldRotate = false;
        attackTimer = -1;
        lastHurtTime = 0;
        jumpFlag = false;
        jumpResetFallDamage = false;
        grimReduceTicks = 0;
    }

    private void reset() {
        hasReceivedVelocity = false;
        attacked = false;
        hypixelAbsorbed = false;
        matrixAbsorbed = false;
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null || player.isInWater() || player.isInLava() || ((IAccessorEntity) player).getIsInWeb())
                return;

            switch (mode.getValue()) {
                case 24:
                    int hurtTime = player.hurtTime;
                    if (hurtTime > lastHurtTime) {

                        KillAura aura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                        EntityLivingBase target = null;
                        if (aura != null && aura.isEnabled() && aura.target != null) {
                            target = aura.target.getEntity();
                        }

                        if (target == null) {
                            if (shouldRotate) {

                                Rotation currentRot = new Rotation(player.rotationYaw, player.rotationPitch);
                                float targetYaw = reduceYaw;
                                float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - currentRot.yaw);

                                float newYaw = currentRot.yaw + yawDiff * 0.5f;
                                player.rotationYaw = newYaw;
                                player.rotationYawHead = newYaw;

                                if (player.onGround) {
                                    player.jump();
                                }
                                shouldRotate = false;
                            }
                        } else {
                            double distance = player.getDistanceToEntity(target);

                            if (distance > 3.0) {
                                if (player.onGround) {
                                    player.jump();
                                }
                            } else {
                                if (player.onGround) {
                                    player.jump();
                                }
                                attackTimer = 1;
                            }
                        }
                    }

                    if (attackTimer == 0) {
                        KillAura aura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                        EntityLivingBase target = null;
                        if (aura != null && aura.isEnabled() && aura.target != null) {
                            target = aura.target.getEntity();
                        }

                        if (target != null && player.getDistanceToEntity(target) <= 3.0) {
                            player.swingItem();
                            mc.playerController.attackEntity(player, target);
                        }
                        attackTimer = -1;
                    }

                    if (attackTimer > 0) {
                        attackTimer--;
                    }
                    lastHurtTime = hurtTime;
                    break;

                case 8:
                    if (hasReceivedVelocity) {
                        player.noClip = true;
                        if (player.hurtTime == 7) player.motionY = 0.4;
                        hasReceivedVelocity = false;
                    }
                    break;
                case 5:
                    if (hasReceivedVelocity) {
                        if (!player.onGround) {
                            if (onLook.getValue()) {
                                KillAura aura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                                Entity target = aura.target != null ? aura.target.getEntity() : null;
                                if (target != null) {
                                    Rotation playerRot = new Rotation(player.rotationYaw, player.rotationPitch);
                                    Rotation targetRot = getRotations(target);
                                    if (getRotationDifference(playerRot, targetRot) > maxAngleDiff.getValue()) {
                                        return;
                                    }
                                }
                            }
                            MoveUtil.setSpeed(MoveUtil.getSpeed() * reverseStrength.getValue());
                        } else if (velocityTimer.hasTimeElapsed(80)) {
                            hasReceivedVelocity = false;
                        }
                    }
                    break;
                case 6:
                    if (hasReceivedVelocity) {
                        KillAura aura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                        Entity target = aura.target != null ? aura.target.getEntity() : null;

                        if (target == null) {
                            ((IAccessorEntityPlayer) player).setSpeedInAir(0.02F);
                        } else if (onLook.getValue() && getRotationDifference(new Rotation(player.rotationYaw, player.rotationPitch), getRotations(target)) > maxAngleDiff.getValue()) {
                            hasReceivedVelocity = false;
                            ((IAccessorEntityPlayer) player).setSpeedInAir(0.02F);
                        } else {
                            if (!player.onGround) {
                                ((IAccessorEntityPlayer) player).setSpeedInAir(smoothReverseStrength.getValue());
                            } else if (velocityTimer.hasTimeElapsed(80)) {
                                hasReceivedVelocity = false;
                                ((IAccessorEntityPlayer) player).setSpeedInAir(0.02F);
                            }
                        }
                    }
                    break;
                case 1:
                    if (hasReceivedVelocity && velocityTimer.hasTimeElapsed(80)) {
                        player.motionX *= horizontal.getValue();
                        player.motionZ *= horizontal.getValue();
                        hasReceivedVelocity = false;
                    }
                    break;
                case 4:
                    if (player.hurtTime > 0 && !player.onGround) {
                        player.motionX *= aacv4Reduce.getValue();
                        player.motionZ *= aacv4Reduce.getValue();
                    }
                    break;
                case 2:
                    if (jump) {
                        if (player.onGround) jump = false;
                    } else {
                        if (player.hurtTime > 0 && player.motionX != 0 && player.motionZ != 0) {
                            player.onGround = true;
                        }
                        if (player.hurtResistantTime > 0 && aacPushY.getValue()) {
                            player.motionY -= 0.014999993;
                        }
                    }
                    if (player.hurtResistantTime >= 19) {
                        player.motionX /= aacPushXZ.getValue();
                        player.motionZ /= aacPushXZ.getValue();
                    }
                    break;
                case 3:
                    if (player.hurtTime > 0) {
                        if (!hasReceivedVelocity || player.onGround || player.fallDistance > 2) return;
                        player.motionY -= 1.0;
                        player.isAirBorne = true;
                        player.onGround = true;
                    } else {
                        hasReceivedVelocity = false;
                    }
                    break;
                case 13:
                    if (hasReceivedVelocity) {
                        if (!intaveNoAction() && intaveJump.getValue()) {
                            if ((intaveJumpChance.getValue() >= 100 || RandomUtil.nextInt(0, 100) < intaveJumpChance.getValue())
                                    && player.onGround && (intaveJumpInInv.getValue() || mc.currentScreen == null)) {
                                player.jump();
                            }
                        }
                        intaveReduced = false;
                        hasReceivedVelocity = false;
                    }
                    break;
                case 15:
                    if (hasReceivedVelocity && player.onGround) hypixelAbsorbed = false;
                    break;
                case 16:
                    if (hasReceivedVelocity) {
                        if (player.onGround && !((IAccessorEntityLivingBase) player).isJumping()) player.jump();
                        hasReceivedVelocity = false;
                    }
                    break;
                case 20:
                    if (hasReceivedVelocity && player.onGround) matrixAbsorbed = false;
                    break;
                case 18:
                    if (attacked) {
                        if (player.hurtTime == 0) attacked = false;
                    }
                    break;
                case 25:
                    if (grimReduceTicks > 0) {
                        grimReduceTicks--;
                        if (player.ticksExisted > 20
                                && (!grimReduceRequireSwing.getValue() || player.isSwingInProgress)
                                && !hasBadPacketState()) {
                            EntityLivingBase target = findGrimReduceTarget();
                            if (target != null) {
                                PacketUtil.sendPacket(new C0APacketAnimation());
                                mc.playerController.attackEntity(player, target);
                            }
                        }
                    }
                    break;
                case 9:
                    if (hasReceivedVelocity) {
                        handleLegitVelocity();
                        hasReceivedVelocity = false;
                    }
                    break;
                case 12:
                    if (hasReceivedVelocity) {
                        matrixReduced = false;
                        hasReceivedVelocity = false;
                    }
                    break;
                case 26:
                    if (!hasReceivedVelocity) break;
                    intaveTick++;
                    if (player.hurtTime == 2) {
                        intaveDamageTick++;
                        if (player.onGround && intaveTick % 2 == 0 && intaveDamageTick <= 10) {
                            player.jump();
                            intaveTick = 0;
                        }
                        hasReceivedVelocity = false;
                    }
                    break;
                case 22:
                    if (this.shouldJumpReset(player)) {
                        player.jump();
                        limitUntilJump = 0;
                    } else {
                        this.updateJumpResetLimit(player);
                    }
                    break;
                case 23:
                    IAccessorTimer timer = (IAccessorTimer) ((IAccessorMinecraft) mc).getTimer();
                    if (player.hurtTime == 9) timer.setTimerSpeed(intave14Timer1.getValue());
                    else if (player.hurtTime >= 3 && player.hurtTime <= 8)
                        timer.setTimerSpeed(intave14Timer2.getValue());
                    else if (player.hurtTime == 2) timer.setTimerSpeed(1.0f);
                    else timer.setTimerSpeed(1.0f);
                    break;
            }
        }

        if (event.getType() == EventType.POST && mode.getValue() == 24) {
            if (jumpFlag) {
                jumpFlag = false;
                EntityPlayerSP player = mc.thePlayer;
                if (player.onGround && player.isSprinting() && !player.isPotionActive(net.minecraft.potion.Potion.jump) && !isInLiquidOrWeb()) {
                    player.movementInput.jump = true;
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mode.getValue() == 14) {
            IAccessorTimer timer = (IAccessorTimer) ((IAccessorMinecraft) mc).getTimer();
            if (timerTicks > 0 && timer.getTimerSpeed() <= 1) {
                float speed = 0.8f + (0.2f * (20 - timerTicks) / 20);
                timer.setTimerSpeed(Math.min(speed, 1f));
                --timerTicks;
            } else if (timer.getTimerSpeed() <= 1) {
                timer.setTimerSpeed(1f);
            }
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() != player.getEntityId()) return;

            velocityTimer.reset();
            IAccessorS12PacketEntityVelocity accessor = (IAccessorS12PacketEntityVelocity) packet;
            if (mode.getValue() == 22) {
                jumpResetFallDamage = packet.getMotionX() == 0 && packet.getMotionZ() == 0 && packet.getMotionY() < 0;
            }

            switch (mode.getValue()) {
                case 24:
                    double x = (double) packet.getMotionX() / 8000.0;
                    double z = (double) packet.getMotionZ() / 8000.0;

                    if (x != 0 || z != 0) {
                        reduceYaw = (float) (Math.toDegrees(Math.atan2(-z, -x)) - 90.0);
                        shouldRotate = true;
                    }

                    if (predictionFakeCheck.getValue() && !allowNext) {
                        allowNext = true;
                        return;
                    }

                    allowNext = true;

                    chanceCounter = (chanceCounter % 100) + predictionChance.getValue();
                    if (chanceCounter >= 100) {
                        jumpFlag = true;

                        if (predictionHorizontal.getValue() > 0) {
                            player.motionX = x * predictionHorizontal.getValue();
                            player.motionZ = z * predictionHorizontal.getValue();
                        } else {
                            player.motionX = 0;
                            player.motionZ = 0;
                        }

                        if (predictionVertical.getValue() > 0) {
                            player.motionY = (double) packet.getMotionY() / 8000.0 * predictionVertical.getValue();
                        } else {
                            player.motionY = 0;
                        }

                        if (predictionDebug.getValue()) {
                            player.addChatMessage(new net.minecraft.util.ChatComponentText(
                                    String.format("Velocity (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                                            player.ticksExisted,
                                            x, (double) packet.getMotionY() / 8000.0, z
                                    )
                            ));
                        }
                    } else {
                        event.setCancelled(true);
                    }
                    break;

                case 0:
                    event.setCancelled(true);
                    if (horizontal.getValue() == 0 && vertical.getValue() == 0) return;

                    if (horizontal.getValue() != 0) {
                        player.motionX = (double) packet.getMotionX() / 8000.0 * horizontal.getValue();
                        player.motionZ = (double) packet.getMotionZ() / 8000.0 * horizontal.getValue();
                    }
                    if (vertical.getValue() != 0) {
                        player.motionY = (double) packet.getMotionY() / 8000.0 * vertical.getValue();
                    }
                    break;
                case 18:
                    if (player.isDead || player.isOnLadder() || player.isInWater() || player.isInLava()) return;
                    double hStr = Math.sqrt(packet.getMotionX() * packet.getMotionX() + packet.getMotionZ() * packet.getMotionZ());
                    if (hStr <= 1000) return;

                    Entity target = null;
                    if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
                        if (player.getDistanceToEntity(mc.objectMouseOver.entityHit) <= grimRange.getValue()) {
                            target = mc.objectMouseOver.entityHit;
                        }
                    }
                    if (target == null) {
                        KillAura aura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
                        if (aura.target != null && player.getDistanceToEntity(aura.target.getEntity()) <= grimRange.getValue()) {
                            target = aura.target.getEntity();
                        }
                    }

                    if (target != null) {
                        boolean sprinting = player.isSprinting();
                        if (!sprinting)
                            PacketUtil.sendPacket(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING));

                        for (int i = 0; i < grimAttacks.getValue(); i++) {
                            PacketUtil.sendPacket(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                            PacketUtil.sendPacket(new C0APacketAnimation());
                        }

                        if (!sprinting)
                            PacketUtil.sendPacket(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING));
                        attacked = true;
                        event.setCancelled(true);
                    }
                    break;
                case 1:
                case 5:
                case 6:
                case 3:
                case 9:
                case 13:
                case 19:
                case 21:
                case 22:
                case 23:
                case 25:
                case 26:
                    grimReduceTicks = 14;
                    hasReceivedVelocity = true;
                    break;
                case 2:
                    if (jump && player.onGround) jump = false;
                    break;
                case 7:
                    double motionX = packet.getMotionX() / 8000.0;
                    double motionZ = packet.getMotionZ() / 8000.0;
                    if (Math.abs(Math.atan2(motionX, motionZ) - Math.toRadians(player.rotationYaw)) < 2.0) {
                        hasReceivedVelocity = true;
                    }
                    break;
                case 8:
                    if (!player.onGround) return;
                    hasReceivedVelocity = true;
                    event.setCancelled(true);
                    break;
                case 11:
                    accessor.setMotionX((int) (packet.getMotionX() * 0.33));
                    accessor.setMotionZ((int) (packet.getMotionZ() * 0.33));
                    if (player.onGround) {
                        accessor.setMotionX((int) (packet.getMotionX() * 0.86));
                        accessor.setMotionZ((int) (packet.getMotionZ() * 0.86));
                    }
                    break;
                case 12:
                    hasReceivedVelocity = true;
                    break;
                case 17:
                    hasReceivedVelocity = true;
                    event.setCancelled(true);
                    PacketUtil.sendPacket(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SNEAKING));
                    PacketUtil.sendPacket(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SNEAKING));
                    break;
                case 14:
                    if (player.onGround || player.fallDistance < 0.5) {
                        hasReceivedVelocity = true;
                        event.setCancelled(true);
                    }
                    break;
                case 15:
                    hasReceivedVelocity = true;
                    if (!player.onGround) {
                        if (!hypixelAbsorbed) {
                            event.setCancelled(true);
                            hypixelAbsorbed = true;
                            return;
                        }
                    }
                    accessor.setMotionX((int) (player.motionX * 8000));
                    accessor.setMotionZ((int) (player.motionZ * 8000));
                    break;
                case 16:
                    hasReceivedVelocity = true;
                    event.setCancelled(true);
                    break;
                case 20:
                    hasReceivedVelocity = true;
                    if (!player.onGround) {
                        if (!matrixAbsorbed) {
                            event.setCancelled(true);
                            matrixAbsorbed = true;
                            return;
                        }
                    }
                    accessor.setMotionX(0);
                    accessor.setMotionZ(0);
                    break;
                case 10:
                    event.setCancelled(true);
                    break;
            }
        }

        if (event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
            IAccessorS27PacketExplosion accessor = (IAccessorS27PacketExplosion) packet;

            if (mode.getValue() == 0) {
                if (horizontal.getValue() == 0 && vertical.getValue() == 0) {
                    event.setCancelled(true);
                } else {
                    accessor.setField_149152_f(accessor.getField_149152_f() * horizontal.getValue());
                    accessor.setField_149153_g(accessor.getField_149153_g() * vertical.getValue());
                    accessor.setField_149159_h(accessor.getField_149159_h() * horizontal.getValue());
                }
            } else if (mode.getValue() == 7) {
                hasReceivedVelocity = true;
            } else if (mode.getValue() == 24) {
                if (predictionDebug.getValue()) {
                    player.addChatMessage(new net.minecraft.util.ChatComponentText(
                            String.format("Explosion (tick: %d, x: %.2f, y: %.2f, z: %.2f)",
                                    player.ticksExisted,
                                    player.motionX + (double) packet.func_149149_c(),
                                    player.motionY + (double) packet.func_149144_d(),
                                    player.motionZ + (double) packet.func_149147_e()
                            )
                    ));
                }

                if (predictionHorizontal.getValue() == 0 || predictionVertical.getValue() == 0) {
                    event.setCancelled(true);
                }
            }
        }

        if (mode.getValue() == 10) {
            if (event.getPacket() instanceof S32PacketConfirmTransaction) {
                event.setCancelled(true);
                S32PacketConfirmTransaction p = (S32PacketConfirmTransaction) event.getPacket();
                PacketUtil.sendPacket(new C0FPacketConfirmTransaction(p.getWindowId(), p.getActionNumber(), vulcanTrans));
                vulcanTrans = !vulcanTrans;
            }
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (mode.getValue() == 2) {
            jump = true;
            if (!mc.thePlayer.isCollidedVertically) event.setCancelled(true);
        } else if (mode.getValue() == 3) {
            if (mc.thePlayer.hurtTime > 0) event.setCancelled(true);
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mode.getValue() == 13) {
            if (event.getTarget() instanceof EntityLivingBase && mc.thePlayer.hurtTime > 0) {
                if (intaveNoAction()) return;
                if (intaveChance.getValue() < 100 && RandomUtil.nextInt(0, 100) >= intaveChance.getValue()) return;
                if (intaveReduceUnnecessarySlowdown.getValue() && intaveReduced) return;

                float factor = mc.thePlayer.isSprinting() ? intaveXZOnSprintHit.getValue() : intaveXZOnHit.getValue();
                mc.thePlayer.motionX *= factor;
                mc.thePlayer.motionZ *= factor;
                intaveReduced = true;

                if (intaveDebug.getValue()) {
                    mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                            String.format("Reduced %.3f %.3f", mc.thePlayer.motionX, mc.thePlayer.motionZ)
                    ));
                }
            }
        } else if (mode.getValue() == 12) {
            if (event.getTarget() instanceof EntityLivingBase && mc.thePlayer.hurtTime > 0) {
                if (matrixReduced) return;

                if (mc.thePlayer.isSprinting()) {
                    double motionX = mc.thePlayer.motionX;
                    double motionZ = mc.thePlayer.motionZ;

                    if (Math.abs(motionX) < 0.625 && Math.abs(motionZ) < 0.625) {
                        mc.thePlayer.motionX = motionX * 0.4D;
                        mc.thePlayer.motionZ = motionZ * 0.4D;
                    } else if (Math.abs(motionX) < 1.25 && Math.abs(motionZ) < 1.25) {
                        mc.thePlayer.motionX = motionX * 0.67D;
                        mc.thePlayer.motionZ = motionZ * 0.67D;
                    }
                    mc.thePlayer.setSprinting(false);

                    if (matrixDebug.getValue()) {
                        mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                                String.format("reduced %.2f %.2f", motionX - mc.thePlayer.motionX, motionZ - mc.thePlayer.motionZ)
                        ));
                    }
                }
                matrixReduced = true;
            }
        } else if (mode.getValue() == 26) {
            if (!hasReceivedVelocity) return;
            if (mc.thePlayer.hurtTime == intaveReduceHurtTime.getValue() && System.currentTimeMillis() - lastAttackTime <= 8000L) {
                mc.thePlayer.motionX *= intaveReduceFactor.getValue();
                mc.thePlayer.motionZ *= intaveReduceFactor.getValue();
            }
            lastAttackTime = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (mode.getValue() == 7 && hasReceivedVelocity) {
            if (!((IAccessorEntityLivingBase) mc.thePlayer).isJumping() && RandomUtil.nextInt(0, 100) < chance.getValue() && limitUntilJump >= ticksUntilJump.getValue() && mc.thePlayer.isSprinting() && mc.thePlayer.onGround && mc.thePlayer.hurtTime == 9) {
                mc.thePlayer.jump();
                limitUntilJump = 0;
            }
            hasReceivedVelocity = false;
        }
        if (mc.thePlayer.hurtTime == 9) limitUntilJump++;
    }

    private boolean hasBadPacketState() {
        return Myau.playerStateManager != null
                && (Myau.playerStateManager.attacking
                || Myau.playerStateManager.digging
                || Myau.playerStateManager.placing
                || Myau.playerStateManager.swapping
                || Myau.playerStateManager.swinging);
    }

    private boolean intaveNoAction() {
        return mc.thePlayer != null && mc.thePlayer.getActivePotionEffects().parallelStream()
                .anyMatch(effect -> (intaveNotWhileSpeed.getValue() && effect.getPotionID() == Potion.moveSpeed.getId())
                        || (intaveNotWhileJumpBoost.getValue() && effect.getPotionID() == Potion.jump.getId()));
    }

    private void handleLegitVelocity() {
        if (mc.thePlayer == null) return;
        if (legitDisableInAir.getValue() && !mc.thePlayer.onGround) return;
        if (mc.thePlayer.maxHurtResistantTime != mc.thePlayer.hurtResistantTime || mc.thePlayer.maxHurtResistantTime == 0) return;

        if (legitChance.getValue() >= 100 || RandomUtil.nextInt(0, 100) < legitChance.getValue()) {
            mc.thePlayer.motionX *= horizontal.getValue() / 100.0D;
            mc.thePlayer.motionZ *= horizontal.getValue() / 100.0D;
            mc.thePlayer.motionY *= vertical.getValue() / 100.0D;
        }
    }

    private boolean canLegitJump() {
        if (mc.thePlayer == null || !mc.thePlayer.onGround) return false;
        if (legitSkipJumpWithBoost.getValue() && mc.thePlayer.isPotionActive(Potion.jump)) {
            net.minecraft.potion.PotionEffect effect = mc.thePlayer.getActivePotionEffect(Potion.jump);
            return effect == null || effect.getAmplifier() < 1;
        }
        return legitJumpInInv.getValue() || mc.currentScreen == null;
    }

    private boolean isTargetNearby() {
        KillAura aura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (aura != null && aura.isEnabled() && aura.getTarget() != null && mc.thePlayer.getDistanceToEntity(aura.getTarget()) <= 4.5F) {
            return true;
        }
        return mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase
                && mc.thePlayer.getDistanceToEntity(mc.objectMouseOver.entityHit) <= 4.5F;
    }

    private boolean isValidGrimReduceTarget(EntityLivingBase target) {
        if (target == null || target == mc.thePlayer || target.isDead || target.deathTime > 0) {
            return false;
        }
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            if (TeamUtil.isFriend(player) || TeamUtil.isBot(player) || TeamUtil.isSameTeam(player)) {
                return false;
            }
        }
        return mc.thePlayer.getDistanceToEntity(target) <= 7.0F;
    }

    private EntityLivingBase findGrimReduceTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura != null) {
            EntityLivingBase killAuraTarget = killAura.getTarget();
            if (this.isValidGrimReduceTarget(killAuraTarget)) {
                return killAuraTarget;
            }
        }

        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            EntityLivingBase target = (EntityLivingBase) mc.objectMouseOver.entityHit;
            if (this.isValidGrimReduceTarget(target)) {
                return target;
            }
        }

        EntityLivingBase nearest = null;
        double nearestDistance = 7.0D;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) entity;
                if (this.isValidGrimReduceTarget(target)) {
                    double distance = mc.thePlayer.getDistanceToEntity(target);
                    if (distance <= nearestDistance) {
                        nearestDistance = distance;
                        nearest = target;
                    }
                }
            }
        }

        return nearest;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }

    private boolean shouldJumpReset(EntityPlayerSP player) {
        if (player.hurtTime != 9 || !player.onGround || !player.isSprinting() || jumpResetFallDamage) {
            return false;
        }
        if (!this.isJumpResetCooldownOver()) {
            return false;
        }
        return jumpResetChance.getValue() >= 100 || RandomUtil.nextInt(0, 100) < jumpResetChance.getValue();
    }

    private boolean isJumpResetCooldownOver() {
        if (jumpByReceivedHits.getValue()) {
            return limitUntilJump >= hitsUntilJump.getValue();
        }
        if (jumpByDelay.getValue()) {
            return limitUntilJump >= jumpResetTicks.getValue();
        }
        return true;
    }

    private void updateJumpResetLimit(EntityPlayerSP player) {
        if (jumpByReceivedHits.getValue()) {
            if (player.hurtTime == 9) {
                limitUntilJump++;
            }
            return;
        }
        limitUntilJump++;
    }

    private Rotation getRotations(Entity entity) {
        double x = entity.posX - mc.thePlayer.posX;
        double z = entity.posZ - mc.thePlayer.posZ;
        double y = entity.posY + entity.getEyeHeight() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0D / Math.PI));
        return new Rotation(yaw, pitch);
    }

    private float getRotationDifference(Rotation a, Rotation b) {
        return Math.abs(MathHelper.wrapAngleTo180_float(a.yaw - b.yaw));
    }
}