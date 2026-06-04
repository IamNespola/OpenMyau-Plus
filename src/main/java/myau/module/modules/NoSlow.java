package myau.module.modules;

import myau.Myau;
import myau.enums.FloatModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.events.PacketEvent;
import myau.events.PlayerUpdateEvent;
import myau.events.RightClickMouseEvent;
import myau.module.Module;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.PacketUtil;
import myau.util.PlayerUtil;
import myau.util.TeamUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty mode = new ModeProperty("mode", 1, new String[]{"NONE", "VANILLA", "GRIM", "INTAVE", "GRIM_TEST"});
    public final BooleanProperty sword = new BooleanProperty("Sword", true, () -> this.mode.getValue() != 0);
    public final BooleanProperty food = new BooleanProperty("Food", true, () -> this.mode.getValue() != 0);
    public final BooleanProperty bow = new BooleanProperty("Bow", true, () -> this.mode.getValue() != 0);
    public final PercentProperty motion = new PercentProperty("motion", 100, () -> this.mode.getValue() == 1);
    public final BooleanProperty sprint = new BooleanProperty("sprint", true, () -> this.mode.getValue() != 0);
    public final BooleanProperty killauraonly = new BooleanProperty("killaura-only", false, () -> this.mode.getValue() != 0 && this.sword.getValue());
    private int count;
    private boolean lastUsingItem;
    private boolean grimTestCancelRelease;

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killauraonly.getValue()) {
            if (!killAura.isEnabled()) return false;
            if (killAura.getTarget() == null) return false;
        }
        return this.mode.getValue() != 0 && this.sword.getValue() && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.mode.getValue() != 0 && this.food.getValue() && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.mode.getValue() != 0 && this.bow.getValue() && ItemUtil.isUsingBow();
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return this.isAnyActive() && this.sprint.getValue();
    }

    public int getMotionMultiplier() {
        count++;
        if (!isAnyActive()) return 100;
        if (mode.getValue() == 2) {
            return count % 2 == 0 ? 100 : 20;
        }
        if (mode.getValue() == 4 && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) {
            return 20;
        }
        return this.motion.getValue();
    }

    @EventTarget(Priority.HIGH)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 3) return;
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem() || mc.thePlayer.getHeldItem() == null) {
            lastUsingItem = false;
            return;
        }
        if (!this.isAnyActive() || !isMoving()) return;

        Item item = mc.thePlayer.getHeldItem().getItem();
        if (ItemUtil.isEating()) {
            if (!lastUsingItem) {
                PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP));
            }
        } else if (item instanceof ItemSword) {
            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        }
        lastUsingItem = true;
    }

    @Override
    public void onDisabled() {
        lastUsingItem = false;
        grimTestCancelRelease = false;
    }

    @EventTarget(Priority.HIGH)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 4 || event.getType() != EventType.SEND) return;
        if (canGrimTestFoodNoSlow()) {
            if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
                event.setCancelled(true);
                PacketUtil.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                PacketUtil.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                mc.playerController.onStoppedUsingItem(mc.thePlayer);
                grimTestCancelRelease = true;
            } else if (grimTestCancelRelease && event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging digging = (C07PacketPlayerDigging) event.getPacket();
                if (digging.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    grimTestCancelRelease = false;
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onGrimTestUpdate(PlayerUpdateEvent event) {
        if (!this.isEnabled() || this.mode.getValue() != 4 || mc.thePlayer == null) return;
        ItemStack itemStack = mc.thePlayer.getHeldItem();
        if (!mc.thePlayer.isUsingItem() || itemStack == null) {
            grimTestCancelRelease = false;
            return;
        }

        if (this.bow.getValue() && itemStack.getItem() instanceof ItemBow) {
            sendGrimTestSlotSpoof();
        } else if (this.sword.getValue() && itemStack.getItem() instanceof ItemSword) {
            sendGrimTestSlotSpoof();
        }
    }

    private void sendGrimTestSlotSpoof() {
        PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
        PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 7 + 2));
        PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
    }

    private boolean canGrimTestFoodNoSlow() {
        ItemStack item = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        return this.food.getValue() && item != null && item.getItem() instanceof ItemFood && item.stackSize > 2;
    }

    private boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F);
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                            return;
                        }
                        break;
                    case ENTITY:
                        Entity entityHit = mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (entityHit instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) entityHit)) {
                            return;
                        }
                        break;
                }
            }
        }
    }
}