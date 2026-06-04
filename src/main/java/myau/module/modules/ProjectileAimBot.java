package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render3DEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.RenderUtil;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.awt.Color;
import java.util.Comparator;

public class ProjectileAimBot extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty bow = new BooleanProperty("bow", true);
    public final BooleanProperty egg = new BooleanProperty("egg", true);
    public final BooleanProperty snowball = new BooleanProperty("snowball", true);
    public final BooleanProperty pearl = new BooleanProperty("ender-pearl", false);
    public final BooleanProperty otherItems = new BooleanProperty("other-items", false);
    public final FloatProperty range = new FloatProperty("range", 10.0F, 0.0F, 30.0F);
    public final BooleanProperty throughWalls = new BooleanProperty("through-walls", false);
    public final FloatProperty throughWallsRange = new FloatProperty("through-walls-range", 10.0F, 0.0F, 30.0F, () -> throughWalls.getValue());
    public final ModeProperty priority = new ModeProperty("priority", 2, new String[]{"Health", "Distance", "Direction"});
    public final ModeProperty gravityType = new ModeProperty("gravity-type", 1, new String[]{"None", "Projectile"});
    public final BooleanProperty predict = new BooleanProperty("predict", true, () -> gravityType.getValue() == 1);
    public final FloatProperty predictSize = new FloatProperty("predict-size", 2.0F, 0.1F, 5.0F, () -> predict.getValue() && gravityType.getValue() == 1);
    public final BooleanProperty mark = new BooleanProperty("mark", true);

    private EntityLivingBase target;

    public ProjectileAimBot() {
        super("ProjectileAimBot", false, false, "Aims throwable projectiles and bows at targets");
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || mc.thePlayer == null || mc.theWorld == null) return;

        target = null;
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || !isValidHeldItem(stack)) return;
        if (stack.getItem() instanceof ItemBow && !mc.thePlayer.isUsingItem()) return;

        target = getTarget();
        if (target == null) return;

        float[] rotations = gravityType.getValue() == 1
                ? getProjectileRotations(target, stack)
                : getDirectRotations(target);
        if (rotations == null) return;

        event.setRotation(rotations[0], rotations[1], 3);
        Myau.rotationManager.setRotation(rotations[0], rotations[1], 3, true);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || target == null || !mark.getValue()) return;
        drawPlatform(target, new Color(37, 126, 255, 70), event.getPartialTicks());
    }

    private boolean isValidHeldItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ItemBow) return bow.getValue();
        if (item instanceof ItemEgg) return egg.getValue();
        if (item instanceof ItemSnowball) return snowball.getValue();
        if (item instanceof ItemEnderPearl) return pearl.getValue();
        return otherItems.getValue();
    }

    private EntityLivingBase getTarget() {
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityLivingBase)
                .map(entity -> (EntityLivingBase) entity)
                .filter(this::isValidTarget)
                .min(Comparator.comparingDouble(this::priorityValue))
                .orElse(null);
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity == mc.thePlayer || entity.isDead || entity.getHealth() <= 0.0F) return false;
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).isPlayerSleeping()) return false;
        if (mc.thePlayer.getDistanceToEntity(entity) > range.getValue()) return false;
        if (throughWalls.getValue()) return mc.thePlayer.getDistanceToEntity(entity) <= throughWallsRange.getValue() || mc.thePlayer.canEntityBeSeen(entity);
        return mc.thePlayer.canEntityBeSeen(entity);
    }

    private double priorityValue(EntityLivingBase entity) {
        switch (priority.getValue()) {
            case 0:
                return entity.getHealth();
            case 1:
                return mc.thePlayer.getDistanceToEntity(entity);
            case 2:
            default:
                return Math.abs(MathHelper.wrapAngleTo180_float(getYawTo(entity) - mc.thePlayer.rotationYaw));
        }
    }

    private float[] getDirectRotations(EntityLivingBase entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        double x = entity.posX - eyes.xCoord;
        double y = entity.posY + entity.getEyeHeight() - eyes.yCoord;
        double z = entity.posZ - eyes.zCoord;
        return RotationUtil.getRotations(x, y, z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 180.0F, 0.0F);
    }

    private float[] getProjectileRotations(EntityLivingBase entity, ItemStack stack) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        double targetX = entity.posX;
        double targetY = entity.posY + entity.getEyeHeight();
        double targetZ = entity.posZ;

        if (predict.getValue()) {
            double predictTicks = predictSize.getValue();
            targetX += (entity.posX - entity.prevPosX) * predictTicks;
            targetY += (entity.posY - entity.prevPosY) * predictTicks;
            targetZ += (entity.posZ - entity.prevPosZ) * predictTicks;
        }

        double diffX = targetX - eyes.xCoord;
        double diffY = targetY - eyes.yCoord;
        double diffZ = targetZ - eyes.zCoord;
        double horizontal = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float velocity = getProjectileVelocity(stack);
        float gravity = getProjectileGravity(stack);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch;
        if (gravity <= 0.0F) {
            pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontal));
        } else {
            double velocitySq = velocity * velocity;
            double root = velocitySq * velocitySq - gravity * (gravity * horizontal * horizontal + 2.0D * diffY * velocitySq);
            if (root < 0.0D) return getDirectRotations(entity);
            pitch = (float) -Math.toDegrees(Math.atan((velocitySq - Math.sqrt(root)) / (gravity * horizontal)));
        }
        return RotationUtil.gcd(new float[]{yaw, MathHelper.clamp_float(pitch, -90.0F, 90.0F)}, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});
    }

    private float getProjectileVelocity(ItemStack stack) {
        if (stack.getItem() instanceof ItemBow) {
            int useDuration = mc.thePlayer.getItemInUseDuration();
            float charge = useDuration / 20.0F;
            charge = (charge * charge + charge * 2.0F) / 3.0F;
            return Math.min(charge, 1.0F) * 3.0F;
        }
        return 0.5F;
    }

    private float getProjectileGravity(ItemStack stack) {
        return stack.getItem() instanceof ItemBow ? 0.05F : 0.03F;
    }

    private float getYawTo(Entity entity) {
        double x = entity.posX - mc.thePlayer.posX;
        double z = entity.posZ - mc.thePlayer.posZ;
        return (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
    }

    private void drawPlatform(Entity entity, Color color, float partialTicks) {
        double renderX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double renderY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double renderZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double radius = Math.max(box.maxX - box.minX, box.maxZ - box.minZ) * 0.75D;

        GlStateManager.pushMatrix();
        GlStateManager.translate(renderX, renderY + 0.02D, renderZ);
        RenderUtil.drawLine((float) -radius, 0.0F, (float) radius, 0.0F, 2.0F, color.getRGB());
        RenderUtil.drawLine(0.0F, (float) -radius, 0.0F, (float) radius, 2.0F, color.getRGB());
        GlStateManager.popMatrix();
    }

    public boolean hasTarget() {
        return target != null && mc.thePlayer != null && mc.thePlayer.canEntityBeSeen(target);
    }

    @Override
    public void onDisabled() {
        target = null;
    }
}
