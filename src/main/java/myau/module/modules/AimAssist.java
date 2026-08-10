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
    public final FloatProperty hSpeed = new FloatProperty("horizontal-speed", 3.0F, 0.0F, 10.0F, () -> "Normal".equals(this.mode.getModeString()));
    public final FloatProperty vSpeed = new FloatProperty("vertical-speed", 0.0F, 0.0F, 10.0F, () -> "Normal".equals(this.mode.getModeString()));
    public final FloatProperty smoothing = new FloatProperty("smoothing", 50.0F, 0.0F, 100.0F, () -> "Normal".equals(this.mode.getModeString()));
    // LockOn 模式属性
    public final FloatProperty lockOnSpeed = new FloatProperty("lockon-speed", 5.0F, 0.1F, 10.0F, () -> "LockOn".equals(this.mode.getModeString()));
    public final FloatProperty lockOnSmooth = new FloatProperty("lockon-smooth", 50.0F, 0.0F, 100.0F, () -> "LockOn".equals(this.mode.getModeString()));
    public final FloatProperty headBoxExpand = new FloatProperty("head-box-expand", 1.0F, 0.5F, 2.0F, () -> "LockOn".equals(this.mode.getModeString()));
    public final BooleanProperty lockOnJitter = new BooleanProperty("lockon-jitter", false, () -> "LockOn".equals(this.mode.getModeString()));
    public final FloatProperty jitterRange = new FloatProperty("jitter-range", 1.0F, 0.1F, 2.0F, () -> "LockOn".equals(this.mode.getModeString()));
    // 通用属性
    public final FloatProperty range = new FloatProperty("range", 4.5F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("fov", 90, 30, 360);
    public final BooleanProperty weaponOnly = new BooleanProperty("weapons-only", true);
    public final BooleanProperty allowTools = new BooleanProperty("allow-tools", false, this.weaponOnly::getValue);
    public final BooleanProperty botChecks = new BooleanProperty("bot-check", true);
    public final BooleanProperty team = new BooleanProperty("teams", true);
    // LockOn 平滑滤波状态
    private double smoothX = 0, smoothY = 0;
    private EntityPlayer lockedTarget = null;

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
        return mc.theWorld.loadedEntityList.stream()
                .filter(entity -> entity instanceof EntityPlayer).map(entity -> (EntityPlayer) entity)
                .filter(this::isValidTarget)
                .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                .findFirst().orElse(null);
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
        if (this.weaponOnly.getValue() && !ItemUtil.isHoldingSword() && !(this.allowTools.getValue() && ItemUtil.isHoldingTool())) return;
        boolean attacking = PlayerUtil.isAttacking();
        if (!attacking || !this.isLookingAtBlock()) {
            if (attacking || !this.timer.hasTimeElapsed(350L)) {
                List<EntityPlayer> inRange = mc.theWorld.loadedEntityList.stream()
                        .filter(entity -> entity instanceof EntityPlayer).map(entity -> (EntityPlayer) entity)
                        .filter(this::isValidTarget)
                        .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                        .collect(Collectors.toList());
                if (!inRange.isEmpty()) {
                    if (inRange.stream().anyMatch(this::isInReach)) inRange.removeIf(entityPlayer -> !this.isInReach(entityPlayer));
                    EntityPlayer player = inRange.get(0);
                    if (!(RotationUtil.distanceToEntity(player) <= 0.0)) {
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
    }

    // ==================== LockOn 模式（屏幕坐标瞄准） ====================
    private void tickLockOn() {
        // 条件检查：只按下鼠标时自瞄
        if (!mc.gameSettings.keyBindAttack.isKeyDown()) return;
        // 破坏方块时不自瞄
        if (isLookingAtBlock()) return;
        // 武器检查
        if (this.weaponOnly.getValue() && !ItemUtil.isHoldingSword() && !(this.allowTools.getValue() && ItemUtil.isHoldingTool())) return;

        // 选择目标
        EntityPlayer target = lockedTarget;
        if (target == null || !isValidTarget(target)) {
            target = selectTarget();
            lockedTarget = target;
            // 重置滤波状态
            smoothX = 0; smoothY = 0;
        }
        if (target == null) return;

        // 1. 获取准星屏幕坐标（屏幕中心）
        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;
        double aimX = screenWidth / 2.0;
        double aimY = screenHeight / 2.0;

        // 2. 获取目标头部屏幕包围盒
        // 头部范围：从眼睛到头顶上方
        EntityPlayerSP player = mc.thePlayer;
        float partialTicks = 1.0F; // POST tick，用完整 tick
        Vec3 targetPos = new Vec3(
                target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks,
                target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks,
                target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks
        );
        Vec3 playerPos = new Vec3(
                player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks,
                player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.getEyeHeight(),
                player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
        );

        // 头部包围盒：从眼睛高度到眼睛高度+0.3（头部区域）
        double headHeight = 0.3;
        double headBase = targetPos.yCoord + target.getEyeHeight() - 0.1;
        double headTop = headBase + headHeight;
        double headBottom = headBase;

        // 将头部8个角投影到屏幕
        double headRadius = 0.15 * headBoxExpand.getValue(); // 头部半径，可缩放
        Vec3[] headCorners = new Vec3[]{
                new Vec3(targetPos.xCoord - headRadius, headBottom, targetPos.zCoord - headRadius),
                new Vec3(targetPos.xCoord + headRadius, headBottom, targetPos.zCoord - headRadius),
                new Vec3(targetPos.xCoord - headRadius, headBottom, targetPos.zCoord + headRadius),
                new Vec3(targetPos.xCoord + headRadius, headBottom, targetPos.zCoord + headRadius),
                new Vec3(targetPos.xCoord - headRadius, headTop, targetPos.zCoord - headRadius),
                new Vec3(targetPos.xCoord + headRadius, headTop, targetPos.zCoord - headRadius),
                new Vec3(targetPos.xCoord - headRadius, headTop, targetPos.zCoord + headRadius),
                new Vec3(targetPos.xCoord + headRadius, headTop, targetPos.zCoord + headRadius)
        };

        double headLeft = Double.MAX_VALUE, headRight = -Double.MAX_VALUE;
        double headScreenTop = Double.MAX_VALUE, headScreenBottom = -Double.MAX_VALUE;

        for (Vec3 corner : headCorners) {
            double[] screenPos = worldToScreen(corner, playerPos, player.rotationYaw, player.rotationPitch, partialTicks);
            if (screenPos == null) continue;
            if (screenPos[0] < headLeft) headLeft = screenPos[0];
            if (screenPos[0] > headRight) headRight = screenPos[0];
            if (screenPos[1] < headScreenTop) headScreenTop = screenPos[1];
            if (screenPos[1] > headScreenBottom) headScreenBottom = screenPos[1];
        }

        // 如果投影失败，回退到角度瞄准
        if (headLeft == Double.MAX_VALUE) {
            AxisAlignedBB box = target.getEntityBoundingBox();
            double border = target.getCollisionBorderSize();
            box = box.expand(border, border, border);
            float[] angles = RotationUtil.getRotationsToBox(box, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 180.0F, 0.0F);
            Myau.rotationManager.setRotation(angles[0], angles[1], 0, false);
            return;
        }

        // 3. 矩形死区判断
        boolean inBox = aimX >= headLeft && aimX <= headRight && aimY >= headScreenTop && aimY <= headScreenBottom;

        // === 物理隔离：手动自由区 ===
        if (inBox) {
            // 准星在头部框内，完全释放鼠标，不做任何位移
            smoothX = 0; smoothY = 0;
            return;
        }

        // === 自瞄回拉区 ===
        // 计算头部中心
        double cx = (headLeft + headRight) / 2.0;
        double cy = (headScreenTop + headScreenBottom) / 2.0;

        // 计算从准星指向中心的向量 (dx, dy)
        double dx = cx - aimX;
        double dy = cy - aimY;

        // 计算溢出距离：横向/纵向超出边界的距离
        double overflowX = 0, overflowY = 0;
        if (aimX < headLeft) overflowX = headLeft - aimX;
        else if (aimX > headRight) overflowX = aimX - headRight;
        if (aimY < headScreenTop) overflowY = headScreenTop - aimY;
        else if (aimY > headScreenBottom) overflowY = aimY - headScreenBottom;

        // 步长 = speed * max(横向溢出, 纵向溢出)
        double maxOverflow = Math.max(overflowX, overflowY);
        if (maxOverflow <= 0) return;
        double step = lockOnSpeed.getValue() * maxOverflow;

        // 归一化方向向量
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= 0) return;
        double normDx = dx / dist;
        double normDy = dy / dist;

        // 目标位移
        double targetX = normDx * step;
        double targetY = normDy * step;

        // 4. 平滑回拉：百分比平滑，与 Normal 一致
        double smoothFactor = lockOnSmooth.getValue() / 100.0;
        double moveX = targetX * smoothFactor;
        double moveY = targetY * smoothFactor;
        smoothX = moveX;
        smoothY = moveY;

        // 5. 随机抖动
        double jitterX = 0, jitterY = 0;
        if (lockOnJitter.getValue()) {
            double jRange = jitterRange.getValue();
            jitterX = (Math.random() * 2.0 - 1.0) * jRange;
            jitterY = (Math.random() * 2.0 - 1.0) * jRange;
        }

        double finalMoveX = smoothX + jitterX;
        double finalMoveY = smoothY + jitterY;

        // 6. 将屏幕像素位移转换为角度增量
        float sensitivity = mc.gameSettings.mouseSensitivity;
        float scale = 0.008F * sensitivity * sensitivity + 0.008F * sensitivity + 0.2F;
        // 垂直需要额外缩放（屏幕宽高比）
        float aspectRatio = (float) screenWidth / (float) screenHeight;
        float yawDelta = (float) (finalMoveX * scale);
        float pitchDelta = (float) (finalMoveY * scale * aspectRatio);

        // 应用角度
        Myau.rotationManager.setRotation(
                mc.thePlayer.rotationYaw + yawDelta,
                mc.thePlayer.rotationPitch - pitchDelta, // 屏幕Y向上，pitch向下为正
                0, false
        );
    }

    /**
     * 将世界坐标投影到屏幕坐标
     * 返回 [screenX, screenY] 或 null（在相机后方）
     */
    private double[] worldToScreen(Vec3 worldPos, Vec3 cameraPos, float cameraYaw, float cameraPitch, float partialTicks) {
        // 计算相对向量
        double dx = worldPos.xCoord - cameraPos.xCoord;
        double dy = worldPos.yCoord - cameraPos.yCoord;
        double dz = worldPos.zCoord - cameraPos.zCoord;

        // 转换为相机空间（先绕Y轴旋转，再绕X轴旋转）
        // 绕Y轴（偏航）：yaw = 0 时看向 -Z，所以需要旋转
        double yawRad = Math.toRadians(cameraYaw);
        double pitchRad = Math.toRadians(cameraPitch);

        // 第一步：绕Y轴旋转（偏航）
        double cosYaw = Math.cos(-yawRad);
        double sinYaw = Math.sin(-yawRad);
        double x1 = dx * cosYaw - dz * sinYaw;
        double z1 = dx * sinYaw + dz * cosYaw;
        double y1 = dy;

        // 第二步：绕X轴旋转（俯仰）
        double cosPitch = Math.cos(-pitchRad);
        double sinPitch = Math.sin(-pitchRad);
        double x2 = x1;
        double y2 = y1 * cosPitch - z1 * sinPitch;
        double z2 = y1 * sinPitch + z1 * cosPitch;

        // 在相机后方，不可见
        if (z2 <= 0) return null;

        // 透视投影到屏幕
        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;
        double fov = 70.0; // MC 默认 FOV 70
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
