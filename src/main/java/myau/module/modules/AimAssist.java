                                float[] rotation = RotationUtil.getRotationsToBox(
                       package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.util.*;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();

    // --- Mode: Normal or Slinky variants (reconstructed from SlinkyAimAssist) ---
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{
            "Normal", "Slinky-Regular", "Slinky-Linear", "Slinky-LockOn", "Slinky-Silent"
    });

    // --- Normal mode properties ---
    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 3.0F, 0.0F, 10.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 0.0F, 0.0F, 10.0F);
    public final PercentProperty smoothing = new PercentProperty("smoothing", 50);

    // --- Slinky mode properties (reconstructed from SlinkyAimAssist) ---
    public final FloatProperty slinkyHorSpeed = new FloatProperty("slinky-hor-speed", 1.0F, 0.0F, 10.0F);
    public final FloatProperty slinkyVerSpeed = new FloatProperty("slinky-ver-speed", 1.0F, 0.0F, 10.0F);
    public final BooleanProperty slinkyLockOnVertical = new BooleanProperty("slinky-lock-vertical", false);
    public final BooleanProperty slinkySilentMoveCorrect = new BooleanProperty("slinky-silent-move-correct", false);
    public final BooleanProperty slinkySilentIgnoreManualAim = new BooleanProperty("slinky-silent-ignore-manual-aim", false);
    public final FloatProperty slinkyPredict = new FloatProperty("slinky-predict", 0.0F, 0.0F, 5.0F);
    public final FloatProperty slinkyRandomization = new FloatProperty("slinky-randomization", 0.0F, 0.0F, 5.0F);
    public final BooleanProperty slinkyVerHitboxCorrect = new BooleanProperty("slinky-ver-hitbox", false);
    public final BooleanProperty slinkyHorHitboxCorrect = new BooleanProperty("slinky-hor-hitbox", false);
    public final BooleanProperty slinkyAllowExceedFov = new BooleanProperty("slinky-allow-exceed-fov", false);
    public final BooleanProperty slinkyRequireSprint = new BooleanProperty("slinky-require-sprint", false);
    public final BooleanProperty slinkyRequireMousePressed = new BooleanProperty("slinky-require-mouse-pressed", false);
    public final BooleanProperty slinkyRequireMouseMoved = new BooleanProperty("slinky-require-mouse-moved", false);
    public final BooleanProperty slinkyDisableOnBlockBreak = new BooleanProperty("slinky-disable-on-block-break", false);
    public final BooleanProperty slinkyRequireWeapon = new BooleanProperty("slinky-require-weapon", false);
    public final BooleanProperty slinkyIgnoreInvis = new BooleanProperty("slinky-ignore-invis", false);
    public final BooleanProperty slinkyRequireLineOfSight = new BooleanProperty("slinky-require-line-of-sight", false);
    public final ModeProperty slinkySortBy = new ModeProperty("slinky-sort-by", 0, new String[]{"Distance", "Health", "HurtTime", "AimAngle"});

    // --- Shared properties ---
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 90, 30, 360);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponOnly::getValue);
    public final BooleanProperty botChecks = new BooleanProperty("bot-check", true);
    public final BooleanProperty team = new BooleanProperty("teams", true);

    // --- Slinky internal state ---
    private long lastAimTime = 0;
    private EntityPlayer lockedTarget = null;
    private float lastYaw = 0, lastPitch = 0;

    // ========================== Normal Mode ==========================

    private boolean isValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer != mc.thePlayer && entityPlayer != mc.thePlayer.ridingEntity) {
            if (entityPlayer == mc.getRenderViewEntity() || entityPlayer == mc.getRenderViewEntity().ridingEntity) {
                return false;
            } else if (entityPlayer.deathTime > 0) {
                return false;
            } else if (RotationUtil.distanceToEntity(entityPlayer) > (double) this.range.getValue()) {
                return false;
            } else if (RotationUtil.angleToEntity(entityPlayer) > (float) this.fov.getValue()) {
                return false;
            } else if (RotationUtil.rayTrace(entityPlayer) != null) {
                return false;
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return false;
            } else {
                return (!this.team.getValue() || !TeamUtil.isSameTeam(entityPlayer)) && (!this.botChecks.getValue() || !TeamUtil.isBot(entityPlayer));
            }
        } else {
            return false;
        }
    }

    private boolean isInReach(EntityPlayer entityPlayer) {
        Reach reach = (Reach) Myau.moduleManager.modules.get(Reach.class);
        double distance = reach.isEnabled() ? (double) reach.range.getValue() : 3.0;
        return RotationUtil.distanceToEntity(entityPlayer) <= distance;
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    // ========================== Slinky Mode ==========================

    /** 100ms throttle (same as native 0x7ff86fb69800) */
    private boolean isTimeToAim() {
        long now = System.currentTimeMillis();
        if (now - lastAimTime >= 100) { lastAimTime = now; return true; }
        return false;
    }

    /** Requirement gates (native 0x7ff86fb69f80) */
    private boolean checkSlinkyRequirements() {
        // disable_on_block_break: native checks block break state before aiming
        if (slinkyDisableOnBlockBreak.getValue() && isLookingAtBlock()) return false;
        if (slinkyRequireSprint && !mc.thePlayer.isSprinting()) return false;
        if (slinkyRequireMousePressed && !mc.gameSettings.keyBindAttack.isKeyDown()) return false;
        if (slinkyRequireMouseMoved && !slinkyMouseMoved()) return false;
        if (slinkyRequireWeapon && !ItemUtil.isHoldingSword()) return false;
        return true;
    }

    /** Whether the player's rotation changed since last tick (mouse moved). */
    private boolean slinkyMouseMoved() {
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        boolean moved = yaw != lastYaw || pitch != lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;
        return moved;
    }

    /** Select best target sorted by slinkySortBy (native get_target_entity @ 0x7ff86fb63170). */
    private EntityPlayer selectSlinkyTarget() {
        EntityPlayer best = null;
        double bestScore = Double.MAX_VALUE;
        double bestDist = Double.MAX_VALUE;
        double rangeSq = range.getValue() * range.getValue();
        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        int sortIndex = slinkySortBy.getValue();

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer e = (EntityPlayer) obj;
            if (e == mc.thePlayer) continue;
            if (e.deathTime > 0) continue;
            if (slinkyIgnoreInvis && e.isInvisible()) continue;

            double dx = e.posX - px, dy = e.posY - py, dz = e.posZ - pz;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > rangeSq) continue;

            double dist = Math.sqrt(distSq);
            double score;

            switch (sortIndex) {
                case 1: // Health
                    score = TeamUtil.getHealthScore(e);
                    break;
                case 2: // HurtTime
                    score = -e.hurtResistantTime;
                    break;
                case 3: // FOV/AimAngle
                    score = RotationUtil.angleToEntity(e);
                    break;
                case 0: // Distance
                default:
                    score = dist;
                    break;
            }

            // tie-break: if scores equal, prefer closer target
            if (score < bestScore || (score == bestScore && dist < bestDist)) {
                bestScore = score;
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    /** Check if existing target is still valid. */
    private boolean isSlinkyTargetValid(EntityPlayer e) {
        if (e == null || e.deathTime > 0) return false;
        if (slinkyIgnoreInvis && e.isInvisible()) return false;
        return mc.thePlayer.getDistanceToEntity(e) <= range.getValue();
    }

    /** Compute aiming angles (native 0x7ff86fb49f60) with prediction and hitbox correction. */
    private float[] computeSlinkyAngles(EntityPlayer target) {
        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        double tx = target.posX, ty = target.posY, tz = target.posZ;

        // Predict: advance target position by velocity * predict ticks
        if (slinkyPredict.getValue() > 0) {
            double vx = target.posX - target.prevPosX;
            double vy = target.posY - target.prevPosY;
            double vz = target.posZ - target.prevPosZ;
            double pred = slinkyPredict.getValue();
            tx += vx * pred;
            ty += vy * pred;
            tz += vz * pred;
        }

        // Hitbox correction: aim at eye level (~1.62) rather than feet
        if (slinkyVerHitboxCorrect.getValue()) ty += 1.62;
        if (slinkyHorHitboxCorrect.getValue()) py += 1.62;

        double dx = tx - px, dy = ty - py, dz = tz - pz;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                (float) Math.toDegrees(Math.atan2(dx, dz)),
                (float) Math.toDegrees(Math.atan2(dy, horiz))
        };
    }

    /** FOV gate: skip if target is outside the configured fov cone. */
    private boolean isWithinSlinkyFov(float targetYaw) {
        float curYaw = mc.thePlayer.rotationYaw;
        return Math.abs(normalizeAngle(targetYaw - curYaw)) <= (float) fov.getValue();
    }

    /** Apply angles with smoothing (reconstructed from Slinky's angle_apply @ 0x7ff86fb6bf40). */
    private void applySlinkyAngles(float[] angles, String modeName) {
        float targetYaw = angles[0];
        float targetPitch = angles[1];
        float curYaw = mc.thePlayer.rotationYaw;
        float curPitch = mc.thePlayer.rotationPitch;

        float dyaw = normalizeAngle(targetYaw - curYaw);
        float dpitch = slinkyLockOnVertical.getValue() ? normalizeAngle(targetPitch - curPitch) : 0f;

        // Randomization jitter
        if (slinkyRandomization.getValue() > 0) {
            float jitter = (float) (slinkyRandomization.getValue() * (Math.random() * 2.0 - 1.0));
            dyaw += jitter;
            dpitch += jitter;
        }

        float finalYaw, finalPitch;

        switch (modeName) {
            case "Slinky-Silent": {
                // Silent: full correction via setAngles (silent_move_correct gate)
                if (slinkySilentMoveCorrect.getValue()) {
                    finalYaw = curYaw + dyaw;
                    finalPitch = curPitch + dpitch;
                } else {
                    return; // no correction when silent_move_correct is false
                }
                break;
            }
            case "Slinky-LockOn": {
                // LockOn: strong lock, larger constant step
                float step = (float) Math.min(1.0, 0.2 + slinkyHorSpeed.getValue() * 0.15);
                finalYaw = curYaw + dyaw * step;
                finalPitch = curPitch + dpitch * step;
                break;
            }
            case "Slinky-Linear": {
                // Linear: constant-rate interpolation
                float step = (float) (0.05 * (slinkyHorSpeed.getValue() + slinkyVerSpeed.getValue()));
                finalYaw = curYaw + dyaw * step;
                finalPitch = curPitch + dpitch * step;
                break;
            }
            case "Slinky-Regular":
            default: {
                // Regular: proportional smoothing, hor_speed for yaw, ver_speed for pitch
                float yawStep = (float) (slinkyHorSpeed.getValue() * 0.1);
                float pitchStep = (float) (slinkyVerSpeed.getValue() * 0.1);
                finalYaw = curYaw + dyaw * yawStep;
                finalPitch = curPitch + dpitch * pitchStep;
                break;
            }
        }

        Myau.rotationManager.setRotation(finalYaw, finalPitch, 0, false);
    }

    private float normalizeAngle(float a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    // ========================== Tick Handler ==========================

    public AimAssist() {
        super("AimAssist", false , false, "Assists your aim by subtly adjusting your view towards nearby targets when attacking.");
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.currentScreen == null) return;

        String currentMode = this.mode.getModeString();
        if ("Normal".equals(currentMode)) {
            tickNormal();
        } else if (currentMode.startsWith("Slinky-")) {
            tickSlinky();
        }
    }

    private void tickNormal() {
        if (!(Boolean) this.weaponOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            boolean attacking = PlayerUtil.isAttacking();
            if (!attacking || !this.isLookingAtBlock()) {
                if (attacking || !this.timer.hasTimeElapsed(350L)) {
                    List<EntityPlayer> inRange = mc.theWorld
                            .loadedEntityList
                            .stream()
                            .filter(entity -> entity instanceof EntityPlayer)
                            .map(entity -> (EntityPlayer) entity)
                            .filter(this::isValidTarget)
                            .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                            .collect(Collectors.toList());
                    if (!inRange.isEmpty()) {
                        if (inRange.stream().anyMatch(this::isInReach)) {
                            inRange.removeIf(entityPlayer -> !this.isInReach(entityPlayer));
                        }
                        EntityPlayer player = inRange.get(0);
                        if (!(RotationUtil.distanceToEntity(player) <= 0.0)) {
                            AxisAlignedBB axisAlignedBB = player.getEntityBoundingBox();
                            double collisionBorderSize = player.getCollisionBorderSize();
                            float[] rotation = RotationUtil.getRotationsToBox(
                                    axisAlignedBB.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize),
                                    mc.thePlayer.rotationYaw,
                                    mc.thePlayer.rotationPitch,
                                    180.0F,
                                    (float) this.smoothing.getValue() / 100.0F
                            );
                            float yaw = Math.min(Math.abs(this.hSpeed.getValue()), 10.0F);
                            float pitch = Math.min(Math.abs(this.vSpeed.getValue()), 10.0F);
                            Myau.rotationManager
                                    .setRotation(
                                            mc.thePlayer.rotationYaw + (rotation[0] - mc.thePlayer.rotationYaw) * 0.1F * yaw,
                                            mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch,
                                            0,
                                            false
                                    );
                        }
                    }
                }
            }
        }
    }

    private void tickSlinky() {
        // 1. 100ms CPS throttle
        if (!isTimeToAim()) return;

        // 2. Requirement gates
        if (!checkSlinkyRequirements()) return;

        // 3. Target acquisition
        EntityPlayer target = lockedTarget;
        if (target == null || !isSlinkyTargetValid(target)) {
            target = selectSlinkyTarget();
        }
        lockedTarget = target;
        if (target == null) return;

        // 4. Line of sight check
        if (slinkyRequireLineOfSight && RotationUtil.rayTrace(target) != null) return;

        // 5. Compute angles
        float[] angles = computeSlinkyAngles(target);

        // 6. Silent ignore manual aim: skip silent correction while player aims manually
        String modeName = this.mode.getModeString();
        if ("Slinky-Silent".equals(modeName) && slinkySilentIgnoreManualAim.getValue() && slinkyMouseMoved()) return;

        // 7. FOV gate
        if (!slinkyAllowExceedFov.getValue() && !isWithinSlinkyFov(angles[0])) return;

        // 8. Apply angles (dispatch by Slinky sub-mode)
        applySlinkyAngles(angles, modeName);
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode() && !Myau.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            this.timer.reset();
        }
    }
}
                 axisAlignedBB.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize),
                                        mc.thePlayer.rotationYaw,
                                        mc.thePlayer.rotationPitch,
                                        180.0F,
                                        (float) this.smoothing.getValue() / 100.0F
                                );
                                float yaw = Math.min(Math.abs(this.hSpeed.getValue()), 10.0F);
                                float pitch = Math.min(Math.abs(this.vSpeed.getValue()), 10.0F);
                                Myau.rotationManager
                                        .setRotation(
                                                mc.thePlayer.rotationYaw + (rotation[0] - mc.thePlayer.rotationYaw) * 0.1F * yaw,
                                                mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch,
                                                0,
                                                false
                                        );
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode() && !Myau.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            this.timer.reset();
        }
    }
}
