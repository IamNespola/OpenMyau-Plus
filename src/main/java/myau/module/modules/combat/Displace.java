package myau.module.modules.combat;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.events.UpdateEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.PlayerUtil;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Displace extends Module {
    private static final int DISPLACE_WINDOW_TICKS = 10;

    public final FloatProperty yawOffset = new FloatProperty("Yaw offset", 90.0F, 0.0F, 180.0F);
    public final FloatProperty delay = new FloatProperty("Delay", 0.0F, 0.0F, 500.0F);
    public final ModeProperty direction = new ModeProperty("Direction", 0, new String[]{"Left", "Right"});
    public final BooleanProperty findVoid = new BooleanProperty("Find void", false);
    public final BooleanProperty blink = new BooleanProperty("Blink", false);
    public final BooleanProperty hasKnockback = new BooleanProperty("Has knockback", false);

    private boolean displaceThisTick = false;
    private boolean active = false;
    private boolean hasKB = false;
    private boolean compensateNextTick = false;
    private boolean displaceLeft = false;
    private boolean wasDisplacingLastTick = false;
    private boolean releaseBlinkNextGameTick = false;
    private int tickCounter;
    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();

    public Displace() {
        super("Displace",Category.COMBAT,false);
    }

    @Override
    public void onEnabled() {
        displaceThisTick = false;
        active = false;
        hasKB = false;
        compensateNextTick = false;
        wasDisplacingLastTick = false;
        releaseBlinkNextGameTick = false;
        tickCounter = 0;
        targetWindowStartTicks.clear();
        releaseBlink();
    }

    @Override
    public void onDisabled() {
        active = false;
        compensateNextTick = false;
        wasDisplacingLastTick = false;
        releaseBlinkNextGameTick = false;
        targetWindowStartTicks.clear();
        releaseBlink();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{(int) Math.round(delay.getValue()) + "ms"};
    }

    private static int msToTicks(double ms) {
        if (ms <= 0.0D) {
            return 0;
        }
        return (int) Math.ceil(ms / 50.0D);
    }

    private boolean anyMovementKey() {
        return mc.gameSettings.keyBindForward.isKeyDown()
                || mc.gameSettings.keyBindBack.isKeyDown()
                || mc.gameSettings.keyBindLeft.isKeyDown()
                || mc.gameSettings.keyBindRight.isKeyDown();
    }

    private boolean tryFindVoidDirection(EntityPlayer target) {
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) return false;

        dx /= dist;
        dz /= dist;

        double rightX = -dz;
        double rightZ = dx;

        double eyeY = target.posY + (double) target.getEyeHeight();

        int leftVoidCount = 0;
        int rightVoidCount = 0;

        for (int i = 1; i <= 12; i++) {
            double off = i * 0.5;

            double rx = target.posX + rightX * off;
            double rz = target.posZ + rightZ * off;
            if (mc.theWorld.rayTraceBlocks(new Vec3(rx, eyeY, rz), new Vec3(rx, eyeY - 10, rz)) == null) {
                rightVoidCount++;
            }

            double lx = target.posX - rightX * off;
            double lz = target.posZ - rightZ * off;
            if (mc.theWorld.rayTraceBlocks(new Vec3(lx, eyeY, lz), new Vec3(lx, eyeY - 10, lz)) == null) {
                leftVoidCount++;
            }
        }

        if (leftVoidCount == 0 && rightVoidCount == 0) return false;

        if (leftVoidCount != rightVoidCount) {
            displaceLeft = leftVoidCount > rightVoidCount;
        }
        return true;
    }

    private void pruneTargetDelayStates() {
        if (mc.theWorld == null) {
            targetWindowStartTicks.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Integer>> iterator = targetWindowStartTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            Entity entity = mc.theWorld.getEntityByID(entry.getKey());
            if (!(entity instanceof EntityPlayer) || entity.isDead || ((EntityPlayer) entity).deathTime != 0) {
                iterator.remove();
            }
        }
    }

    private boolean shouldDisplaceInCurrentWindow(EntityPlayer target, int currentTick) {
        if (target == null) {
            return true;
        }

        int targetId = target.getEntityId();
        Integer windowStartTick = targetWindowStartTicks.get(targetId);
        if (windowStartTick == null || currentTick - windowStartTick >= DISPLACE_WINDOW_TICKS) {
            targetWindowStartTicks.put(targetId, currentTick);
            return true;
        }

        int delayTicks = msToTicks(delay.getValue());
        if (delayTicks <= 0) {
            return true;
        }

        int elapsed = currentTick - windowStartTick;
        return elapsed >= delayTicks;
    }

    private void releaseBlink() {
        Myau.blinkManager.setBlinkState(false, BlinkModules.DISPLACE);
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) return;

        if (releaseBlinkNextGameTick) {
            releaseBlink();
            releaseBlinkNextGameTick = false;
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled()) {
            compensateNextTick = false;
            return;
        }
        if (!active) {
            compensateNextTick = false;
            return;
        }

        if (compensateNextTick && !displaceThisTick) {
            compensateNextTick = false;
            if (displaceLeft) {
                mc.thePlayer.movementInput.moveStrafe = -1;
            } else {
                mc.thePlayer.movementInput.moveStrafe = 1;
            }
            return;
        }

        if (!displaceThisTick || hasKB) return;
        if (!anyMovementKey()) return;

        mc.thePlayer.movementInput.moveForward = 1;
        compensateNextTick = true;
    }

    @EventTarget(Priority.HIGH)
    public void onSendPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.POST) return;

        if (!blink.getValue() || !active || !displaceThisTick || releaseBlinkNextGameTick) {
            return;
        }
        if (!(event.getPacket() instanceof C03PacketPlayer)) {
            return;
        }
        if (Myau.blinkManager.getBlinkingModule() == BlinkModules.DISPLACE) {
            return;
        }

        Myau.blinkManager.setBlinkState(true, BlinkModules.DISPLACE);
        releaseBlinkNextGameTick = true;
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (!this.isEnabled()) return;

        if (mc.thePlayer == null || mc.theWorld == null) {
            active = false;
            compensateNextTick = false;
            wasDisplacingLastTick = false;
            return;
        }

        tickCounter++;
        int currentTick = tickCounter;
        pruneTargetDelayStates();

        boolean passesItemCondition = true;
        if (hasKnockback.getValue()) {
            passesItemCondition = EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
        }
        if (!passesItemCondition) {
            active = false;
            displaceThisTick = false;
            compensateNextTick = false;
            wasDisplacingLastTick = false;
            return;
        }

        EntityPlayer target = null;
        boolean attacking = Mouse.isButtonDown(0)
                || isKillAuraActive();
        if (attacking) {
            target = findClosestTarget(9.0);
        }

        boolean hasKBEnchant = EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
        active = target != null && (hasKBEnchant || anyMovementKey());
        if (!active) {
            displaceThisTick = false;
            compensateNextTick = false;
            wasDisplacingLastTick = false;
            return;
        }

        if (!findVoid.getValue() || !tryFindVoidDirection(target)) {
            displaceLeft = direction.getValue() == 0;
        }

        hasKB = hasKBEnchant;
        displaceThisTick = !displaceThisTick;
        if (displaceThisTick && !shouldDisplaceInCurrentWindow(target, currentTick)) {
            displaceThisTick = false;
            compensateNextTick = false;
            wasDisplacingLastTick = false;
            return;
        }

        if (!displaceThisTick && wasDisplacingLastTick) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            if (key != 0) {
                KeyBinding.onTick(key);
            }
        }

        wasDisplacingLastTick = displaceThisTick;

        if (!displaceThisTick) return;

        float baseYaw = mc.thePlayer.rotationYaw;
        float offset = yawOffset.getValue();

        if (displaceLeft) {
            baseYaw -= offset;
        } else {
            baseYaw += offset;
        }

        event.setRotation(baseYaw, mc.thePlayer.rotationPitch, 1);
        event.setPervRotation(baseYaw, 1);
    }

    private boolean isKillAuraActive() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.isEnabled() && killAura.target != null;
    }

    private EntityPlayer findClosestTarget(double maxRange) {
        EntityPlayer closest = null;
        double closestDist = maxRange;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            if (entity == mc.thePlayer) continue;
            if (entity.isDead || ((EntityPlayer) entity).deathTime != 0) continue;

            double dist = mc.thePlayer.getDistanceToEntity(entity);
            if (dist < closestDist) {
                closest = (EntityPlayer) entity;
                closestDist = dist;
            }
        }

        return closest;
    }
}
