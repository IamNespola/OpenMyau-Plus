package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.Vec3;

public class HitSelect extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"SECOND", "CRITICALS", "W_TAP", "PAUSE", "ACTIVE"});
    public final ModeProperty preference = new ModeProperty("Preference", 0, new String[]{"Move speed", "KB reduction", "Critical hits"}, () -> this.mode.getValue() == 3 || this.mode.getValue() == 4);
    public final IntProperty delay = new IntProperty("Delay", 420, 300, 500, () -> this.mode.getValue() == 3 || this.mode.getValue() == 4);
    public final IntProperty chance = new IntProperty("Chance", 80, 0, 100, () -> this.mode.getValue() == 3 || this.mode.getValue() == 4);
    public final FloatProperty range = new FloatProperty("Range", 8.0F, 1.0F, 20.0F, () -> this.mode.getValue() == 3 || this.mode.getValue() == 4);

    private boolean sprintState = false;
    private boolean set = false;
    private boolean keepSprintWasEnabled = false;
    private double savedSlowdown = 0.0;

    private int blockedHits = 0;
    private int allowedHits = 0;
    private long attackTime = -1L;
    private boolean currentShouldAttack = false;

    public HitSelect() {
        super("HitSelect", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        if (event.getType() == EventType.PRE && (this.mode.getValue() == 3 || this.mode.getValue() == 4)) {
            EntityLivingBase target = this.getNearestEntityInRange();
            if (target == null) {
                this.resetState();
            } else {
                this.currentShouldAttack = false;
                if (Math.random() * 100.0D > this.chance.getValue()) {
                    this.currentShouldAttack = true;
                } else {
                    switch (this.preference.getValue()) {
                        case 1: // KB reduction
                            this.currentShouldAttack = !mc.thePlayer.onGround && mc.thePlayer.motionY < 0.0D;
                            break;
                        case 2: // Critical hits
                            this.currentShouldAttack = mc.thePlayer.hurtTime > 0 && !mc.thePlayer.onGround && this.isMoving(mc.thePlayer);
                            break;
                    }
                    if (!this.currentShouldAttack) {
                        this.currentShouldAttack = System.currentTimeMillis() - this.attackTime >= this.delay.getValue();
                    }
                }
            }
        }

        if (event.getType() == EventType.POST) {
            this.resetMotion();
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND || event.isCancelled()) {
            return;
        }

        if (event.getPacket() instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction packet = (C0BPacketEntityAction) event.getPacket();
            switch (packet.getAction()) {
                case START_SPRINTING:
                    this.sprintState = true;
                    break;
                case STOP_SPRINTING:
                    this.sprintState = false;
                    break;
            }
            return;
        }

        if (event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity use = (C02PacketUseEntity) event.getPacket();

            if (use.getAction() != C02PacketUseEntity.Action.ATTACK) {
                return;
            }

            Entity target = use.getEntityFromWorld(mc.theWorld);
            if (target == null || target instanceof EntityLargeFireball) {
                return;
            }

            if (!(target instanceof EntityLivingBase)) {
                return;
            }

            EntityLivingBase living = (EntityLivingBase) target;
            boolean allow = true;

            switch (this.mode.getValue()) {
                case 0: // SECOND
                    allow = this.prioritizeSecondHit(mc.thePlayer, living);
                    break;
                case 1: // CRITICALS
                    allow = this.prioritizeCriticalHits(mc.thePlayer);
                    break;
                case 2: // WTAP
                    allow = this.prioritizeWTapHits(mc.thePlayer, this.sprintState);
                    break;
                case 3: // PAUSE
                    allow = this.prioritizePauseHits();
                    break;
                case 4: // ACTIVE
                    allow = true;
                    break;
            }

            if (!allow) {
                event.setCancelled(true);
                this.blockedHits++;
            } else {
                this.allowedHits++;
                this.attackTime = System.currentTimeMillis();
            }
        }
    }

    private boolean prioritizeSecondHit(EntityLivingBase player, EntityLivingBase target) {
        // If target is already hurt, allow the hit
        if (target.hurtTime != 0) {
            return true;
        }

        // If player hasn't recovered from hurt time, allow the hit
        if (player.hurtTime <= player.maxHurtTime - 1) {
            return true;
        }

        // If too close, allow the hit
        double dist = player.getDistanceToEntity(target);
        if (dist < 2.5) {
            return true;
        }

        // If not moving towards each other, allow the hit
        if (!this.isMovingTowards(target, player, 60.0)) {
            return true;
        }

        if (!this.isMovingTowards(player, target, 60.0)) {
            return true;
        }

        // Block the hit and fix motion
        this.fixMotion();
        return false;
    }

    private boolean prioritizeCriticalHits(EntityLivingBase player) {
        // If on ground, allow the hit
        if (player.onGround) {
            return true;
        }

        // If hurt, allow the hit
        if (player.hurtTime != 0) {
            return true;
        }

        // If falling, allow the hit (for crits)
        if (player.fallDistance > 0.0f) {
            return true;
        }

        // Block the hit and fix motion
        this.fixMotion();
        return false;
    }

    private boolean prioritizeWTapHits(EntityLivingBase player, boolean sprinting) {
        // If against wall, allow the hit
        if (player.isCollidedHorizontally) {
            return true;
        }

        // If not moving forward, allow the hit
        if (!mc.gameSettings.keyBindForward.isKeyDown()) {
            return true;
        }

        // If already sprinting, allow the hit
        if (sprinting) {
            return true;
        }

        // Block the hit and fix motion
        this.fixMotion();
        return false;
    }

    private boolean prioritizePauseHits() {
        if (this.currentShouldAttack) {
            return true;
        }
        this.fixMotion();
        return false;
    }

    private void resetState() {
        this.currentShouldAttack = false;
    }

    private EntityLivingBase getNearestEntityInRange() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return null;
        }

        EntityLivingBase nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.isDead || living.getHealth() <= 0.0F) {
                continue;
            }
            double distance = mc.thePlayer.getDistanceToEntity(living);
            if (distance <= this.range.getValue() && distance < bestDistance) {
                nearest = living;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean isMoving(EntityLivingBase entity) {
        return Math.abs(entity.motionX) > 0.005D || Math.abs(entity.motionZ) > 0.005D;
    }

    private void fixMotion() {
        if (this.set) {
            return;
        }

        KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
        if (keepSprint == null) {
            return;
        }

        try {
            // Save the current slowdown value
            this.savedSlowdown = keepSprint.slowdown.getValue().doubleValue();
            this.keepSprintWasEnabled = keepSprint.isEnabled();

            // Temporarily enable KeepSprint silently so this internal motion fix does not spam toggles.
            if (!this.keepSprintWasEnabled) {
                keepSprint.setEnabled(true);
            }
            keepSprint.slowdown.setValue(0);

            this.set = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetMotion() {
        if (!this.set) {
            return;
        }

        KeepSprint keepSprint = (KeepSprint) Myau.moduleManager.modules.get(KeepSprint.class);
        if (keepSprint == null) {
            return;
        }

        try {
            // Restore the original slowdown value
            keepSprint.slowdown.setValue((int) this.savedSlowdown);

            // Only restore the enabled state if HitSelect changed it.
            if (!this.keepSprintWasEnabled && keepSprint.isEnabled()) {
                keepSprint.setEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.set = false;
        this.keepSprintWasEnabled = false;
        this.savedSlowdown = 0.0;
    }

    private boolean isMovingTowards(EntityLivingBase source, EntityLivingBase target, double maxAngle) {
        Vec3 currentPos = source.getPositionVector();
        Vec3 lastPos = new Vec3(source.lastTickPosX, source.lastTickPosY, source.lastTickPosZ);
        Vec3 targetPos = target.getPositionVector();

        // Calculate movement vector
        double mx = currentPos.xCoord - lastPos.xCoord;
        double mz = currentPos.zCoord - lastPos.zCoord;
        double movementLength = Math.sqrt(mx * mx + mz * mz);

        // If not moving, return false
        if (movementLength == 0.0) {
            return false;
        }

        // Normalize movement vector
        mx /= movementLength;
        mz /= movementLength;

        // Calculate vector to target
        double tx = targetPos.xCoord - currentPos.xCoord;
        double tz = targetPos.zCoord - currentPos.zCoord;
        double targetLength = Math.sqrt(tx * tx + tz * tz);

        // If target is at same position, return false
        if (targetLength == 0.0) {
            return false;
        }

        // Normalize target vector
        tx /= targetLength;
        tz /= targetLength;

        // Calculate dot product (cosine of angle between vectors)
        double dotProduct = mx * tx + mz * tz;

        // Check if angle is within threshold
        return dotProduct >= Math.cos(Math.toRadians(maxAngle));
    }

    @Override
    public void onDisabled() {
        this.resetMotion();
        this.sprintState = false;
        this.set = false;
        this.savedSlowdown = 0.0;
        this.attackTime = -1L;
        this.currentShouldAttack = false;
        this.blockedHits = 0;
        this.allowedHits = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
