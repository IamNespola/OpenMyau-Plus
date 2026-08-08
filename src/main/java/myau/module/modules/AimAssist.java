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
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Normal", "Slinky-Regular", "Slinky-Linear", "Slinky-LockOn", "Slinky-Silent"});
    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 3.0F, 0.0F, 10.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 0.0F, 0.0F, 10.0F);
    public final PercentProperty smoothing = new PercentProperty("smoothing", 50);
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
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 90, 30, 360);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponOnly::getValue);
    public final BooleanProperty botChecks = new BooleanProperty("bot-check", true);
    public final BooleanProperty team = new BooleanProperty("teams", true);
    private long lastAimTime = 0;
    private EntityPlayer lockedTarget = null;
    private float lastYaw = 0, lastPitch = 0;

    private boolean isValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer != mc.thePlayer && entityPlayer != mc.thePlayer.ridingEntity) {
            if (entityPlayer == mc.getRenderViewEntity() || entityPlayer == mc.getRenderViewEntity().ridingEntity) return false;
            else if (entityPlayer.deathTime > 0) return false;
            else if (RotationUtil.distanceToEntity(entityPlayer) > (double) this.range.getValue()) return false;
            else if (RotationUtil.angleToEntity(entityPlayer) > (float) this.fov.getValue()) return false;
            else if (RotationUtil.rayTrace(entityPlayer) != null) return false;
            else if (TeamUtil.isFriend(entityPlayer)) return false;
            else return (!this.team.getValue() || !TeamUtil.isSameTeam(entityPlayer)) && (!this.botChecks.getValue() || !TeamUtil.isBot(entityPlayer));
        } else return false;
    }

    private boolean isInReach(EntityPlayer entityPlayer) {
        Reach reach = (Reach) Myau.moduleManager.modules.get(Reach.class);
        double distance = reach.isEnabled() ? (double) reach.range.getValue() : 3.0;
        return RotationUtil.distanceToEntity(entityPlayer) <= distance;
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    private boolean isTimeToAim() {
        long now = System.currentTimeMillis();
        if (now - lastAimTime >= 100) { lastAimTime = now; return true; }
        return false;
    }

    private boolean checkSlinkyRequirements() {
        if (slinkyDisableOnBlockBreak.getValue() && isLookingAtBlock()) return false;
        if (slinkyRequireSprint.getValue() && !mc.thePlayer.isSprinting()) return false;
        if (slinkyRequireMousePressed.getValue() && !mc.gameSettings.keyBindAttack.isKeyDown()) return false;
        if (slinkyRequireMouseMoved.getValue() && !slinkyMouseMoved()) return false;
        if (slinkyRequireWeapon.getValue() && !ItemUtil.isHoldingSword()) return false;
        return true;
    }

    private boolean slinkyMouseMoved() {
        float yaw = mc.thePlayer.rotationYaw;
        float pitch = mc.thePlayer.rotationPitch;
        boolean moved = yaw != lastYaw || pitch != lastPitch;
        lastYaw = yaw;
        lastPitch = pitch;
        return moved;
    }

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
            if (slinkyIgnoreInvis.getValue() && e.isInvisible()) continue;
            double dx = e.posX - px, dy = e.posY - py, dz = e.posZ - pz;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > rangeSq) continue;
            double dist = Math.sqrt(distSq);
            double score;
            switch (sortIndex) {
                case 1: score = TeamUtil.getHealthScore(e); break;
                case 2: score = -e.hurtResistantTime; break;
                case 3: score = RotationUtil.angleToEntity(e); break;
                case 0: default: score = dist; break;
            }
            if (score < bestScore || (score == bestScore && dist < bestDist)) {
                bestScore = score; bestDist = dist; best = e;
            }
        }
        return best;
    }

    private boolean isSlinkyTargetValid(EntityPlayer e) {
        if (e == null || e.deathTime > 0) return false;
        if (slinkyIgnoreInvis.getValue() && e.isInvisible()) return false;
        return mc.thePlayer.getDistanceToEntity(e) <= range.getValue();
    }

    private float[] computeSlinkyAngles(EntityPlayer target) {
        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        double tx = target.posX, ty = target.posY, tz = target.posZ;
        if (slinkyPredict.getValue() > 0) {
            double vx = target.posX - target.prevPosX;
            double vy = target.posY - target.prevPosY;
            double vz = target.posZ - target.prevPosZ;
            double pred = slinkyPredict.getValue();
            tx += vx * pred; ty += vy * pred; tz += vz * pred;
        }
        if (slinkyVerHitboxCorrect.getValue()) ty += 1.62;
        if (slinkyHorHitboxCorrect.getValue()) py += 1.62;
        double dx = tx - px, dy = ty - py, dz = tz - pz;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                (float) Math.toDegrees(Math.atan2(dx, dz)),
                (float) Math.toDegrees(Math.atan2(dy, horiz))
        };
    }

    private boolean isWithinSlinkyFov(float targetYaw) {
        float curYaw = mc.thePlayer.rotationYaw;
        return Math.abs(normalizeAngle(targetYaw - curYaw)) <= (float) fov.getValue();
    }

    private void applySlinkyAngles(float[] angles, String modeName) {
        float targetYaw = angles[0], targetPitch = angles[1];
        float curYaw = mc.thePlayer.rotationYaw, curPitch = mc.thePlayer.rotationPitch;
        float dyaw = normalizeAngle(targetYaw - curYaw);
        float dpitch = slinkyLockOnVertical.getValue() ? normalizeAngle(targetPitch - curPitch) : 0f;
        if (slinkyRandomization.getValue() > 0) {
            float jitter = (float) (slinkyRandomization.getValue() * (Math.random() * 2.0 - 1.0));
            dyaw += jitter; dpitch += jitter;
        }
        float finalYaw, finalPitch;
        switch (modeName) {
            case "Slinky-Silent": {
                if (slinkySilentMoveCorrect.getValue()) { finalYaw = curYaw + dyaw; finalPitch = curPitch + dpitch; }
                else return;
                break;
            }
            case "Slinky-LockOn": {
                float step = (float) Math.min(1.0, 0.2 + slinkyHorSpeed.getValue() * 0.15);
                finalYaw = curYaw + dyaw * step; finalPitch = curPitch + dpitch * step;
                break;
            }
            case "Slinky-Linear": {
                float step = (float) (0.05 * (slinkyHorSpeed.getValue() + slinkyVerSpeed.getValue()));
                finalYaw = curYaw + dyaw * step; finalPitch = curPitch + dpitch * step;
                break;
            }
            case "Slinky-Regular":
            default: {
                float yawStep = (float) (slinkyHorSpeed.getValue() * 0.1);
                float pitchStep = (float) (slinkyVerSpeed.getValue() * 0.1);
                finalYaw = curYaw + dyaw * yawStep; finalPitch = curPitch + dpitch * pitchStep;
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

    public AimAssist() {
        super("AimAssist", false, false, "Assists your aim by subtly adjusting your view towards nearby targets when attacking.");
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.currentScreen == null) return;
        String currentMode = this.mode.getModeString();
        if ("Normal".equals(currentMode)) tickNormal();
        else if (currentMode.startsWith("Slinky-")) tickSlinky();
    }

    private void tickNormal() {
        if (!(Boolean) this.weaponOnly.getValue() || ItemUtil.hasRawUnbreakingEnchant() || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            boolean attacking = PlayerUtil.isAttacking();
            if (!attacking || !this.isLookingAtBlock()) {
                if (attacking || !this.timer.hasTimeElapsed(350L)) {
                    List<EntityPlayer> inRange = mc.theWorld.loadedEntityList.stream()
                            .filter(entity -> entity instanceof EntityPlayer).map(entity -> (EntityPlayer) entity)
                            .filter(this::isValidTarget)
                            .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                            .collect(Collectors.toList());
                    if (!inRange.isEmpty()) {
                        if (inRange.stream().anyMatch(this::isInReach)) inRange.removeIf(entityPlayer -> !this.isInReach(entityPlayer));
                        EntityPlayer player = inRange.get(0);
                        if (!(RotationUtil.distanceToEntity(player) <= 0.0)) {
                            AxisAlignedBB axisAlignedBB = player.getEntityBoundingBox();
                            double collisionBorderSize = player.getCollisionBorderSize();
                            float[] rotation = RotationUtil.getRotationsToBox(
                                    axisAlignedBB.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize),
                                    mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 180.0F,
                                    (float) this.smoothing.getValue() / 100.0F);
                            float yaw = Math.min(Math.abs(this.hSpeed.getValue()), 10.0F);
                            float pitch = Math.min(Math.abs(this.vSpeed.getValue()), 10.0F);
                            Myau.rotationManager.setRotation(
                                    mc.thePlayer.rotationYaw + (rotation[0] - mc.thePlayer.rotationYaw) * 0.1F * yaw,
                                    mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch, 0, false);
                        }
                    }
                }
            }
        }
    }

    private void tickSlinky() {
        if (!isTimeToAim()) return;
        if (!checkSlinkyRequirements()) return;
        EntityPlayer target = lockedTarget;
        if (target == null || !isSlinkyTargetValid(target)) target = selectSlinkyTarget();
        lockedTarget = target;
        if (target == null) return;
        if (slinkyRequireLineOfSight.getValue() && RotationUtil.rayTrace(target) != null) return;
        float[] angles = computeSlinkyAngles(target);
        String modeName = this.mode.getModeString();
        if ("Slinky-Silent".equals(modeName) && slinkySilentIgnoreManualAim.getValue() && slinkyMouseMoved()) return;
        if (!slinkyAllowExceedFov.getValue() && !isWithinSlinkyFov(angles[0])) return;
        applySlinkyAngles(angles, modeName);
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode() && !Myau.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            this.timer.reset();
        }
    }
}
