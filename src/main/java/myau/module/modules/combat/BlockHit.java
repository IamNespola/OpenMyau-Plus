package myau.module.modules.combat;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.FloatProperty;
import myau.util.ItemUtil;
import myau.util.RandomUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class BlockHit extends Module {
    // Properties
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"VANILLA", "PACKET", "HEURISTIC", "SMART"});
    public final BooleanProperty raytrace = new BooleanProperty("raytrace", true);
    public final PercentProperty chance = new PercentProperty("chance", 100);
    public final IntProperty blockTicks = new IntProperty("block-ticks", 2, 1, 10);
    public final IntProperty unblockTicks = new IntProperty("unblock-ticks", 2, 1, 10);
    public final BooleanProperty autoBlock = new BooleanProperty("auto-block", true);
    public final BooleanProperty onlyWhileAttacking = new BooleanProperty("only-attacking", true);
    public final BooleanProperty checkCooldown = new BooleanProperty("check-cooldown", true);
    public final FloatProperty reactionDelay = new FloatProperty("reaction-delay", 100.0F, 0.0F, 500.0F, () -> mode.getValue() == 2 || mode.getValue() == 3);
    public final FloatProperty reactionVariance = new FloatProperty("reaction-variance", 50.0F, 0.0F, 200.0F, () -> mode.getValue() == 2 || mode.getValue() == 3);
    public final BooleanProperty postHitDelay = new BooleanProperty("post-hit-delay", true);
    public final IntProperty minPostDelay = new IntProperty("min-post-delay", 50, 0, 500, () -> postHitDelay.getValue());
    public final IntProperty maxPostDelay = new IntProperty("max-post-delay", 150, 0, 500, () -> postHitDelay.getValue());
    public final BooleanProperty smartPattern = new BooleanProperty("smart-pattern", true);
    public final BooleanProperty releaseOnMiss = new BooleanProperty("release-on-miss", true);

    // State tracking
    private boolean shouldBlock = false;
    private int ticksUntilAction = 0;
    private boolean isBlocking = false;
    private int hitCounter = 0;
    private long lastAttackTime = 0;
    private long lastBlockTime = 0;
    private long scheduledBlockTime = 0;
    private final TimerUtil cooldownTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private int blockPattern = 0; // For smart patterns

    // Heuristic data
    private double[] reactionTimes = new double[10];
    private int reactionIndex = 0;

    public BlockHit() {
        super("BlockHit", Category.COMBAT,false);

        // Initialize reaction time data with realistic values
        for (int i = 0; i < reactionTimes.length; i++) {
            reactionTimes[i] = 150.0 + RandomUtil.nextDouble(-30.0, 30.0);
        }
    }

    @Override
    public void onEnabled() {
        shouldBlock = false;
        ticksUntilAction = 0;
        isBlocking = false;
        hitCounter = 0;
        lastAttackTime = 0;
        lastBlockTime = 0;
        scheduledBlockTime = 0;
        lastTarget = null;
        blockPattern = 0;
        cooldownTimer.reset();
    }

    @Override
    public void onDisabled() {
        if (isBlocking && mc.thePlayer != null) {
            unblock();
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        // Check if holding sword
        if (!isHoldingSword()) return;

        Entity target = event.getTarget();

        // Raytrace check
        if (raytrace.getValue()) {
            if (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null) {
                if (releaseOnMiss.getValue() && isBlocking) {
                    unblock();
                }
                return;
            }
        }

        // Chance check
        if (RandomUtil.nextInt(0, 100) > chance.getValue()) return;

        // Check cooldown
        if (checkCooldown.getValue() && !canAttack()) {
            return;
        }

        // Update state
        lastAttackTime = System.currentTimeMillis();
        hitCounter++;

        if (target instanceof EntityLivingBase) {
            lastTarget = (EntityLivingBase) target;
        }

        // Schedule blocking based on mode
        if (autoBlock.getValue()) {
            scheduleBlock();
        }
    }

    /**
     * Schedule blocking with appropriate timing
     */
    private void scheduleBlock() {
        long currentTime = System.currentTimeMillis();
        long delay = 0;

        switch (mode.getValue()) {
            case 0: // VANILLA
                shouldBlock = true;
                ticksUntilAction = 1;
                break;

            case 1: // PACKET
                delay = postHitDelay.getValue() ?
                        RandomUtil.nextInt(minPostDelay.getValue(), maxPostDelay.getValue()) : 0;
                scheduledBlockTime = currentTime + delay;
                shouldBlock = true;
                break;

            case 2: // HEURISTIC
                delay = calculateHeuristicDelay();
                scheduledBlockTime = currentTime + (long)delay;
                shouldBlock = true;
                break;

            case 3: // SMART
                delay = calculateSmartDelay();
                scheduledBlockTime = currentTime + (long)delay;
                shouldBlock = true;
                break;
        }
    }

    /**
     * Calculate heuristic delay based on human reaction patterns
     */
    private long calculateHeuristicDelay() {
        // Update reaction time buffer with new value
        double baseDelay = reactionDelay.getValue();
        double variance = reactionVariance.getValue();
        double newReaction = baseDelay + RandomUtil.nextDouble(-variance, variance);

        reactionTimes[reactionIndex] = newReaction;
        reactionIndex = (reactionIndex + 1) % reactionTimes.length;

        // Calculate average reaction time (simulates muscle memory)
        double avgReaction = 0;
        for (double time : reactionTimes) {
            avgReaction += time;
        }
        avgReaction /= reactionTimes.length;

        // Mix current and average (80% current, 20% average)
        return (long)(newReaction * 0.8 + avgReaction * 0.2);
    }

    /**
     * Calculate smart delay with pattern variation
     */
    private long calculateSmartDelay() {
        blockPattern++;

        // Create varied patterns to avoid detection
        long baseDelay = calculateHeuristicDelay();

        switch (blockPattern % 5) {
            case 0: return baseDelay;
            case 1: return baseDelay + 20;
            case 2: return baseDelay - 10;
            case 3: return baseDelay + 30;
            case 4: return baseDelay + 5;
        }

        return baseDelay;
    }

    /**
     * Check if player can attack (cooldown check)
     */
    private boolean canAttack() {
        if (mc.thePlayer == null) return false;

        // Check vanilla attack cooldown (1.8 doesn't have getCooledAttackStrength)
        // Approximate with timer
        long timeSinceAttack = System.currentTimeMillis() - lastAttackTime;
        return timeSinceAttack >= 500; // 0.5 second cooldown
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND) return;

        // Track attack packets for better cooldown detection
        if (event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() == C02PacketUseEntity.Action.ATTACK) {
                cooldownTimer.reset();
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (!isHoldingSword()) {
            if (isBlocking) {
                unblock();
            }
            return;
        }

        // Only block while attacking
        if (onlyWhileAttacking.getValue() && hitCounter == 0) {
            if (isBlocking) {
                unblock();
            }
            return;
        }

        // Handle scheduled blocking
        long currentTime = System.currentTimeMillis();
        if (shouldBlock && scheduledBlockTime > 0 && currentTime >= scheduledBlockTime) {
            scheduledBlockTime = 0;
        }

        // Tick-based action delay
        if (ticksUntilAction > 0) {
            ticksUntilAction--;
            return;
        }

        // Block/unblock cycle
        if (shouldBlock && !isBlocking && (scheduledBlockTime == 0 || currentTime >= scheduledBlockTime)) {
            block();
            ticksUntilAction = blockTicks.getValue();
            shouldBlock = false;
        } else if (isBlocking) {
            // Check if should unblock
            long blockDuration = currentTime - lastBlockTime;
            int unblockDelay = unblockTicks.getValue() * 50; // Convert ticks to ms

            if (blockDuration >= unblockDelay) {
                unblock();
                ticksUntilAction = 0;
            }
        }

        // Reset hit counter after inactivity
        if (hitCounter > 0) {
            long inactiveTime = currentTime - lastAttackTime;
            if (inactiveTime > 1000) { // 1 second
                hitCounter--;
            }
        }

        // Validate target still exists
        if (lastTarget != null && (lastTarget.isDead || lastTarget.getHealth() <= 0)) {
            lastTarget = null;
            if (isBlocking && releaseOnMiss.getValue()) {
                unblock();
            }
        }
    }

    /**
     * Block with sword using appropriate method
     */
    private void block() {
        if (mc.thePlayer == null || !isHoldingSword()) return;

        if (mode.getValue() == 1 || mode.getValue() == 2 || mode.getValue() == 3) {
            // Packet-based blocking (C07/C08)
            blockPacket();
        } else {
            // Vanilla blocking
            blockVanilla();
        }

        isBlocking = true;
        lastBlockTime = System.currentTimeMillis();
    }

    /**
     * Vanilla blocking method
     */
    private void blockVanilla() {
        mc.thePlayer.sendQueue.addToSendQueue(
                new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem())
        );
    }

    /**
     * Packet-based blocking with C07/C08
     */
    private void blockPacket() {
        // Send release packet first
        mc.thePlayer.sendQueue.addToSendQueue(
                new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        EnumFacing.DOWN
                )
        );

        // Send block packet
        mc.thePlayer.sendQueue.addToSendQueue(
                new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem())
        );
    }

    /**
     * Unblock sword
     */
    private void unblock() {
        if (mc.thePlayer == null) return;

        if (mode.getValue() == 1 || mode.getValue() == 2 || mode.getValue() == 3) {
            // Packet-based unblocking
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                    )
            );
        } else {
            // Vanilla unblocking
            mc.playerController.onStoppedUsingItem(mc.thePlayer);
        }

        isBlocking = false;
    }

    /**
     * Check if holding sword
     */
    private boolean isHoldingSword() {
        return mc.thePlayer != null &&
                mc.thePlayer.getHeldItem() != null &&
                mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    @Override
    public String[] getSuffix() {
        String[] modes = {"Vanilla", "Packet", "Heuristic", "Smart"};
        return new String[]{modes[mode.getValue()]};
    }
}
