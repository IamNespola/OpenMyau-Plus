package myau.module.modules;

import com.google.common.base.Predicates;
import myau.event.EventTarget;
import myau.events.RaytraceEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class Piercing extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty sortMode = new ModeProperty("sort-mode", 0, new String[]{"HURT_TIME", "HEALTH"});
    public final BooleanProperty ignoreBlocks = new BooleanProperty("ignore-blocks", false);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("ignore-teammates", true);
    public final BooleanProperty ignoreNonPlayers = new BooleanProperty("ignore-non-players", true);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapon-only", false);
    public final BooleanProperty insideHitboxOnly = new BooleanProperty("inside-hitbox-only", false);

    public Piercing() {
        super("Piercing", false);
    }

    @EventTarget
    public void onRaytrace(RaytraceEvent event) {
        this.modifyMouseOverFromGetMouseOver(1.0F);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.sortMode.getModeString()};
    }

    public boolean shouldOverrideMouseOver() {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }
        if (this.weaponOnly.getValue() && !this.holdingWeapon()) {
            return false;
        }
        return this.ignoreBlocks.getValue()
                || mc.objectMouseOver == null
                || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK;
    }

    public void modifyMouseOverFromGetMouseOver(float partialTicks) {
        if (this.shouldOverrideMouseOver()) {
            this.modifyMouseOverVanillaLook(partialTicks);
        }
    }

    private boolean holdingWeapon() {
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && (held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe);
    }

    private boolean shouldSkip(Entity entity) {
        if (entity == mc.thePlayer) {
            return true;
        }
        if (this.ignoreNonPlayers.getValue() && !(entity instanceof EntityPlayer)) {
            return true;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (this.ignoreTeammates.getValue() && TeamUtil.isSameTeam(player)) {
                return true;
            }
            if (TeamUtil.isBot(player) || TeamUtil.isFriend(player)) {
                return true;
            }
        }
        return false;
    }

    private void modifyMouseOverVanillaLook(final float partialTicks) {
        final Entity viewEntity = mc.getRenderViewEntity();
        if (viewEntity == null || mc.theWorld == null || mc.playerController == null) {
            return;
        }

        double reach = mc.playerController.getBlockReachDistance();
        final Vec3 eyes = viewEntity.getPositionEyes(partialTicks);
        if (mc.playerController.extendedReach()) {
            reach = 6.0D;
        }
        final Vec3 look = viewEntity.getLook(partialTicks);
        final Vec3 rayEnd = eyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        Entity best = null;
        Vec3 bestHit = null;
        double bestDist = Double.MAX_VALUE;
        boolean bestLiving = false;
        int bestHurt = Integer.MAX_VALUE;
        float bestHp = Float.POSITIVE_INFINITY;
        final int mode = this.sortMode.getValue();

        for (final Entity entity : mc.theWorld.getEntitiesInAABBexcluding(viewEntity,
                viewEntity.getEntityBoundingBox()
                        .addCoord(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach)
                        .expand(1.0D, 1.0D, 1.0D),
                Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith))) {
            if (this.shouldSkip(entity)) {
                continue;
            }

            final float border = entity.getCollisionBorderSize();
            final AxisAlignedBB bb = entity.getEntityBoundingBox().expand(border, border, border);
            final MovingObjectPosition hit = bb.calculateIntercept(eyes, rayEnd);
            final boolean inside = bb.isVecInside(eyes);

            if (!inside && hit == null) {
                continue;
            }
            double dist = inside ? 0.0D : eyes.distanceTo(hit.hitVec);
            if (!mc.playerController.extendedReach() && dist > 3.0D) {
                continue;
            }
            if (dist > reach || dist >= bestDist) {
                continue;
            }
            if (this.insideHitboxOnly.getValue() && dist > 0.10000000149011612D) {
                continue;
            }
            if (entity == viewEntity.ridingEntity && !viewEntity.canRiderInteract() && best != null) {
                continue;
            }

            boolean living = entity instanceof EntityLivingBase;
            int hurt = living ? ((EntityLivingBase) entity).hurtTime : Integer.MAX_VALUE;
            float hp = living ? ((EntityLivingBase) entity).getHealth() : Float.POSITIVE_INFINITY;

            boolean take = false;
            if (best == null || (living && !bestLiving)) {
                take = true;
            } else if (living == bestLiving) {
                if (!living) {
                    take = dist < bestDist;
                } else if (mode == 0) {
                    take = hurt < bestHurt || (hurt == bestHurt && dist < bestDist);
                } else {
                    take = hp < bestHp || (hp == bestHp && dist < bestDist);
                }
            }

            if (take) {
                best = entity;
                bestHit = inside ? (hit == null ? eyes : hit.hitVec) : hit.hitVec;
                bestDist = dist;
                bestLiving = living;
                bestHurt = hurt;
                bestHp = hp;
            }
        }

        if (best != null && reach > 3.0D && bestDist > 3.0D && !mc.playerController.extendedReach()) {
            mc.objectMouseOver = new MovingObjectPosition(
                    MovingObjectPosition.MovingObjectType.MISS, bestHit, null, new BlockPos(bestHit)
            );
            return;
        }

        if (best != null) {
            mc.objectMouseOver = new MovingObjectPosition(best, bestHit);
            if (best instanceof EntityLivingBase || best instanceof EntityItemFrame) {
                mc.pointedEntity = best;
            }
        }
    }
}