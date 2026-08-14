package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.util.*;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Normal", "LockOn"});
    // Normal 模式属性
    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 3.0F, 0.0F, 10.0F);
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 0.0F, 0.0F, 10.0F);
    public final FloatProperty smoothing = new FloatProperty("smoothing", 50.0F, 0.0F, 100.0F);
    // LockOn 模式属性
    public final ModeProperty safeZone = new ModeProperty("safe-zone", 1, new String[]{"Head", "Torso", "Feet"}, () -> "LockOn".equals(this.mode.getModeString()));
    public final BooleanProperty noiseEnabled = new BooleanProperty("noise", false, () -> "LockOn".equals(this.mode.getModeString()));
    public final FloatProperty noiseMinYaw = new FloatProperty("noise-min-yaw", 0.0F, 0.0F, 5.0F, () -> "LockOn".equals(this.mode.getModeString()) && this.noiseEnabled.getValue());
    public final FloatProperty noiseMaxYaw = new FloatProperty("noise-max-yaw", 1.0F, 0.0F, 5.0F, () -> "LockOn".equals(this.mode.getModeString()) && this.noiseEnabled.getValue());
    public final FloatProperty noiseMinPitch = new FloatProperty("noise-min-pitch", 0.0F, 0.0F, 5.0F, () -> "LockOn".equals(this.mode.getModeString()) && this.noiseEnabled.getValue());
    public final FloatProperty noiseMaxPitch = new FloatProperty("noise-max-pitch", 1.0F, 0.0F, 5.0F, () -> "LockOn".equals(this.mode.getModeString()) && this.noiseEnabled.getValue());
    public final FloatProperty noiseSpeed = new FloatProperty("noise-speed", 1.0F, 0.1F, 10.0F, () -> "LockOn".equals(this.mode.getModeString()) && this.noiseEnabled.getValue());
    // Noise 状态
    private double noiseYaw = 0, noisePitch = 0;
    private long lastNoiseTime = 0;
    // 通用属性
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 90, 30, 360);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponOnly::getValue);
    public final BooleanProperty botChecks = new BooleanProperty("bot-check", true);
    public final BooleanProperty team = new BooleanProperty("teams", true);
    private boolean isValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer != mc.thePlayer && entityPlayer != mc.thePlayer.ridingEntity) {
            if (entityPlayer == mc.getRenderViewEntity() || entityPlayer == mc.getRenderViewEntity().ridingEntity) return false;
            else if (entityPlayer.deathTime > 0) return false;
            else if (RotationUtil.distanceToEntity(entityPlayer) > (double) this.range.getValue()) return false;
            else if (RotationUtil.angleToEntity(entityPlayer) > (float) this.fov.getValue()) return false;
            else if (RotationUtil.rayTrace(entityPlayer) != null) return false;
            else if (TeamUtil.isFriend(entityPlayer)) return false;
            else return (!this.team.getValue() || !TeamUtil.isSameTeam(entityPlayer)) && (!this.botChecks.getValue() || !TeamUtil.isBot(entityPlayer));
        } else return false;
    }

    private boolean isInReach(EntityPlayer entityPlayer) {
        Reach reach = (Reach) Myau.moduleManager.modules.get(Reach.class);
        double distance = reach.isEnabled() ? (double) reach.range.getValue() : 3.0;
        return RotationUtil.distanceToEntity(entityPlayer) <= distance;
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    private EntityPlayer selectTarget() {
        List<EntityPlayer> inRange = mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer).map(entity -> (EntityPlayer) entity)
                .filter(this::isValidTarget)
                .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                .collect(Collectors.toList());
        if (inRange.isEmpty()) return null;
        if (inRange.stream().anyMatch(this::isInReach)) inRange.removeIf(entityPlayer -> !this.isInReach(entityPlayer));
        return inRange.get(0);
    }

    public AimAssist() {
        super("AimAssist", false, false, "Assists your aim by subtly adjusting your view towards nearby targets when attacking.");
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.currentScreen != null) return;
        if ("Normal".equals(this.mode.getModeString())) tickNormal();
        else tickLockOn();
    }

    // ==================== Normal 模式 ====================
    private void tickNormal() {
        if (this.weaponOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant() && !(this.allowTools.getValue() && ItemUtil.isHoldingTool())) return;
        boolean attacking = PlayerUtil.isAttacking();
        if (!attacking || !this.isLookingAtBlock()) {
            if (attacking || !this.timer.hasTimeElapsed(350L)) {
                EntityPlayer player = selectTarget();
                if (player != null && !(RotationUtil.distanceToEntity(player) <= 0.0)) {
                    AxisAlignedBB axisAlignedBB = player.getEntityBoundingBox();
                    double collisionBorderSize = player.getCollisionBorderSize();
                    float[] rotation = RotationUtil.getRotationsToBox(
                            axisAlignedBB.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize),
                            mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 180.0F,
                            (float) this.smoothing.getValue() / 100.0F);
                    float yaw = Math.min(Math.abs(this.hSpeed.getValue()), 10.0F);
                    float pitch = Math.min(Math.abs(this.vSpeed.getValue()), 10.0F);
                    Myau.rotationManager.setRotation(
                            mc.thePlayer.rotationYaw + (rotation[0] - mc.thePlayer.rotationYaw) * 0.1F * yaw,
                            mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch, 0, false);
                }
            }
        }
    }

    // ==================== LockOn 模式（部位死区 + Noise + 角度瞄准） ====================
    private void tickLockOn() {
        if (!mc.gameSettings.keyBindAttack.isKeyDown()) return;
        if (isLookingAtBlock()) return;
        if (this.weaponOnly.getValue() && !ItemUtil.hasRawUnbreakingEnchant() && !(this.allowTools.getValue() && ItemUtil.isHoldingTool())) return;

        EntityPlayer target = selectTarget();
        if (target == null) return;

        // 1. 将人体分为头/躯干/腿三部分（参考 KillAura）
        AxisAlignedBB fullBox = target.getEntityBoundingBox();
        double bbHeight = fullBox.maxY - fullBox.minY;
        double headSize = bbHeight / 4.5F;
        double torsoSize = bbHeight / 2.75F;
        AxisAlignedBB headBox = new AxisAlignedBB(fullBox.minX, fullBox.maxY - headSize, fullBox.minZ, fullBox.maxX, fullBox.maxY, fullBox.maxZ);
        AxisAlignedBB torsoBox = new AxisAlignedBB(fullBox.minX, fullBox.minY + torsoSize, fullBox.minZ, fullBox.maxX, fullBox.maxY - headSize, fullBox.maxZ);
        AxisAlignedBB feetBox = new AxisAlignedBB(fullBox.minX, fullBox.minY, fullBox.minZ, fullBox.maxX, fullBox.minY + torsoSize, fullBox.maxZ);

        // 根据safeZone选择死区
        AxisAlignedBB safeBox;
        String zone = this.safeZone.getModeString();
        if ("Head".equals(zone)) safeBox = headBox;
        else if ("Feet".equals(zone)) safeBox = feetBox;
        else safeBox = torsoBox;

        // 2. 将安全区8角投影到屏幕，判断准星是否在框内
        EntityPlayerSP player = mc.thePlayer;
        float partialTicks = 1.0F;
        Vec3 playerPos = new Vec3(
                player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks,
                player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.getEyeHeight(),
                player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
        );

        Vec3[] safeCorners = new Vec3[]{
                new Vec3(safeBox.minX, safeBox.minY, safeBox.minZ),
                new Vec3(safeBox.maxX, safeBox.minY, safeBox.minZ),
                new Vec3(safeBox.minX, safeBox.minY, safeBox.maxZ),
                new Vec3(safeBox.maxX, safeBox.minY, safeBox.maxZ),
                new Vec3(safeBox.minX, safeBox.maxY, safeBox.minZ),
                new Vec3(safeBox.maxX, safeBox.maxY, safeBox.minZ),
                new Vec3(safeBox.minX, safeBox.maxY, safeBox.maxZ),
                new Vec3(safeBox.maxX, safeBox.maxY, safeBox.maxZ)
        };

        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;
        double aimX = screenWidth / 2.0;
        double aimY = screenHeight / 2.0;

        double boxLeft = Double.MAX_VALUE, boxRight = -Double.MAX_VALUE;
        double boxTop = Double.MAX_VALUE, boxBottom = -Double.MAX_VALUE;

        for (Vec3 corner : safeCorners) {
            double[] screenPos = worldToScreen(corner, playerPos, player.rotationYaw, player.rotationPitch);
            if (screenPos == null) continue;
            if (screenPos[0] < boxLeft) boxLeft = screenPos[0];
            if (screenPos[0] > boxRight) boxRight = screenPos[0];
            if (screenPos[1] < boxTop) boxTop = screenPos[1];
            if (screenPos[1] > boxBottom) boxBottom = screenPos[1];
        }

        // 3. 矩形死区判断：准星在安全区 → 零吸附
        boolean inBox = boxLeft != Double.MAX_VALUE
                && aimX >= boxLeft && aimX <= boxRight
                && aimY >= boxTop && aimY <= boxBottom;

        // === 物理隔离：手动自由区 ===
        if (inBox) return;

        // === 非自由区：角度瞄准（与 Normal 一致） ===
        double border = target.getCollisionBorderSize();
        AxisAlignedBB aimBox = safeBox.expand(border, border, border);
        float[] rotation = RotationUtil.getRotationsToBox(
                aimBox,
                mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 180.0F,
                (float) this.smoothing.getValue() / 100.0F);
        float yaw = Math.min(Math.abs(this.hSpeed.getValue()), 10.0F);
        float pitch = Math.min(Math.abs(this.vSpeed.getValue()), 10.0F);
        float targetYaw = mc.thePlayer.rotationYaw + (rotation[0] - mc.thePlayer.rotationYaw) * 0.1F * yaw;
        float targetPitch = mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch;

        // === Noise 随机偏移 ===
        if (noiseEnabled.getValue()) {
            updateNoise();
            targetYaw += (float) noiseYaw;
            targetPitch += (float) noisePitch;
        }

        Myau.rotationManager.setRotation(targetYaw, targetPitch, 0, false);
    }

    /**
     * 更新 Noise 随机偏移（按 noise-speed 控制变化频率）
     */
    private void updateNoise() {
        long now = System.currentTimeMillis();
        long interval = (long) (1000.0 / noiseSpeed.getValue());
        if (now - lastNoiseTime >= interval) {
            lastNoiseTime = now;
            double minYaw = noiseMinYaw.getValue();
            double maxYaw = noiseMaxYaw.getValue();
            double minPitch = noiseMinPitch.getValue();
            double maxPitch = noiseMaxPitch.getValue();
            noiseYaw = minYaw + Math.random() * (maxYaw - minYaw);
            if (Math.random() < 0.5) noiseYaw = -noiseYaw;
            noisePitch = minPitch + Math.random() * (maxPitch - minPitch);
            if (Math.random() < 0.5) noisePitch = -noisePitch;
        }
    }

    /**
     * 将世界坐标投影到屏幕坐标
     * 返回 [screenX, screenY] 或 null（在相机后方）
     */
    private double[] worldToScreen(Vec3 worldPos, Vec3 cameraPos, float cameraYaw, float cameraPitch) {
        double dx = worldPos.xCoord - cameraPos.xCoord;
        double dy = worldPos.yCoord - cameraPos.yCoord;
        double dz = worldPos.zCoord - cameraPos.zCoord;

        double yawRad = Math.toRadians(cameraYaw);
        double pitchRad = Math.toRadians(cameraPitch);

        double cosYaw = Math.cos(-yawRad);
        double sinYaw = Math.sin(-yawRad);
        double x1 = dx * cosYaw - dz * sinYaw;
        double z1 = dx * sinYaw + dz * cosYaw;
        double y1 = dy;

        double cosPitch = Math.cos(-pitchRad);
        double sinPitch = Math.sin(-pitchRad);
        double x2 = x1;
        double y2 = y1 * cosPitch - z1 * sinPitch;
        double z2 = y1 * sinPitch + z1 * cosPitch;

        if (z2 <= 0) return null;

        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;
        double fov = 70.0;
        double fovRad = Math.toRadians(fov);
        double aspect = (double) screenWidth / (double) screenHeight;
        double tanHalfFov = Math.tan(fovRad / 2.0);

        double screenX = (x2 / z2) / (tanHalfFov * aspect) * (screenWidth / 2.0) + screenWidth / 2.0;
        double screenY = (y2 / z2) / tanHalfFov * (screenHeight / 2.0) + screenHeight / 2.0;

        return new double[]{screenX, screenY};
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode() && !Myau.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            this.timer.reset();
        }
    }
}