package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.enums.FloatModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.mixin.IAccessorKeyBinding;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.util.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

import java.util.Random;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty swordMode = new ModeProperty("Sword Mode", 1, new String[]{"None", "Vanilla", "Hypixel", "NCP", "NewNCP", "Watchdog", "Intave", "Grim", "NewGrim", "Verus", "AAC", "Spartan", "OpalWatchdog"});
    public final IntProperty swapDelay = new IntProperty("Swap Delay", 0, 0, 3, () -> swordMode.getValue() == 2);
    public final BooleanProperty noAttack = new BooleanProperty("No Attack", false, () -> swordMode.getValue() == 2);
    public final PercentProperty swordMotion = new PercentProperty("Sword Motion", 100, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty swordSprint = new BooleanProperty("Sword Sprint", true, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty onlyKillAuraAutoBlock = new BooleanProperty("Only Kill Aura Auto Block", false, () -> this.swordMode.getValue() != 0);
    public final ModeProperty foodMode = new ModeProperty("Food Mode", 0, new String[]{"None", "Vanilla", "Float", "NCP", "NewNCP", "Watchdog", "Intave", "Grim", "NewGrim", "Verus", "AAC", "Spartan", "OpalWatchdog"});
    public final PercentProperty foodMotion = new PercentProperty("Food Motion", 100, () -> this.foodMode.getValue() != 0);
    public final BooleanProperty foodSprint = new BooleanProperty("Food Sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeProperty bowMode = new ModeProperty("Bow Mode", 0, new String[]{"None", "Vanilla", "Float", "NCP", "NewNCP", "Watchdog", "Intave", "Grim", "NewGrim", "Verus", "AAC", "Spartan", "OpalWatchdog"});
    public final PercentProperty bowMotion = new PercentProperty("Bow Motion", 100, () -> this.bowMode.getValue() != 0);
    public final BooleanProperty bowSprint = new BooleanProperty("Bow Sprint", true, () -> this.bowMode.getValue() != 0);

    // Miau Client NoSlow modes (ported from miau.module.modules.movement.noslow.*).
    // Appended to the Sword/Food/Bow mode lists above; property values >= MIAU_BASE map to these indices.
    private static final int MIAU_BASE = 3;
    private static final int MIAU_NCP = 0;
    private static final int MIAU_NEW_NCP = 1;
    private static final int MIAU_WATCHDOG = 2;
    private static final int MIAU_INTAVE = 3;
    private static final int MIAU_GRIM = 4;
    private static final int MIAU_NEW_GRIM = 5;
    private static final int MIAU_VERUS = 6;
    private static final int MIAU_AAC = 7;
    private static final int MIAU_SPARTAN = 8;
    private static final int MIAU_OPAL_WATCHDOG = 9;

    public final BooleanProperty antiSwitch = new BooleanProperty("Anti-Switch", false, () -> this.hasMiauMode());

    // inlined BadPacketsComponent (Miau tracks these globally)
    private boolean bpSlot, bpAttack, bpSwing, bpBlock, bpInventory;
    private boolean savedSlot, savedAttack, savedSwing, savedBlock, savedInventory;

    // NewNCP / Intave / Spartan state
    private int newNcpDisable;
    private int intaveDisable;
    private int spartanDisable;

    // Watchdog state
    private int wdOffGroundTicks;
    private boolean wdStop;
    private boolean wdDisable;

    // NewGrim state
    private int newGrimTicks;

    // OpalWatchdog state
    private int opalNextCycleTick = -1;
    private boolean opalRunThisTick;
    private boolean opalStopUse;
    private boolean opalBlocking;
    private int opalSlotChangeTick = -1;

    private int delay = 0;
    private boolean post = false;

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword() && (!this.onlyKillAuraAutoBlock.getValue() || this.isKillAuraAutoBlocking());
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating()
                || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    private boolean isKillAuraAutoBlocking() {
        KillAura aura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (!aura.isPlayerBlocking() || !aura.isEnabled()) {
            return false;
        }
        return aura.isBlocking();
    }

    public boolean isAnyActive() {
        if (this.swordMode.getValue() != 2) {
            return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
        } else if (this.swordMode.getValue() == 2 && isSwordActive()) {
            KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
            if (!noAttack.getValue() || !((killAura.blockTick == 0 && killAura.autoBlock.getValue() == 2) || (killAura.autoBlock.getValue() == 6 && killAura.blockTick == killAura.attackTick.getValue()) || (killAura.autoBlock.getValue() != 6 && killAura.autoBlock.getValue() != 2) || (killAura.autoBlock.getValue() == 5 && killAura.blockTick == 0) && killAura.isEnabled() && killAura.isPlayerBlocking())) {
                return delay == 0;
            }
        }
        return false;
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue()
                || this.isFoodActive() && this.foodSprint.getValue()
                || this.isBowActive() && this.bowSprint.getValue();
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) {
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            return this.foodMotion.getValue();
        } else {
            return ItemUtil.isUsingBow() ? this.bowMotion.getValue() : 100;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        // BadPacketsComponent bookkeeping: save on PRE, reset on POST (runs even while disabled).
        if (mc.thePlayer != null && mc.theWorld != null) {
            if (event.getType() == EventType.PRE) {
                this.savedSlot = this.bpSlot;
                this.savedAttack = this.bpAttack;
                this.savedSwing = this.bpSwing;
                this.savedBlock = this.bpBlock;
                this.savedInventory = this.bpInventory;
            } else if (event.getType() == EventType.POST) {
                this.resetBadPackets();
            }
        }
        if (!this.isEnabled()) return;
        if (mc.thePlayer != null && mc.theWorld != null) {
            this.updateMiau(event);
        }
        if (ItemUtil.isHoldingSword() && mc.thePlayer.isUsingItem()) {
            if (isSwordActive()) {
                if (this.swordMode.getValue() == 2) {
                    if (event.getType() == EventType.PRE) {
                        delay--;
                        if (delay < 0) {
                            KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);
                            if (!noAttack.getValue() || !((killAura.blockTick == 0 && killAura.autoBlock.getValue() == 2) || (killAura.autoBlock.getValue() == 6 && killAura.blockTick == killAura.attackTick.getValue()) || (killAura.autoBlock.getValue() != 6 && killAura.autoBlock.getValue() != 2) || (killAura.autoBlock.getValue() == 5 && killAura.blockTick == 0) && killAura.isEnabled() && killAura.isPlayerBlocking())) {
                                int randomSlot = new Random().nextInt(9);
                                while (randomSlot == mc.thePlayer.inventory.currentItem) {
                                    randomSlot = new Random().nextInt(9);
                                }
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                            }
                            post = true;
                            delay = swapDelay.getValue();
                        }
                    }
                }
            }
        } else {
            if (post) {
                post = false;
            }
        }
    }

    @EventTarget
    public void onMotion(PostMotionEvent event) {
        if (!this.isEnabled()) return;
        if (!ItemUtil.isHoldingSword() || !mc.thePlayer.isUsingItem()) return;
        if (isSwordActive()) {
            if (this.swordMode.getValue() == 2) {
                if (post) {
                    post = false;
                }
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.isAnyActive()) {
            float multiplier = (float) this.getMotionMultiplier() / 100.0F;
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && this.isFloatMode()) {
            int item = mc.thePlayer.inventory.currentItem;
            Myau.floatManager.setFloatState(true, FloatModules.NO_SLOW);
        } else {
            Myau.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }
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
                }
            }
            if (this.isFloatMode() && !Myau.floatManager.isPredicted() && mc.thePlayer.onGround) {
                event.setCancelled(true);
                mc.thePlayer.motionY = 0.42F;
            }
            int heldMiauMode = this.heldItemMiauMode();
            if (heldMiauMode == MIAU_WATCHDOG) {
                this.watchdogOnRightClick(event);
            } else if (heldMiauMode == MIAU_OPAL_WATCHDOG && this.miauSwordActive(MIAU_OPAL_WATCHDOG)) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        // BadPacketsComponent tracking (runs regardless of mode).
        if (event.getType() == EventType.SEND) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof C09PacketHeldItemChange) {
                this.bpSlot = true;
            } else if (packet instanceof C0APacketAnimation) {
                this.bpSwing = true;
            } else if (packet instanceof C02PacketUseEntity) {
                this.bpAttack = true;
            } else if (packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C07PacketPlayerDigging) {
                this.bpBlock = true;
            } else if (packet instanceof C0EPacketClickWindow
                    || (packet instanceof C16PacketClientStatus
                    && ((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)
                    || packet instanceof C0DPacketCloseWindow) {
                this.bpInventory = true;
            }
        }
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S08PacketPlayerPosLook) {
            if (this.isMiauModeUsed(MIAU_NEW_NCP)) {
                this.newNcpDisable = 0;
            }
            if (this.isMiauModeUsed(MIAU_SPARTAN)) {
                this.spartanDisable = 0;
            }
        }
        if (this.isMiauModeUsed(MIAU_INTAVE)
                && event.getType() == EventType.SEND
                && event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            if (this.miauSwordActive(MIAU_INTAVE) && !this.bad(false, true, true, false, false)) {
                int currentSlot = mc.thePlayer.inventory.currentItem;
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 8 + 1));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
            }
        }
        if (this.isMiauModeUsed(MIAU_OPAL_WATCHDOG)) {
            this.opalOnPacket(event);
        }
    }

    @Override
    public void onEnabled() {
        this.newGrimTicks = 0;
    }

    @Override
    public void onDisabled() {
        this.newGrimTicks = 0;
        if (this.isMiauModeUsed(MIAU_OPAL_WATCHDOG) && mc.thePlayer != null) {
            this.opalRelease();
            this.opalResetCycle();
        }
    }

    // ============ Miau Client NoSlow (ported, compacted) ============

    private int miauIdx(ModeProperty property) {
        return property.getValue() >= MIAU_BASE ? property.getValue() - MIAU_BASE : -1;
    }

    private boolean hasMiauMode() {
        return this.swordMode.getValue() >= MIAU_BASE
                || this.foodMode.getValue() >= MIAU_BASE
                || this.bowMode.getValue() >= MIAU_BASE;
    }

    private boolean isMiauModeUsed(int mode) {
        return this.miauIdx(this.swordMode) == mode
                || this.miauIdx(this.foodMode) == mode
                || this.miauIdx(this.bowMode) == mode;
    }

    // Which Miau mode governs the currently held item (-1 if none / not a Miau mode).
    private int heldItemMiauMode() {
        net.minecraft.item.ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            return -1;
        }
        if (ItemUtil.isHoldingSword()) {
            return this.miauIdx(this.swordMode);
        }
        if (ItemUtil.isEating() || held.getItem() instanceof ItemFood || held.getItem() instanceof ItemPotion) {
            return this.miauIdx(this.foodMode);
        }
        if (held.getItem() instanceof ItemBow) {
            return this.miauIdx(this.bowMode);
        }
        return -1;
    }

    private boolean miauSwordActive(int mode) {
        return this.miauIdx(this.swordMode) == mode && ItemUtil.isHoldingSword()
                && (!this.onlyKillAuraAutoBlock.getValue() || this.isKillAuraAutoBlocking());
    }

    private boolean miauFoodActive(int mode) {
        return this.miauIdx(this.foodMode) == mode && ItemUtil.isEating();
    }

    private boolean miauBowActive(int mode) {
        return this.miauIdx(this.bowMode) == mode && ItemUtil.isUsingBow();
    }

    private boolean miauPotionActive(int mode) {
        return this.miauIdx(this.foodMode) == mode
                && mc.thePlayer.isUsingItem()
                && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion;
    }

    private boolean miauAnyActive(int mode) {
        return mc.thePlayer.isUsingItem()
                && (this.miauSwordActive(mode) || this.miauFoodActive(mode) || this.miauBowActive(mode) || this.miauPotionActive(mode));
    }

    public boolean isMiauAntiSwitchActive() {
        if (!this.isEnabled() || !this.antiSwitch.getValue() || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }
        net.minecraft.item.ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemSword)) {
            return false;
        }
        return mc.thePlayer.isUsingItem();
    }

    // Called from MixinEntityPlayerSP: is the held item governed by a Miau mode right now?
    public boolean isMiauAnyActive() {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem()) {
            return false;
        }
        int mode = this.heldItemMiauMode();
        return mode >= 0 && this.miauAnyActive(mode);
    }

    public float getMiauMotionMultiplier() {
        if (this.heldItemMiauMode() == MIAU_GRIM) {
            return 0.35f;
        }
        return 1.0f;
    }

    // Called from MixinEntityPlayerSP to decide whether the vanilla use-item slowdown is skipped.
    public boolean shouldCancelMiauSlowdown() {
        if (!this.isEnabled()) {
            return false;
        }
        if (this.heldItemMiauMode() == MIAU_NEW_GRIM) {
            if (!this.miauAnyActive(MIAU_NEW_GRIM)) {
                this.newGrimTicks = 0;
                return false;
            }
            this.newGrimTicks++;
            if (this.newGrimTicks >= 2) {
                this.newGrimTicks = 0;
                return true;
            }
            return false;
        }
        return true;
    }

    private boolean bad(boolean slotCheck, boolean attackCheck, boolean swingCheck, boolean blockCheck, boolean inventoryCheck) {
        return (this.savedSlot && slotCheck)
                || (this.savedAttack && attackCheck)
                || (this.savedSwing && swingCheck)
                || (this.savedBlock && blockCheck)
                || (this.savedInventory && inventoryCheck);
    }

    private void resetBadPackets() {
        this.bpSlot = false;
        this.bpAttack = false;
        this.bpSwing = false;
        this.bpBlock = false;
        this.bpInventory = false;
        this.savedSlot = false;
        this.savedAttack = false;
        this.savedSwing = false;
        this.savedBlock = false;
        this.savedInventory = false;
    }

    private void miauApplyMotion() {
        float multiplier = this.getMiauMotionMultiplier();
        mc.thePlayer.movementInput.moveForward *= multiplier;
        mc.thePlayer.movementInput.moveStrafe *= multiplier;
    }

    private KillAura getKillAura() {
        return (KillAura) Myau.moduleManager.modules.get(KillAura.class);
    }

    private void updateMiau(UpdateEvent event) {
        if (this.isMiauModeUsed(MIAU_NCP)) {
            this.updateMiauNCP(event);
        }
        if (this.isMiauModeUsed(MIAU_NEW_NCP)) {
            this.newNcpDisable = this.updateMiauC09Bypass(event, MIAU_NEW_NCP, this.newNcpDisable, false);
        }
        if (this.isMiauModeUsed(MIAU_WATCHDOG)) {
            this.updateMiauWatchdog(event);
        }
        if (this.isMiauModeUsed(MIAU_INTAVE)) {
            this.intaveDisable = this.updateMiauC09Bypass(event, MIAU_INTAVE, this.intaveDisable, false);
        }
        if (this.isMiauModeUsed(MIAU_GRIM)) {
            this.updateMiauGrim(event);
        }
        if (this.isMiauModeUsed(MIAU_VERUS)) {
            this.updateMiauVerus(event);
        }
        if (this.isMiauModeUsed(MIAU_AAC)) {
            this.updateMiauAAC(event);
        }
        if (this.isMiauModeUsed(MIAU_SPARTAN)) {
            this.spartanDisable = this.updateMiauC09Bypass(event, MIAU_SPARTAN, this.spartanDisable, true);
        }
        if (this.isMiauModeUsed(MIAU_OPAL_WATCHDOG)) {
            this.updateMiauOpalWatchdog(event);
        }
    }

    private void updateMiauNCP(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.miauSwordActive(MIAU_NCP)) {
                PacketUtil.sendPacket(new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            }
        } else if (event.getType() == EventType.POST) {
            if (this.miauSwordActive(MIAU_NCP)) {
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            }
        }
        if (this.miauAnyActive(MIAU_NCP)) {
            this.miauApplyMotion();
        }
    }

    // NewNCP / Intave / Spartan share the same C09-swap bypass; Spartan adds a slot check.
    private int updateMiauC09Bypass(UpdateEvent event, int mode, int disable, boolean spartanSlotCheck) {
        if (event.getType() == EventType.PRE) {
            disable++;
            if (this.miauAnyActive(mode)) {
                KillAura aura = this.getKillAura();
                if (disable > 10
                        && !this.bad(false, true, true, false, false)
                        && (!spartanSlotCheck || !this.bad(true, false, false, false, false))
                        && (aura == null || aura.getTarget() == null)) {
                    int currentSlot = mc.thePlayer.inventory.currentItem;
                    PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 8 + 1));
                    PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
            }
        }
        KillAura aura = this.getKillAura();
        if (aura != null && aura.getTarget() != null) {
            return disable;
        }
        if (this.miauAnyActive(mode)) {
            this.miauApplyMotion();
        }
        return disable;
    }

    private void updateMiauWatchdog(UpdateEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        // Block-under check — reset disable while standing on a block.
        if (mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)).getBlock() != Blocks.air
                && !mc.thePlayer.isUsingItem()) {
            this.wdDisable = false;
        }
        // Slab check — disable no-slow while on a slab.
        double posY = mc.thePlayer.posY;
        if (Math.abs(posY - Math.round(posY)) > 0.03 && mc.thePlayer.onGround) {
            this.wdDisable = true;
        }
        // offGroundTicks tracking for non-sword items (food/bow/potion).
        if (mc.thePlayer.isUsingItem()
                && !(mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
            if (mc.thePlayer.onGround) {
                this.wdOffGroundTicks = 0;
            } else {
                this.wdOffGroundTicks++;
            }
            if (this.wdOffGroundTicks >= 2) {
                this.wdStop = false;
            } else if (mc.thePlayer.onGround && !this.wdDisable) {
                // PosY anti-flag: offset Y by 1E-14 so Watchdog doesn't detect it.
                mc.thePlayer.posY += 1E-14;
            }
        }
        // Cancel movement slowdown for food/bow/potion while not disabled.
        if (!this.wdDisable) {
            if (this.miauFoodActive(MIAU_WATCHDOG) || this.miauBowActive(MIAU_WATCHDOG) || this.miauPotionActive(MIAU_WATCHDOG)) {
                mc.thePlayer.movementInput.moveForward *= 5.0f;
                mc.thePlayer.movementInput.moveStrafe *= 5.0f;
            }
        }
        // Sword NoSlow: C09 swap + cancel slowdown.
        if (this.miauSwordActive(MIAU_WATCHDOG)) {
            int currentSlot = mc.thePlayer.inventory.currentItem;
            PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 7 + (int) (Math.random() * 2) + 1));
            PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
            mc.thePlayer.movementInput.moveForward *= 5.0f;
            mc.thePlayer.movementInput.moveStrafe *= 5.0f;
        }
    }

    private void watchdogOnRightClick(RightClickMouseEvent event) {
        if (mc.thePlayer.getHeldItem() == null) {
            return;
        }
        if (mc.thePlayer.isUsingItem()
                || (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion
                && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getMetadata()))
                || mc.thePlayer.getHeldItem().getItem() instanceof ItemFood
                || mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
            if (this.wdOffGroundTicks < 2 && this.wdOffGroundTicks != 0 && !this.wdDisable) {
                event.setCancelled(true);
            } else if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                event.setCancelled(true);
            }
        }
    }

    private void updateMiauGrim(UpdateEvent event) {
        if (this.miauAnyActive(MIAU_GRIM)) {
            if (event.getType() == EventType.PRE) {
                int currentSlot = mc.thePlayer.inventory.currentItem;
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 8 + 1));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
            }
            this.miauApplyMotion();
        }
    }

    private void updateMiauVerus(UpdateEvent event) {
        if (this.miauSwordActive(MIAU_VERUS)) {
            if (event.getType() == EventType.PRE) {
                PacketUtil.sendPacket(new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            } else {
                if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
            }
        }
        if (this.miauAnyActive(MIAU_VERUS)) {
            this.miauApplyMotion();
        }
    }

    private void updateMiauAAC(UpdateEvent event) {
        if (this.miauSwordActive(MIAU_AAC)) {
            if (event.getType() == EventType.PRE) {
                int currentSlot = mc.thePlayer.inventory.currentItem;
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot % 8 + 1));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(currentSlot));
                if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
            }
        }
        if (this.miauAnyActive(MIAU_AAC)) {
            this.miauApplyMotion();
        }
    }

    // OpalWatchdog: 3-tick block/release tap-cycle (Opal's WatchdogNoSlow via Miau's 1.8.9 port).
    private void updateMiauOpalWatchdog(UpdateEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (mc.currentScreen != null) {
            this.opalResetCycle();
            this.opalRelease();
            return;
        }

        // stopUse: finish 1-tick item-use flicker.
        if (this.opalStopUse) {
            if (mc.thePlayer.isUsingItem()) {
                this.opalBlock();
                mc.thePlayer.stopUsingItem();
            }
            this.opalStopUse = false;
        } else if (!this.miauSwordActive(MIAU_OPAL_WATCHDOG)) {
            if (!mc.thePlayer.isUsingItem()) {
                this.opalRelease();
            }
        }

        int age = mc.thePlayer.ticksExisted;
        boolean rightPressed = mc.gameSettings.keyBindUseItem.isKeyDown();

        if (this.miauSwordActive(MIAU_OPAL_WATCHDOG)) {
            if (rightPressed) {
                if (this.opalNextCycleTick < 0) {
                    this.opalNextCycleTick = age;
                }
                if (age >= this.opalNextCycleTick) {
                    // Cycle tick: release block.
                    if (this.opalBlocking) {
                        this.opalRelease();
                    }
                    this.opalRunThisTick = true;
                    this.opalNextCycleTick = age + 2; // happen again in 3 ticks
                } else if (!this.opalBlocking) {
                    // Off-cycle: keep blocked.
                    this.opalBlock();
                }
            } else {
                this.opalResetCycle();
                if (!mc.thePlayer.isUsingItem()) {
                    this.opalRelease();
                }
            }

            // runThisTick: simulate right-click press or cancel.
            if (this.opalRunThisTick && this.miauSwordActive(MIAU_OPAL_WATCHDOG)) {
                if (rightPressed) {
                    if (!mc.thePlayer.isUsingItem() || !this.opalBlocking) {
                        // Looking at interactable block or breaking -> skip.
                        if (mc.objectMouseOver != null
                                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                                && mc.objectMouseOver.getBlockPos() != null) {
                            Block block = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
                            IAccessorPlayerControllerMP accessor = (IAccessorPlayerControllerMP) mc.playerController;
                            if (this.isMiauInteractableBlock(block) || accessor.getIsHittingBlock()) {
                                this.opalRunThisTick = false;
                                return;
                            }
                        }
                        this.opalStopUse = true;
                        ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(true);
                    } else {
                        // Using item and already blocking: cancel right-click.
                        ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(false);
                    }
                } else {
                    this.opalStopUse = false;
                }
                this.opalRunThisTick = false;
            }
        } else {
            // Non-sword items: cancel slowdown.
            if (this.miauFoodActive(MIAU_OPAL_WATCHDOG) || this.miauBowActive(MIAU_OPAL_WATCHDOG) || this.miauPotionActive(MIAU_OPAL_WATCHDOG)) {
                mc.thePlayer.movementInput.moveForward *= 5.0f;
                mc.thePlayer.movementInput.moveStrafe *= 5.0f;
            }
        }
    }

    private void opalOnPacket(PacketEvent event) {
        // Detect slot change (C09).
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C09PacketHeldItemChange) {
            if (mc.thePlayer.ticksExisted - this.opalSlotChangeTick != 1) {
                this.opalRelease();
                this.opalResetCycle();
            }
            this.opalSlotChangeTick = mc.thePlayer.ticksExisted;
        }
        // Server says player finished eating (status=9).
        if (event.getType() == EventType.RECEIVE && event.getPacket() instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus statusPacket = (S19PacketEntityStatus) event.getPacket();
            if (statusPacket.getEntity(mc.theWorld) == mc.thePlayer && statusPacket.getOpCode() == 9) {
                this.opalRelease();
            }
        }
    }

    private void opalBlock() {
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword) {
            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
            this.opalBlocking = true;
        }
    }

    private void opalRelease() {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        this.opalBlocking = false;
    }

    private void opalResetCycle() {
        this.opalStopUse = false;
        this.opalRunThisTick = false;
        this.opalNextCycleTick = -1;
    }

    private boolean isMiauInteractableBlock(Block block) {
        return block instanceof net.minecraft.block.BlockDoor
                || block instanceof net.minecraft.block.BlockChest
                || block instanceof net.minecraft.block.BlockFurnace
                || block instanceof net.minecraft.block.BlockWorkbench
                || block instanceof net.minecraft.block.BlockAnvil
                || block instanceof net.minecraft.block.BlockEnchantmentTable
                || block instanceof net.minecraft.block.BlockBrewingStand
                || block instanceof net.minecraft.block.BlockBeacon
                || block instanceof net.minecraft.block.BlockLever
                || block instanceof net.minecraft.block.BlockButtonWood
                || block instanceof net.minecraft.block.BlockButtonStone
                || block instanceof net.minecraft.block.BlockTrapDoor
                || block instanceof net.minecraft.block.BlockFenceGate
                || block instanceof net.minecraft.block.BlockRedstoneRepeater
                || block instanceof net.minecraft.block.BlockRedstoneComparator
                || block instanceof net.minecraft.block.BlockHopper
                || block instanceof net.minecraft.block.BlockDropper
                || block instanceof net.minecraft.block.BlockDispenser
                || block instanceof net.minecraft.block.BlockEnderChest
                || block == Blocks.anvil
                || block == Blocks.enchanting_table
                || block == Blocks.brewing_stand;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{swordMotion.getValue() + "%"};
    }
}