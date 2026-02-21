package myau.module.modules;

import myau.event.EventTarget;
import myau.events.AttackEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public class SprintReset extends Module {

    public final ModeProperty mode = new ModeProperty("Mode", 2, new String[]{"PACKET", "LEGIT", "SILENT"});
    private final BooleanProperty onlyWhileSprinting = new BooleanProperty("Only While Sprinting", true);
    private final BooleanProperty onlyWhileMoving = new BooleanProperty("Only While Moving", true);
    private final BooleanProperty resetOnCrit = new BooleanProperty("Reset On Crit", true);
    private final BooleanProperty smartReset = new BooleanProperty("Smart Reset", false);
    private final BooleanProperty fastReset = new BooleanProperty("Fast Reset", false);
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean attacked = false;
    private boolean needsReset = false;
    private int ticksSinceAttack = 0;
    private Entity lastTarget = null;

    public SprintReset() {
        super("SprintReset", false, false);
    }

    @Override
    public void onEnabled() {
        attacked = false;
        needsReset = false;
        ticksSinceAttack = 0;
        lastTarget = null;
    }

    @Override
    public void onDisabled() {
        attacked = false;
        needsReset = false;
        ticksSinceAttack = 0;
        lastTarget = null;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Entity target = event.getTarget();
        if (target == null) return;
        if (!(target instanceof EntityLivingBase)) return;

        if (onlyWhileSprinting.getValue() && !mc.thePlayer.isSprinting()) return;
        if (onlyWhileMoving.getValue() && !isMoving()) return;

        if (smartReset.getValue()) {
            if (((EntityLivingBase) target).getHealth() <= 0) return;
            if (mc.thePlayer.getDistanceToEntity(target) > 6.0f) return;
        }
        if (fastReset.getValue() && lastTarget != null && lastTarget != target) {
            handlePacketReset();
        }

        lastTarget = target;
        attacked = true;

        switch (mode.getValue()) {
            case 0:
                handlePacketReset();
                break;
            case 1:
                handleLegitReset();
                break;
            case 2:
                handleSilentReset();
                break;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (attacked) {
            ticksSinceAttack++;
        }

        if (mode.getValue() == 1 && needsReset) {
            if (ticksSinceAttack >= 1) {
                mc.thePlayer.setSprinting(true);
                needsReset = false;
                attacked = false;
                ticksSinceAttack = 0;
            }
        }

        if (mode.getValue() == 2 && needsReset) {
            if (ticksSinceAttack >= 1) {
                mc.thePlayer.sendQueue.addToSendQueue(
                        new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING)
                );
                needsReset = false;
                attacked = false;
                ticksSinceAttack = 0;
            }
        }

        if (ticksSinceAttack > 5) {
            attacked = false;
            needsReset = false;
            ticksSinceAttack = 0;
        }
    }

    private void handlePacketReset() {
        mc.thePlayer.sendQueue.addToSendQueue(
                new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING)
        );
        mc.thePlayer.sendQueue.addToSendQueue(
                new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING)
        );
        if (resetOnCrit.getValue() && canCrit()) {
            performCritReset();
        }
        attacked = false;
        ticksSinceAttack = 0;
    }

    private void handleLegitReset() {
        mc.thePlayer.setSprinting(false);
        needsReset = true;
        ticksSinceAttack = 0;

        if (resetOnCrit.getValue() && canCrit()) {
            performCritReset();
        }
    }

    private void handleSilentReset() {
        mc.thePlayer.sendQueue.addToSendQueue(
                new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING)
        );

        needsReset = true;
        ticksSinceAttack = 0;

        if (resetOnCrit.getValue() && canCrit()) {
            performCritReset();
        }
    }

    private void performCritReset() {
        if (mc.thePlayer.onGround && !mc.thePlayer.isInWater() && !mc.thePlayer.isOnLadder()) {
            double x = mc.thePlayer.posX;
            double y = mc.thePlayer.posY;
            double z = mc.thePlayer.posZ;

            mc.thePlayer.sendQueue.addToSendQueue(
                    new C03PacketPlayer.C04PacketPlayerPosition(x, y + 0.0625, z, false)
            );
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false)
            );
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C03PacketPlayer.C04PacketPlayerPosition(x, y + 1.1E-5, z, false)
            );
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false)
            );
        }
    }

    private boolean canCrit() {
        return mc.thePlayer.onGround
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isInLava()
                && !mc.thePlayer.isOnLadder()
                && !mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.blindness)
                && mc.thePlayer.ridingEntity == null;
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }
}