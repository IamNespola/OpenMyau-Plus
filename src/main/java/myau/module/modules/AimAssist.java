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
    public final FloatProperty smoothing = new FloatProperty("smoothing", 50.0F, 0.0F, 100.0F);
    // LockOn 模式属性
    public final FloatProperty lockOnSpeed = new FloatProperty("lockon-speed", 5.0F, 0.0F, 10.0F, () -> "LockOn".equals(this.mode.getModeString()));
    public final FloatProperty headBoxExpand = new FloatProperty("head-box-expand", 1.0F, 0.5F, 2.0F, () -> "LockOn".equals(this.mode.getModeString()));
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
        if (this.weaponOnly.getValue() && !ItemUtil.isHoldingSword() && !(this.allowTools.getValue() && ItemUtil.isHoldingTool())) return;
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

    // ==================== LockOn 模式（屏幕死区 + 角度瞄准） ====================
    private void tickLockOn() {
        // 条件检查：只按下鼠标时自瞄
        if (!mc.gameSettings.keyBindAttack.isKeyDown()) return;
        // 破坏方块时不自瞄
        if (isLookingAtBlock()) return;
        // 武器检查
        if (this.weaponOnly.getValue() && !ItemUtil.isHoldingSword() && !(this.allowTools.getValue() && ItemUtil.isHoldingTool())) return;

        // 选人：与 Normal 一致
        EntityPlayer target = selectTarget();
        if (target == null) return;

        // 1. 获取准星屏幕坐标（屏幕中心）
        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;
        double aimX = screenWidth / 2.0;
        double aimY = screenHeight / 2.0;

        // 2. 获取目标头部屏幕包围盒
        EntityPlayerSP player = mc.thePlayer;
        float partialTicks = 1.0F;
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

        double headHeight = 0.3;
        double headBase = targetPos.yCoord + target.getEyeHeight() - 0.1;
        double headTop = headBase + headHeight;
        double headBottom = headBase;
        double headRadius = 0.15 * headBoxExpand.getValue();

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
            double[] screenPos = worldToScreen(corner, playerPos, player.rotationYaw, player.rotationPitch);
            if (screenPos == null) continue;
            if (screenPos[0] < headLeft) headLeft = screenPos[0];
            if (screenPos[0] > headRight) headRight = screenPos[0];
            if (screenPos[1] < headScreenTop) headScreenTop = screenPos[1];
            if (screenPos[1] > headScreenBottom) headScreenBottom = screenPos[1];
        }

        // 3. 矩形死区判断
        boolean inBox = headLeft != Double.MAX_VALUE
                && aimX >= headLeft && aimX <= headRight
                && aimY >= headScreenTop && aimY <= headScreenBottom;

        // === 物理隔离：手动自由区 ===
        if (inBox) return; // 准星在头部框内，零吸附

        // === 非自由区：采用 Normal 的角度瞄准逻辑，只瞄头 ===
        AxisAlignedBB headBox = new AxisAlignedBB(
                targetPos.xCoord - headRadius, headBottom, targetPos.zCoord - headRadius,
                targetPos.xCoord + headRadius, headTop, targetPos.zCoord + headRadius
        );
        double border = target.getCollisionBorderSize();
        headBox = headBox.expand(border, border, border);
        float[] rotation = RotationUtil.getRotationsToBox(
                headBox,
                mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 180.0F,
                (float) this.smoothing.getValue() / 100.0F);
        float speed = Math.min(Math.abs(this.lockOnSpeed.getValue()), 10.0F);
        Myau.rotationManager.setRotation(
                mc.thePlayer.rotationYaw + (rotation[0] - mc.thePlayer.rotationYaw) * 0.1F * speed,
                mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * speed, 0, false);
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
