package keystrokesmod.script.classes;

import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Entity {
    public net.minecraft.entity.Entity entity;
    public String type;
    public int entityId;
    public boolean isLiving;
    public boolean isPlayer;
    public boolean isUser;
    private static HashMap<Integer, Entity> cache = new HashMap<>();

    public Entity(net.minecraft.entity.Entity entity) {
        this.entity = entity;
        if (entity == null) {
            return;
        }
        this.type = entity.getClass().getSimpleName();
        this.entityId = entity.getEntityId();
        this.isLiving = entity instanceof EntityLivingBase;
        this.isPlayer = entity instanceof EntityPlayer;
        if (this.isPlayer && Minecraft.getMinecraft().thePlayer != null && entity.getUniqueID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
            this.isUser = true;
        }
    }

    public static Entity convert(net.minecraft.entity.Entity entity) {
        if (entity == null) {
            return null;
        }
        int id = entity.getEntityId() + System.identityHashCode(entity);
        Entity cachedEntity = cache.get(id);

        if (cachedEntity == null) {
            cachedEntity = new Entity(entity);
            cache.put(id, cachedEntity);
        }

        return cachedEntity;
    }

    public static void clearCache() {
        cache.clear();
    }

    public boolean allowEditing() {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }
        return (((EntityPlayer) entity).capabilities.allowEdit);
    }

    public double distanceTo(Vec3 position) {
        return entity.getDistance(position.x, position.y, position.z);
    }

    public double distanceToSq(Vec3 position) {
        return entity.getDistanceSq(position.x, position.y, position.z);
    }

    public double distanceToGround() {
        return Utils.distanceToGround(entity);
    }

    public boolean isHoldingBlock() {
        return this.isLiving && ((EntityLivingBase)this.entity).getHeldItem() != null && ((EntityLivingBase)this.entity).getHeldItem().getItem() instanceof ItemBlock;
    }

    public boolean isHoldingWeapon() {
        return this.isLiving && Utils.holdingWeapon((EntityLivingBase)this.entity);
    }

    public float getAbsorption() {
        if (!(entity instanceof EntityLivingBase)) {
            return -1;
        }
        return (((EntityLivingBase) entity).getAbsorptionAmount());
    }

    public Vec3 getBlockPosition() {
        return new Vec3(entity.getPosition().getX(), entity.getPosition().getY(), entity.getPosition().getZ());
    }

    public String getDisplayName() {
        if (this.entity instanceof EntityItem) {
            return ((EntityItem)this.entity).getEntityItem().getDisplayName();
        }
        return this.entity.getDisplayName().getUnformattedText();
    }

    public Entity getRidingEntity() {
        return Entity.convert(this.entity.ridingEntity);
    }

    public Entity getRiddenByEntity() {
        return Entity.convert(this.entity.riddenByEntity);
    }

    public Vec3i getServerPosition() {
        return new Vec3i(entity.serverPosX, entity.serverPosY, entity.serverPosZ);
    }

    public double distanceEyesToClosest(Entity other) {
        final AxisAlignedBB box = other.entity.getEntityBoundingBox();
        return this.entity.getPositionEyes(1.0f).distanceTo(new net.minecraft.util.Vec3(
                Math.max(box.minX, Math.min(box.maxX, this.getX())),
                Math.max(box.minY, Math.min(box.maxY, this.getY())),
                Math.max(box.minZ, Math.min(box.maxZ, this.getZ()))
        ));
    }

    public int getExperienceLevel() {
        if (!(entity instanceof EntityPlayer)) {
            return 0;
        }
        return ((EntityPlayer) entity).experienceLevel;
    }

    public float getExperience() {
        if (!(entity instanceof EntityPlayer)) {
            return 0;
        }
        return ((EntityPlayer) entity).experience;
    }

    public float getFallDistance() {
        return entity.fallDistance;
    }

    public String getUUID() {
        return this.entity.getUniqueID().toString();
    }

    public String getCustomNameTag() {
        return this.entity.getCustomNameTag();
    }

    public double getBPS() {
        if (!this.isLiving) {
            return 0.0;
        }
        double x = this.entity.posX - this.entity.prevPosX;
        double z = this.entity.posZ - this.entity.prevPosZ;
        return Math.sqrt(x * x + z * z) * 20.0;
    }

    public String getFacing() {
        return this.entity.getHorizontalFacing().name();
    }

    public float getHealth() {
        if (!(entity instanceof EntityLivingBase)) {
            return -1;
        }
        return ((EntityLivingBase) entity).getHealth();
    }

    public boolean isSleeping() {
        if (this.isPlayer) {
            return ((EntityPlayer) this.entity).isPlayerSleeping();
        }
        return false;
    }

    public float getEyeHeight() {
        return entity.getEyeHeight();
    }

    public float getHeight() {
        return entity.height;
    }

    public float getWidth() {
        return entity.width;
    }

    public boolean isBurning() {
        return entity.isBurning();
    }

    public ItemStack getHeldItem() {
        if (entity instanceof EntityItem) {
            net.minecraft.item.ItemStack item = ((EntityItem) entity).getEntityItem();
            if (item == null) {
                return null;
            }
            return new ItemStack(item, (byte) 0);
        }
        else if (!(entity instanceof EntityLivingBase)) {
            return null;
        }
        net.minecraft.item.ItemStack stack = ((EntityLivingBase) entity).getHeldItem();
        if (stack == null) {
            return null;
        }
        return new ItemStack(stack, (byte) 0);
    }

    public int getHurtTime() {
        if (!(entity instanceof EntityLivingBase)) {
            return -1;
        }
        return ((EntityLivingBase) entity).hurtTime;
    }

    public boolean isConsuming() {
        return Utils.isConsuming(this.entity);
    }

    public Vec3 getLastPosition() {
        return new Vec3(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ);
    }

    public float getMaxHealth() {
        if (!(entity instanceof EntityLivingBase)) {
            return -1;
        }
        return ((EntityLivingBase) entity).getMaxHealth();
    }

    public int getMaxHurtTime() {
        if (!(entity instanceof EntityLivingBase)) {
            return -1;
        }
        return ((EntityLivingBase) entity).maxHurtTime;
    }

    public String getName() {
        if (entity instanceof EntityItem) {
            return ((EntityItem) entity).getEntityItem().getItem().getRegistryName().substring(10);
        }
        return entity.getName();
    }

    public NetworkPlayer getNetworkPlayer() {
        return NetworkPlayer.convert(Minecraft.getMinecraft().getNetHandler().getPlayerInfo(this.entity.getUniqueID()));
    }

    public float getPitch() {
        return entity.rotationPitch;
    }

    public Vec3 getPosition() {
        if (entity == null) {
            return null;
        }
        return new Vec3(entity.posX, entity.posY, entity.posZ);
    }

    public List<Object[]> getPotionEffects() {
        List<Object[]> potionEffects = new ArrayList<>();
        if (!(entity instanceof EntityLivingBase)) {
            return potionEffects;
        }
        for (PotionEffect potionEffect : ((EntityLivingBase) entity).getActivePotionEffects()) {
            Object[] potionData = new Object[]{potionEffect.getPotionID(), potionEffect.getEffectName(), potionEffect.getAmplifier(), potionEffect.getDuration()};
            potionEffects.add(potionData);
        }
        return potionEffects;
    }

    public ItemStack getArmorInSlot(final int slot) {
        return (this.isPlayer && slot >= 0 && slot <= 3) ? ItemStack.convert(((EntityPlayer)this.entity).inventory.armorInventory[slot]) : null;
    }

    public double getSpeed() {
        return Utils.getHorizontalSpeed(entity);
    }

    public int getSwingProgress() {
        return this.isLiving ? ((EntityLivingBase)this.entity).swingProgressInt : -1;
    }

    public int getTicksExisted() {
        return entity.ticksExisted;
    }

    public float getYaw() {
        return entity.rotationYaw;
    }

    public int getFireResistance() {
        return this.entity.fireResistance;
    }

    public float getPrevYaw() {
        return entity.prevRotationYaw;
    }

    public float getPrevPitch() {
        return entity.prevRotationPitch;
    }

    public double getPrevX() {
        return entity.prevPosX;
    }

    public double getPrevY() {
        return entity.prevPosY;
    }

    public double getPrevZ() {
        return entity.prevPosZ;
    }

    public double getX() {
        return entity.posX;
    }

    public double getY() {
        return entity.posY;
    }

    public double getZ() {
        return entity.posZ;
    }

    public void setMotionX(final double motion) {
        entity.motionX = motion;
    }

    public void setMotionY(final double motion) {
        entity.motionY = motion;
    }

    public void setMotionZ(final double motion) {
        entity.motionZ = motion;
    }

    public boolean isCreative() {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }
        return (((EntityPlayer) entity).capabilities.isCreativeMode);
    }

    public boolean isCollided() {
        if (!(entity instanceof EntityPlayer)) {
            return Minecraft.getMinecraft().theWorld.checkBlockCollision(this.entity.getEntityBoundingBox().expand(0.05, 0.0, 0.05));
        }
        return entity.isCollided;
    }

    public boolean isCollidedHorizontally() {
        return entity.isCollidedHorizontally;
    }

    public boolean isCollidedVertically() {
        return entity.isCollidedVertically;
    }

    public boolean isDead() {
        return this.entity.isDead || (this.isLiving && ((EntityLivingBase)this.entity).deathTime > 0);
    }

    public int getHunger() {
        if (!this.isPlayer || ((EntityPlayer) this.entity).getFoodStats() == null) {
            return 0;
        }
        return ((EntityPlayer) this.entity).getFoodStats().getFoodLevel();
    }

    public float getSaturation() {
        if (!this.isPlayer || ((EntityPlayer) this.entity).getFoodStats() == null) {
            return 0.0f;
        }
        return ((EntityPlayer) this.entity).getFoodStats().getSaturationLevel();
    }

    public float getAir() {
        return this.entity.getAir();
    }

    public boolean isInvisible() {
        return entity.isInvisible();
    }

    public boolean isInWater() {
        return entity.isInWater();
    }

    public boolean isInLava() {
        return entity.isInLava();
    }

    public Entity getFisher() {
        if (this.entity instanceof EntityFishHook) {
            return convert(((EntityFishHook) this.entity).angler);
        }
        return null;
    }

    public boolean isInLiquid() {
        return !this.entity.isOffsetPositionInLiquid(0, 0, 0);
    }

    public boolean isOnLadder() {
        if (this.isLiving) {
            if (((EntityLivingBase) this.entity).isOnLadder()) {
                return true;
            }
        }
        return false;
    }

    public boolean isOnEdge() {
        return Utils.onEdge(this.entity);
    }

    public boolean isSprinting() {
        return entity.isSprinting();
    }

    public boolean isSneaking() {
        return entity.isSneaking();
    }

    public boolean isUsingItem() {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }
        return (((EntityPlayer) entity).isUsingItem());
    }

    public boolean onGround() {
        return entity.onGround;
    }

    public void setMotion(double x, double y, double z) {
        entity.motionX = x;
        entity.motionY = y;
        entity.motionZ = z;
    }

    public Vec3 getMotion() {
        return new Vec3(entity.motionX, entity.motionY, entity.motionZ);
    }

    public void setPitch(float pitch) {
        entity.rotationPitch = pitch;
    }

    public void setYaw(float yaw) {
        entity.rotationYaw = yaw;
    }

    public void setPosition(Vec3 position) {
        entity.setPosition(position.x, position.y, position.z);
    }

    public void setPosition(double x, double y, double z) {
        entity.setPosition(x, y, z);
    }

    public void setLastTickPosition(double x, double y, double z) {
        entity.lastTickPosX = x;
        entity.lastTickPosY = y;
        entity.lastTickPosZ = z;
    }

    public void setPositionRaw(double x, double y, double z) {
        entity.posX = x;
        entity.posY = y;
        entity.posZ = z;
    }

    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean bl) {
        entity.setPositionAndRotation2(x, y, z, yaw, pitch, posRotationIncrements, bl);
    }

    public void setServerPosition(int x, int y, int z) {
        entity.serverPosX = x;
        entity.serverPosY = y;
        entity.serverPosZ = z;
    }

    public void setServerPosition(Vec3i vec3i) {
        setServerPosition(vec3i.x, vec3i.y, vec3i.z);
    }

    public void onUpdate() {
        entity.onUpdate();
    }
}
