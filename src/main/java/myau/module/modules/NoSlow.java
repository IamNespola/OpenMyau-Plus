package myau.module.modules;

import myau.Myau;
import myau.enums.FloatModules;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.LivingUpdateEvent;
import myau.events.PlayerUpdateEvent;
import myau.events.RightClickMouseEvent;
import myau.module.Module;
import myau.util.BlockUtil;
import myau.util.ItemUtil;
import myau.util.PlayerUtil;
import myau.util.TeamUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.BlockPos;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty swordMode = new ModeProperty("sword-mode", 1, new String[]{"NONE", "VANILLA", "GRIM"});
    public final PercentProperty swordMotion = new PercentProperty("sword-motion", 100, () -> this.swordMode.getValue() == 1);
    public final BooleanProperty swordSprint = new BooleanProperty("sword-sprint", true, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty killauraonly = new BooleanProperty("killaura-only", false, () -> this.swordMode.getValue() != 0);
    public final ModeProperty foodMode = new ModeProperty("food-mode", 0, new String[]{"NONE", "VANILLA", "GRIM"});
    public final PercentProperty foodMotion = new PercentProperty("food-motion", 100, () -> this.foodMode.getValue() == 1);
    public final BooleanProperty foodSprint = new BooleanProperty("food-sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeProperty bowMode = new ModeProperty("bow-mode", 0, new String[]{"NONE", "VANILLA", "GRIM"});
    public final PercentProperty bowMotion = new PercentProperty("bow-motion", 100, () -> this.bowMode.getValue() == 1);
    public final BooleanProperty bowSprint = new BooleanProperty("bow-sprint", true, () -> this.bowMode.getValue() != 0);
    private int count;

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killauraonly.getValue()) {
            if (!killAura.isEnabled()) return false;
            if (killAura.getTarget() == null) return false;
        }
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return (this.isSwordActive() && this.swordSprint.getValue())
                || (this.isFoodActive() && this.foodSprint.getValue())
                || (this.isBowActive() && this.bowSprint.getValue());
    }

    public int getMotionMultiplier() {
        count++;
        if (ItemUtil.isHoldingSword()) {
            if (swordMode.getValue() == 2) {
                if (count % 2 == 0) {
                    return 100;
                } else {
                    return 20;
                }
            }
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            if (foodMode.getValue() == 2) {
                if (count % 2 == 0) {
                    return 100;
                } else {
                    return 20;
                }
            }
            return this.foodMotion.getValue();
        } else if (ItemUtil.isUsingBow()) {
            if (bowMode.getValue() == 2) {
                if (count % 2 == 0) {
                    return 100;
                } else {
                    return 20;
                }
            }
            return this.bowMotion.getValue();
        }
        return 100;
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