package myau.module.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.events.UpdateEvent;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ItemListProperty;
import myau.property.properties.ModeProperty;
import myau.util.CombatTargeting;
import myau.util.MoveUtil;
import myau.util.RotationUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

/**
 * @author strangerrrrs
 */
public class Displace extends Module {
    private static final int DISPLACE_WINDOW_TICKS = 10;
    private static final int VOID_SCAN_DIRECTIONS = 32;
    private static final int VOID_SCAN_RINGS = 12;
    private static final int VOID_SCAN_DEPTH = 10;
    private static final double VOID_SCAN_STEP = 0.5D;
    private static final double DYNAMIC_SCAN_STEP = 0.5D;
    private static final double DYNAMIC_SCAN_DISTANCE = 6.0D;
    private static final double DYNAMIC_SCAN_SIDE_STEP = 0.45D;
    private static final double DYNAMIC_WALL_CHECK_STEP = 0.25D;
    private static final double DYNAMIC_COLLISION_INSET = 0.03D;
    private static final long ARROW_FADE_MS = 250L;
    private static final double ARROW_FORWARD_GAP = 0.24D;
    private static final double ARROW_BODY_LENGTH = 0.74D;
    private static final double ARROW_BODY_HALF_HEIGHT = 0.08D;
    private static final double ARROW_HEAD_BACKSET = 0.18D;
    private static final double ARROW_HEAD_LENGTH = 0.52D;
    private static final double ARROW_HEAD_HALF_HEIGHT = 0.30D;
    private static final double[] VOID_SCAN_X = new double[VOID_SCAN_DIRECTIONS];
    private static final double[] VOID_SCAN_Z = new double[VOID_SCAN_DIRECTIONS];

    public final ModeProperty dynamicAngle =
            new ModeProperty("Dynamic-angle", 0, new String[] {"Static", "Dynamic"});
    public final FloatProperty yawOffset =
            new FloatProperty("Yaw-offset", 90F, 0F, 180F, () -> dynamicAngle.getValue() == 0);
    public final FloatProperty delay = new FloatProperty("Delay", 0F, 0F, 500F);
    public final ModeProperty direction =
            new ModeProperty(
                    "Direction", 0, new String[] {"Left", "Right"}, () -> dynamicAngle.getValue() == 0);
    public final BooleanProperty showDirection = new BooleanProperty("Show-direction", true);
    public final BooleanProperty findVoid =
            new BooleanProperty("Find-void", false, () -> dynamicAngle.getValue() == 0);
    public final BooleanProperty blink = new BooleanProperty("Blink", false);
    public final BooleanProperty ignoreTeammates = new BooleanProperty("Ignore-teammates", true);
    public final BooleanProperty hasKnockback = new BooleanProperty("Has-knockback", false);
    public final BooleanProperty itemWhitelistToggle = new BooleanProperty("Item-whitelist", false);
    public final ItemListProperty itemWhitelist = new ItemListProperty("Whitelisted-items", "");

