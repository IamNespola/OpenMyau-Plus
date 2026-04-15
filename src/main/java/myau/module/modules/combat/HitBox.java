package myau.module.modules.combat;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LeftClickMouseEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class HitBox extends Module {
    private MovingObjectPosition targetEntity = null;
    public final FloatProperty multiplier = new FloatProperty("multiplier", 1.2F, 1.0F, 5.0F);
    public final ModeProperty showHitbox = new ModeProperty("show-hitbox", 0, new String[]{"NONE", "PLAYERS", "MOBS", "ANIMALS", "ALL"});
    public final ColorProperty color = new ColorProperty("color", new Color(255, 255, 255).getRGB(), () -> this.showHitbox.getValue() != 0);
    public final BooleanProperty teams = new BooleanProperty("teams", true, () -> this.showHitbox.getValue() == 1 || this.showHitbox.getValue() == 4);
    public final BooleanProperty botCheck = new BooleanProperty("bot-check", true, () -> this.showHitbox.getValue() == 1 || this.showHitbox.getValue() == 4);

    public HitBox() {
        super("HitBox", Category.COMBAT,false);
    }

    public static float getExpansion(Entity entity) {
        HitBox hitBox = (HitBox) Myau.moduleManager.modules.get(HitBox.class);
        if (hitBox != null && hitBox.isEnabled() && entity instanceof EntityLivingBase) {
            return hitBox.multiplier.getValue();
        }
        return 1.0F;
    }

    private void calculateMouseOver(float partialTicks) {
        if (mc.getRenderViewEntity() != null && mc.theWorld != null) {
            mc.pointedEntity = null;
            Entity pointedEntity = null;
            double reach = 3.0;
            this.targetEntity = mc.getRenderViewEntity().rayTrace(reach, partialTicks);
            double distance = reach;
            Vec3 eyePos = mc.getRenderViewEntity().getPositionEyes(partialTicks);
            if (this.targetEntity != null) {
                distance = this.targetEntity.hitVec.distanceTo(eyePos);
            }
            Vec3 lookVec = mc.getRenderViewEntity().getLook(partialTicks);
            Vec3 reachVec = eyePos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
            Vec3 hitVec = null;
            float expansion = 1.0F;
            List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                    mc.getRenderViewEntity(),
                    mc.getRenderViewEntity()
                            .getEntityBoundingBox()
                            .addCoord(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach)
                            .expand(expansion, expansion, expansion)
            );
            double closestDistance = distance;
            for (Entity entity : entities) {
                if (entity.canBeCollidedWith()) {
                    float collisionSize = (float) ((double) entity.getCollisionBorderSize() * getExpansion(entity));
                    AxisAlignedBB expandedBox = entity.getEntityBoundingBox().expand(collisionSize, collisionSize, collisionSize);
                    MovingObjectPosition intercept = expandedBox.calculateIntercept(eyePos, reachVec);
                    if (expandedBox.isVecInside(eyePos)) {
                        if (0.0 < closestDistance || closestDistance == 0.0) {
                            pointedEntity = entity;
                            hitVec = intercept == null ? eyePos : intercept.hitVec;
                            closestDistance = 0.0;
                        }
                    } else if (intercept != null) {
                        double interceptDistance = eyePos.distanceTo(intercept.hitVec);
                        if (interceptDistance < closestDistance || closestDistance == 0.0) {
                            if (entity == mc.getRenderViewEntity().ridingEntity && !entity.canRiderInteract()) {
                                if (closestDistance == 0.0) {
                                    pointedEntity = entity;
                                    hitVec = intercept.hitVec;
                                }
                            } else {
                                pointedEntity = entity;
                                hitVec = intercept.hitVec;
                                closestDistance = interceptDistance;
                            }
                        }
                    }
                }
            }
            if (pointedEntity != null && (closestDistance < distance || this.targetEntity == null)) {
                this.targetEntity = new MovingObjectPosition(pointedEntity, hitVec);
                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
                    mc.pointedEntity = pointedEntity;
                }
            }
        }
    }

    private boolean shouldShowEntity(EntityLivingBase entity) {
        if (entity == mc.thePlayer) {
            return false;
        }
        if (entity.deathTime > 0 || entity instanceof EntityArmorStand || entity.isInvisible()) {
            return false;
        }
        if (mc.getRenderViewEntity().getDistanceToEntity(entity) > 128.0F) {
            return false;
        }
        if (!entity.ignoreFrustumCheck && !RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), 0.1F)) {
            return false;
        }
        switch (this.showHitbox.getValue()) {
            case 0:
                return false;
            case 1:
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (TeamUtil.isFriend(player)) {
                        return false;
                    }
                    if (this.teams.getValue() && TeamUtil.isSameTeam(player)) {
                        return false;
                    }
                    if (this.botCheck.getValue() && TeamUtil.isBot(player)) {
                        return false;
                    }
                    return true;
                }
                return false;
            case 2:
                if (entity instanceof EntityDragon || entity instanceof EntityWither) {
                    return true;
                }
                if (entity instanceof EntityMob || entity instanceof EntitySlime) {
                    return !(entity instanceof EntitySilverfish);
                }
                return false;
            case 3:
                return entity instanceof EntityAnimal
                        || entity instanceof EntityBat
                        || entity instanceof EntitySquid
                        || entity instanceof EntityVillager
                        || entity instanceof EntityIronGolem;
            case 4:
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (TeamUtil.isFriend(player)) {
                        return false;
                    }
                    if (this.teams.getValue() && TeamUtil.isSameTeam(player)) {
                        return false;
                    }
                    if (this.botCheck.getValue() && TeamUtil.isBot(player)) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.calculateMouseOver(1.0F);
        }
    }

    @EventTarget(Priority.HIGH)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !event.isCancelled() && this.targetEntity != null) {
            mc.objectMouseOver = this.targetEntity;
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && this.showHitbox.getValue() != 0) {
            List<EntityLivingBase> entities = mc.theWorld.loadedEntityList
                    .stream()
                    .filter(entity -> entity instanceof EntityLivingBase)
                    .map(entity -> (EntityLivingBase) entity)
                    .filter(this::shouldShowEntity)
                    .collect(Collectors.toList());
            if (!entities.isEmpty()) {
                RenderUtil.enableRenderState();
                Color renderColor = new Color(this.color.getValue());
                for (EntityLivingBase entity : entities) {
                    float collisionSize = (float) ((double) entity.getCollisionBorderSize() * this.multiplier.getValue());
                    AxisAlignedBB expandedBox = entity.getEntityBoundingBox().expand(collisionSize, collisionSize, collisionSize);
                    AxisAlignedBB offsetBox = new AxisAlignedBB(
                            expandedBox.minX - entity.posX + (RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX()),
                            expandedBox.minY - entity.posY + (RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()),
                            expandedBox.minZ - entity.posZ + (RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()),
                            expandedBox.maxX - entity.posX + (RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX()),
                            expandedBox.maxY - entity.posY + (RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()),
                            expandedBox.maxZ - entity.posZ + (RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ())
                    );
                    RenderUtil.drawBoundingBox(offsetBox, renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), 150, 1.5F);
                }
                RenderUtil.disableRenderState();
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%.1fx", this.multiplier.getValue())};
    }
}