    private boolean displaceThisTick = false;
    private boolean active = false;
    private boolean hasKB = false;
    private boolean compensateNextTick = false;
    private boolean displaceLeft = false;
    private boolean wasDisplacingLastTick = false;
    private boolean releaseBlinkNextGameTick = false;
    private boolean blinkingModule = false;
    private Float dynamicVoidYaw = null;
    private Float renderDisplaceYaw = null;
    private EntityPlayer renderTarget = null;
    private Float fadingDisplaceYaw = null;
    private EntityPlayer fadingTarget = null;
    private long arrowFadeStartMs = 0L;
    private Float lastRenderedDisplaceYaw = null;
    private EntityPlayer lastRenderedTarget = null;
    private long lastRenderedArrowMs = 0L;
    private int tickCounter;
    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    static {
        for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
            double angle = Math.PI * 2.0D * (double) i / (double) VOID_SCAN_DIRECTIONS;
            VOID_SCAN_X[i] = Math.cos(angle);
            VOID_SCAN_Z[i] = Math.sin(angle);
        }
    }

    public Displace() {
        super("Displace", false);
    }

    @Override
    public String[] getSuffix() {
        int ms = Math.round(delay.getValue());
        return new String[] {ms + "ms"};
    }

    @Override
    public void onEnabled() {
        displaceThisTick = false;
        active = false;
        hasKB = false;
        compensateNextTick = false;
        wasDisplacingLastTick = false;
        releaseBlinkNextGameTick = false;
        dynamicVoidYaw = null;
        renderDisplaceYaw = null;
        renderTarget = null;
        clearArrowState();
        tickCounter = 0;
        targetWindowStartTicks.clear();
        releaseBlink();
    }

    @Override
    public void onDisabled() {
        active = false;
        compensateNextTick = false;
        wasDisplacingLastTick = false;
        releaseBlinkNextGameTick = false;
        dynamicVoidYaw = null;
        renderDisplaceYaw = null;
        renderTarget = null;
        clearArrowState();
        targetWindowStartTicks.clear();
        releaseBlink();
    }

    private static int msToTicks(double ms) {
        if (ms <= 0.0D) {
            return 0;
        }
        return (int) Math.ceil(ms / 50.0D);
    }

    private boolean anyMovementKey() {
        return mc.gameSettings.keyBindForward.isKeyDown()
                || mc.gameSettings.keyBindBack.isKeyDown()
                || mc.gameSettings.keyBindLeft.isKeyDown()
                || mc.gameSettings.keyBindRight.isKeyDown();
    }

    private boolean isDynamicAngle() {
        return dynamicAngle.getValue() == 1;
    }

    private Float findStaticVoidYaw(EntityPlayer target) {
        if (target == null || mc.thePlayer == null || mc.theWorld == null) {
            return null;
        }

        double bestX = 0.0D;
        double bestZ = 0.0D;
        double bestScore = Double.MAX_VALUE;

        for (int ring = 1; ring <= VOID_SCAN_RINGS; ring++) {
            double radius = (double) ring * VOID_SCAN_STEP;
            boolean foundInRing = false;

            for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
                double x = target.posX + VOID_SCAN_X[i] * radius;
                double z = target.posZ + VOID_SCAN_Z[i] * radius;
                if (!isVoidColumn(x, target.posY, z)) {
                    continue;
                }

                double playerDx = x - mc.thePlayer.posX;
                double playerDz = z - mc.thePlayer.posZ;
                double playerDistSq = playerDx * playerDx + playerDz * playerDz;
                double score = radius * radius * 1000.0D + playerDistSq;
                if (score < bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestZ = z;
                    foundInRing = true;
                }
            }

            if (foundInRing) {
                break;
            }
        }

        if (bestScore == Double.MAX_VALUE) {
            return null;
        }

        updateDisplaceSide(target, bestX, bestZ);

        double dx = bestX - target.posX;
        double dz = bestZ - target.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001D) {
            return null;
        }

        double aimRadius = Math.min(dist, Math.max(0.35D, (double) target.width * 0.5D + 0.15D));
        double aimX = target.posX + dx / dist * aimRadius;
        double aimZ = target.posZ + dz / dist * aimRadius;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        double adx = aimX - eyes.xCoord;
        double ady = (target.posY + (double) target.getEyeHeight() * 0.5D) - eyes.yCoord;
        double adz = aimZ - eyes.zCoord;
        return (float) Math.toDegrees(Math.atan2(adz, adx)) - 90.0F;
    }

    private Float findDynamicVoidYaw(EntityPlayer target) {
        if (target == null || mc.thePlayer == null || mc.theWorld == null) {
            return null;
        }

        double bestForwardX = 0.0D;
        double bestForwardZ = 0.0D;
        double bestScore = 0.0D;

        for (int i = 0; i < VOID_SCAN_DIRECTIONS; i++) {
            double forwardX = VOID_SCAN_X[i];
            double forwardZ = VOID_SCAN_Z[i];
            double score = scoreVoidPath(target, forwardX, forwardZ);

            if (score > bestScore) {
                bestScore = score;
                bestForwardX = forwardX;
                bestForwardZ = forwardZ;
            }
        }

        if (bestScore <= 0.0D) {
            return null;
        }

        updateDisplaceSide(target, target.posX + bestForwardX, target.posZ + bestForwardZ);
        return yawFromForward(bestForwardX, bestForwardZ);
    }

    private float yawFromForward(double forwardX, double forwardZ) {
        return (float) (Math.toDegrees(Math.atan2(forwardZ, forwardX)) - 90.0D);
    }

    private double scoreVoidPath(EntityPlayer target, double forwardX, double forwardZ) {
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double score = 0.0D;
        double checkedForward = 0.0D;
        int consecutiveCenterVoid = 0;
        AxisAlignedBB baseCollisionBox =
                target
                        .getEntityBoundingBox()
                        .contract(DYNAMIC_COLLISION_INSET, 0.0D, DYNAMIC_COLLISION_INSET);

        for (int step = 1; step <= (int) (DYNAMIC_SCAN_DISTANCE / DYNAMIC_SCAN_STEP); step++) {
            double forward = (double) step * DYNAMIC_SCAN_STEP;
            if (!isDynamicPathClear(
                    target, baseCollisionBox, forwardX, forwardZ, checkedForward, forward)) {
                break;
            }
            checkedForward = forward;

            boolean centerVoid = false;

            for (int side = -1; side <= 1; side++) {
                double sideOffset = (double) side * DYNAMIC_SCAN_SIDE_STEP;
                double x = target.posX + forwardX * forward + sideX * sideOffset;
                double z = target.posZ + forwardZ * forward + sideZ * sideOffset;
                if (isVoidColumn(x, target.posY, z)) {
                    double laneWeight = side == 0 ? 1.4D : 1.0D;
                    score += laneWeight * (DYNAMIC_SCAN_DISTANCE + DYNAMIC_SCAN_STEP - forward);
                    centerVoid |= side == 0;
                }
            }

            if (centerVoid) {
                consecutiveCenterVoid++;
                score += consecutiveCenterVoid * 2.0D;
            } else {
                consecutiveCenterVoid = 0;
            }
        }

        return score;
    }

    private boolean isDynamicPathClear(
            EntityPlayer target,
            AxisAlignedBB baseCollisionBox,
            double forwardX,
            double forwardZ,
            double fromForward,
            double toForward) {
        for (double forward = fromForward + DYNAMIC_WALL_CHECK_STEP;
             forward <= toForward + 1.0E-4D;
             forward += DYNAMIC_WALL_CHECK_STEP) {
            AxisAlignedBB checkBox =
                    baseCollisionBox.offset(forwardX * forward, 0.0D, forwardZ * forward);
            if (hasBlockCollision(target, checkBox)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasBlockCollision(EntityPlayer target, AxisAlignedBB box) {
        int minX = MathHelper.floor_double(box.minX);
        int maxX = MathHelper.floor_double(box.maxX + 1.0D);
        int minY = MathHelper.floor_double(box.minY);
        int maxY = MathHelper.floor_double(box.maxY + 1.0D);
        int minZ = MathHelper.floor_double(box.minZ);
        int maxZ = MathHelper.floor_double(box.maxZ + 1.0D);

        List<AxisAlignedBB> collisions = new ArrayList<AxisAlignedBB>();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int blockX = minX; blockX < maxX; blockX++) {
            for (int blockZ = minZ; blockZ < maxZ; blockZ++) {
                if (!mc.theWorld.isBlockLoaded(blockPos.set(blockX, 64, blockZ))) {
                    return true;
                }

                for (int blockY = minY; blockY < maxY; blockY++) {
                    if (blockY < 0 || blockY >= 256) {
                        return true;
                    }

                    blockPos.set(blockX, blockY, blockZ);
                    IBlockState state = mc.theWorld.getBlockState(blockPos);
                    state
                            .getBlock()
                            .addCollisionBoxesToList(mc.theWorld, blockPos, state, box, collisions, target);
                    if (!collisions.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isVoidColumn(double x, double y, double z) {
        int blockX = MathHelper.floor_double(x);
        int blockZ = MathHelper.floor_double(z);
        int startY = MathHelper.floor_double(y) - 1;
        int endY = Math.max(0, startY - VOID_SCAN_DEPTH);

        for (int blockY = startY; blockY >= endY; blockY--) {
            if (!mc.theWorld.isAirBlock(new BlockPos(blockX, blockY, blockZ))) {
                return false;
            }
        }
        return true;
    }

    private void updateDisplaceSide(EntityPlayer target, double voidX, double voidZ) {
        double targetDx = target.posX - mc.thePlayer.posX;
        double targetDz = target.posZ - mc.thePlayer.posZ;
        double voidDx = voidX - mc.thePlayer.posX;
        double voidDz = voidZ - mc.thePlayer.posZ;
        double cross = targetDx * voidDz - targetDz * voidDx;
        displaceLeft = cross < 0.0D;
    }

    private float getFixedDisplaceYaw() {
        float baseYaw = RotationUtil.customRots ? RotationUtil.serverYaw : mc.thePlayer.rotationYaw;
        float offset = yawOffset.getValue();
        return displaceLeft ? baseYaw - offset : baseYaw + offset;
    }

    private void clearActiveState() {
        startArrowFade();
        active = false;
        displaceThisTick = false;
        compensateNextTick = false;
        wasDisplacingLastTick = false;
        dynamicVoidYaw = null;
        renderDisplaceYaw = null;
        renderTarget = null;
    }

    private void clearFadingArrow() {
        fadingDisplaceYaw = null;
        fadingTarget = null;
        arrowFadeStartMs = 0L;
    }

    private void clearArrowState() {
        clearFadingArrow();
        lastRenderedDisplaceYaw = null;
        lastRenderedTarget = null;
        lastRenderedArrowMs = 0L;
    }

    private void startArrowFade() {
        long nowMs = System.currentTimeMillis();
        if (lastRenderedDisplaceYaw != null
                && lastRenderedTarget != null
                && !lastRenderedTarget.isDead
                && nowMs - lastRenderedArrowMs <= ARROW_FADE_MS) {
            fadingDisplaceYaw = lastRenderedDisplaceYaw;
            fadingTarget = lastRenderedTarget;
            arrowFadeStartMs = nowMs;
        }
        lastRenderedDisplaceYaw = null;
        lastRenderedTarget = null;
        lastRenderedArrowMs = 0L;
    }

    private void pruneTargetDelayStates() {
        if (mc.theWorld == null) {
            targetWindowStartTicks.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Integer>> iterator = targetWindowStartTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            Entity entity = mc.theWorld.getEntityByID(entry.getKey());
            if (!(entity instanceof EntityPlayer)
                    || entity.isDead
                    || ((EntityPlayer) entity).deathTime != 0) {
                iterator.remove();
            }
        }
    }

    private boolean shouldDisplaceInCurrentWindow(EntityPlayer target, int currentTick) {
        if (target == null) {
            return true;
        }

        int targetId = target.getEntityId();
        Integer windowStartTick = targetWindowStartTicks.get(targetId);
        if (windowStartTick == null || currentTick - windowStartTick >= DISPLACE_WINDOW_TICKS) {
            targetWindowStartTicks.put(targetId, currentTick);
            return true;
        }

        int delayTicks = msToTicks(delay.getValue());
        if (delayTicks <= 0) {
            return true;
        }

        int elapsed = currentTick - windowStartTick;
        return elapsed >= delayTicks;
    }

    private void releaseBlink() {
        if (!blinkingModule) return;
        Myau.blinkManager.setBlinkState(false, BlinkModules.DISPLACE);
        blinkingModule = false;
    }

    @EventTarget(Priority.HIGHEST)
    public void onGameTick(TickEvent e) {
        if (e.getType() != EventType.PRE) return;
        if (releaseBlinkNextGameTick) {
            releaseBlink();
            releaseBlinkNextGameTick = false;
        }
    }

    @EventTarget
    public void onRenderWorld(Render3DEvent e) {
        if (!this.isEnabled()) return;
        if (!showDirection.getValue()) {
            clearArrowState();
            return;
        }

        long nowMs = System.currentTimeMillis();
        boolean activeArrow =
                active && renderDisplaceYaw != null && renderTarget != null && !renderTarget.isDead;
        Float arrowYaw = renderDisplaceYaw;
        EntityPlayer arrowTarget = renderTarget;
        float alpha = 1.0F;

        if (activeArrow) {
            clearFadingArrow();
        } else {
            if (fadingDisplaceYaw == null || fadingTarget == null || fadingTarget.isDead) {
                clearFadingArrow();
                return;
            }

            long fadeElapsedMs = nowMs - arrowFadeStartMs;
            if (fadeElapsedMs >= ARROW_FADE_MS) {
                clearFadingArrow();
                return;
            }

            arrowYaw = fadingDisplaceYaw;
            arrowTarget = fadingTarget;
            alpha = 1.0F - (float) fadeElapsedMs / (float) ARROW_FADE_MS;
        }

        float partialTicks = e.getPartialTicks();
        double centerX =
                arrowTarget.lastTickPosX + (arrowTarget.posX - arrowTarget.lastTickPosX) * partialTicks;
        double centerY =
                arrowTarget.lastTickPosY
                        + (arrowTarget.posY - arrowTarget.lastTickPosY) * partialTicks
                        + (double) arrowTarget.height * 0.5D;
        double centerZ =
                arrowTarget.lastTickPosZ + (arrowTarget.posZ - arrowTarget.lastTickPosZ) * partialTicks;

        double yawRad = Math.toRadians(arrowYaw);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        double baseOffset = (double) arrowTarget.width * 0.5D + ARROW_FORWARD_GAP;
        double tailX = centerX + forwardX * baseOffset;
        double tailZ = centerZ + forwardZ * baseOffset;
        double bodyEndX = tailX + forwardX * ARROW_BODY_LENGTH;
        double bodyEndZ = tailZ + forwardZ * ARROW_BODY_LENGTH;
        double headBackX = tailX + forwardX * (ARROW_BODY_LENGTH - ARROW_HEAD_BACKSET);
        double headBackZ = tailZ + forwardZ * (ARROW_BODY_LENGTH - ARROW_HEAD_BACKSET);
        double tipX = bodyEndX + forwardX * ARROW_HEAD_LENGTH;
        double tipZ = bodyEndZ + forwardZ * ARROW_HEAD_LENGTH;

        double viewerX = mc.getRenderManager().viewerPosX;
        double viewerY = mc.getRenderManager().viewerPosY;
        double viewerZ = mc.getRenderManager().viewerPosZ;

        GL11.glPushMatrix();
        GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT
                        | GL11.GL_COLOR_BUFFER_BIT
                        | GL11.GL_LINE_BIT
                        | GL11.GL_DEPTH_BUFFER_BIT
                        | GL11.GL_CURRENT_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.82F * alpha);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex3d(tailX - viewerX, centerY - viewerY, tailZ - viewerZ);
        arrowVertex(bodyEndX, centerY, bodyEndZ, -ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        arrowVertex(bodyEndX, centerY, bodyEndZ, ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        arrowVertex(bodyEndX, centerY, bodyEndZ, -ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        arrowVertex(headBackX, centerY, headBackZ, -ARROW_HEAD_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
        arrowVertex(bodyEndX, centerY, bodyEndZ, -ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
        arrowVertex(bodyEndX, centerY, bodyEndZ, ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        arrowVertex(bodyEndX, centerY, bodyEndZ, ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
        arrowVertex(headBackX, centerY, headBackZ, ARROW_HEAD_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glEnd();

        GL11.glLineWidth(2.0F);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.95F * alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(tailX - viewerX, centerY - viewerY, tailZ - viewerZ);
        arrowVertex(bodyEndX, centerY, bodyEndZ, -ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        arrowVertex(headBackX, centerY, headBackZ, -ARROW_HEAD_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glVertex3d(tipX - viewerX, centerY - viewerY, tipZ - viewerZ);
        arrowVertex(headBackX, centerY, headBackZ, ARROW_HEAD_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        arrowVertex(bodyEndX, centerY, bodyEndZ, ARROW_BODY_HALF_HEIGHT, viewerX, viewerY, viewerZ);
        GL11.glEnd();

        GL11.glPopAttrib();
        GL11.glPopMatrix();

        if (activeArrow) {
            lastRenderedDisplaceYaw = arrowYaw;
            lastRenderedTarget = arrowTarget;
            lastRenderedArrowMs = nowMs;
        }
    }

    private void arrowVertex(
            double x,
            double y,
            double z,
            double verticalOffset,
            double viewerX,
            double viewerY,
            double viewerZ) {
        GL11.glVertex3d(x - viewerX, y + verticalOffset - viewerY, z - viewerZ);
    }

    @EventTarget(Priority.LOWEST)
    public void onPostInput(MoveInputEvent e) {
        if (!this.isEnabled()) return;
        if (!active) {
            compensateNextTick = false;
            return;
        }

        if (compensateNextTick && !displaceThisTick) {
            compensateNextTick = false;
            if (displaceLeft) {
                mc.thePlayer.movementInput.moveStrafe = -1;
            } else {
                mc.thePlayer.movementInput.moveStrafe = 1;
            }
            return;
        }

        if (!displaceThisTick || hasKB) return;
        if (!anyMovementKey()) return;

        mc.thePlayer.movementInput.moveForward = 1;
        compensateNextTick = true;
    }

    @EventTarget(Priority.HIGH)
    public void onSendPacket(PacketEvent e) {
        if (!this.isEnabled()) return;
        if (!blink.getValue() || !active || !displaceThisTick || releaseBlinkNextGameTick) {
            return;
        }
        if (!(e.getPacket() instanceof C03PacketPlayer)) {
            return;
        }
        if (blinkingModule) return;

        Myau.blinkManager.setBlinkState(true, BlinkModules.DISPLACE);
        blinkingModule = true;
        releaseBlinkNextGameTick = true;
    }



    @EventTarget(Priority.LOWEST)
    public void onUpdateLowest(UpdateEvent e) {
        if (!this.isEnabled()) return;
        if (e.getType() != EventType.PRE) return;

        tickCounter++;
        int currentTick = tickCounter;
        pruneTargetDelayStates();

        boolean passesItemCondition = true;
        if (hasKnockback.getValue() || itemWhitelistToggle.getValue()) {
            boolean kbPass = !hasKnockback.getValue() || EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
            boolean wlPass =
                    !itemWhitelistToggle.getValue() || itemWhitelist.matches(mc.thePlayer.getHeldItem());
            passesItemCondition = kbPass || wlPass;
        }
        if (!passesItemCondition) {
            clearActiveState();
            return;
        }

        EntityPlayer target = null;
        boolean attacking =
                mc.gameSettings.keyBindAttack.isKeyDown()
                        || (Myau.moduleManager.modules.get(KillAura.class) != null
                        && Myau.moduleManager.modules.get(KillAura.class).isEnabled()
                        && ((KillAura) Myau.moduleManager.modules.get(KillAura.class)).getTarget() != null);
        if (attacking) {
            target = CombatTargeting.findClosestTarget(9.0, ignoreTeammates.getValue());
        }

        boolean hasKBEnchant = EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
        active = target != null && (hasKBEnchant || anyMovementKey());
        if (!active) {
            clearActiveState();
            return;
        }

        dynamicVoidYaw =
                isDynamicAngle()
                        ? findDynamicVoidYaw(target)
                        : findVoid.getValue() ? findStaticVoidYaw(target) : null;
        if (dynamicVoidYaw == null && !isDynamicAngle()) {
            displaceLeft = direction.getValue() == 0;
        }
        renderDisplaceYaw =
                dynamicVoidYaw != null ? dynamicVoidYaw : isDynamicAngle() ? null : getFixedDisplaceYaw();
        renderTarget = renderDisplaceYaw != null ? target : null;
        if (renderDisplaceYaw == null) {
            clearActiveState();
            return;
        }

        hasKB = hasKBEnchant;
        displaceThisTick = !displaceThisTick;
        if (displaceThisTick && !shouldDisplaceInCurrentWindow(target, currentTick)) {
            startArrowFade();
            displaceThisTick = false;
            compensateNextTick = false;
            wasDisplacingLastTick = false;
            dynamicVoidYaw = null;
            renderDisplaceYaw = null;
            renderTarget = null;
            return;
        }

        if (!displaceThisTick && wasDisplacingLastTick) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            if (key != 0) {
                KeyBinding.onTick(key);
            }
        }

        wasDisplacingLastTick = displaceThisTick;

        if (!displaceThisTick || renderDisplaceYaw == null) return;

        e.setRotation(renderDisplaceYaw, mc.thePlayer.rotationPitch, 100);
        MoveUtil.fixMovement(renderDisplaceYaw);
    }
}