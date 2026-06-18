package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.RandomUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TickBase extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean duringTickModification = false;

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"PAST", "FUTURE"});
    public final BooleanProperty onlyOnKillAura = new BooleanProperty("only-on-killaura", true);
    public final IntProperty change = new IntProperty("changes", 100, 0, 100);
    public final IntProperty balanceMaxValue = new IntProperty("balance-max-value", 100, 1, 1000);
    public final FloatProperty balanceRecoveryIncrement = new FloatProperty("balance-recovery-increment", 0.1F, 0.01F, 10.0F);
    public final IntProperty maxTicksAtATime = new IntProperty("max-ticks-at-a-time", 20, 1, 100);
    public final FloatProperty minRangeToAttack = new FloatProperty("min-range-to-attack", 3.0F, 0.0F, 10.0F);
    public final FloatProperty maxRangeToAttack = new FloatProperty("max-range-to-attack", 5.0F, 0.0F, 10.0F);
    public final BooleanProperty forceGround = new BooleanProperty("force-ground", false);
    public final IntProperty pauseAfterTick = new IntProperty("pause-after-tick", 0, 0, 100);
    public final BooleanProperty pauseOnFlag = new BooleanProperty("pause-on-flag", true);
    public final BooleanProperty line = new BooleanProperty("line", true);
    public final ColorProperty lineColor = new ColorProperty("line-color", 0xFF00FF00);

    private int ticksToSkip;
    private float tickBalance;
    private boolean reachedTheLimit;
    private boolean modificationFlag;
    private final List<TickData> tickBuffer = new ArrayList<>();

    public TickBase() {
        super("TickBase", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getValue() == 0 ? "Past" : "Future"};
    }

    @Override
    public void onEnabled() {
        ticksToSkip = 0;
        tickBalance = 0.0F;
        reachedTheLimit = false;
        modificationFlag = false;
        duringTickModification = false;
        tickBuffer.clear();
    }

    @Override
    public void onDisabled() {
        duringTickModification = false;
        ticksToSkip = 0;
        tickBuffer.clear();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.ridingEntity != null || isBlinkEnabled()) return;

        if (event.getType() == EventType.PRE && ticksToSkip-- > 0) {
            event.setCancelled(true);
            return;
        }

        if (event.getType() != EventType.POST) return;

        if (modificationFlag) {
            modificationFlag = false;
            duringTickModification = false;
        }

        runTickBaseIfPossible();
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.ridingEntity != null || isBlinkEnabled()) return;
        updateSimulationBuffer();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || !pauseOnFlag.getValue()) return;
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            tickBalance = 0.0F;
            ticksToSkip = 0;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || !line.getValue() || tickBuffer.isEmpty()) return;
        Color color = new Color(lineColor.getValue(), true);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        mc.entityRenderer.disableLightmap();
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
        double renderX = mc.getRenderManager().viewerPosX;
        double renderY = mc.getRenderManager().viewerPosY;
        double renderZ = mc.getRenderManager().viewerPosZ;
        synchronized (tickBuffer) {
            for (TickData tick : tickBuffer) {
                GL11.glVertex3d(tick.position.xCoord - renderX, tick.position.yCoord - renderY, tick.position.zCoord - renderZ);
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    private void updateSimulationBuffer() {
        synchronized (tickBuffer) {
            tickBuffer.clear();
            if (tickBalance <= 0.0F) reachedTheLimit = true;
            if (tickBalance > balanceMaxValue.getValue() / 2.0F) reachedTheLimit = false;
            if (tickBalance <= balanceMaxValue.getValue()) tickBalance += balanceRecoveryIncrement.getValue();
            if (reachedTheLimit) return;

            int ticks = Math.min((int) tickBalance, maxTicksAtATime.getValue() * (mode.getValue() == 0 ? 2 : 1));
            double x = mc.thePlayer.posX;
            double y = mc.thePlayer.posY;
            double z = mc.thePlayer.posZ;
            double motionX = mc.thePlayer.motionX;
            double motionY = mc.thePlayer.motionY;
            double motionZ = mc.thePlayer.motionZ;
            boolean onGround = mc.thePlayer.onGround;
            float fallDistance = mc.thePlayer.fallDistance;

            for (int i = 0; i < ticks; i++) {
                float forward = mc.thePlayer.movementInput.moveForward;
                float strafe = mc.thePlayer.movementInput.moveStrafe;
                boolean jump = mc.gameSettings.keyBindJump.isKeyDown();

                if (jump && onGround) {
                    motionY = 0.42D;
                    if (mc.thePlayer.isPotionActive(Potion.jump)) {
                        motionY += (mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;
                    }
                    onGround = false;
                }

                double[] inputMotion = getInputMotion(forward, strafe, mc.thePlayer.rotationYaw, onGround ? getGroundAcceleration() : 0.02F);
                motionX += inputMotion[0];
                motionZ += inputMotion[1];

                x += motionX;
                y += motionY;
                z += motionZ;

                motionY -= 0.08D;
                motionY *= 0.98D;

                if (y <= mc.thePlayer.posY && mc.thePlayer.onGround) {
                    y = mc.thePlayer.posY;
                    motionY = 0.0D;
                    onGround = true;
                }

                float friction = onGround ? 0.91F * mc.theWorld.getBlockState(mc.thePlayer.getPosition().down()).getBlock().slipperiness : 0.91F;
                motionX *= friction;
                motionZ *= friction;
                fallDistance = onGround ? 0.0F : fallDistance + (float) Math.max(0.0D, -motionY);
                tickBuffer.add(new TickData(new Vec3(x, y, z), fallDistance, onGround, mc.thePlayer.isCollidedHorizontally));
            }
        }
    }

    private float getGroundAcceleration() {
        float slipperiness = mc.theWorld.getBlockState(mc.thePlayer.getPosition().down()).getBlock().slipperiness * 0.91F;
        return mc.thePlayer.getAIMoveSpeed() * (0.16277136F / (slipperiness * slipperiness * slipperiness));
    }

    private double[] getInputMotion(float forward, float strafe, float yaw, float speed) {
        float input = strafe * strafe + forward * forward;
        if (input < 1.0E-4F) {
            return new double[]{0.0D, 0.0D};
        }
        input = MathHelper.sqrt_float(input);
        if (input < 1.0F) input = 1.0F;
        input = speed / input;
        strafe *= input;
        forward *= input;
        float sin = MathHelper.sin(yaw * (float) Math.PI / 180.0F);
        float cos = MathHelper.cos(yaw * (float) Math.PI / 180.0F);
        return new double[]{strafe * cos - forward * sin, forward * cos + strafe * sin};
    }

    private void runTickBaseIfPossible() {
        if (tickBuffer.isEmpty()) return;
        EntityLivingBase enemy = getNearestEntityInRange();
        if (enemy == null) return;
        double currentDistance = mc.thePlayer.getDistanceToEntity(enemy);
        int bestTick = -1;
        int criticalTick = -1;
        float minRange = Math.min(minRangeToAttack.getValue(), maxRangeToAttack.getValue());
        float maxRange = Math.max(minRangeToAttack.getValue(), maxRangeToAttack.getValue());

        synchronized (tickBuffer) {
            for (int i = 0; i < tickBuffer.size(); i++) {
                TickData tick = tickBuffer.get(i);
                double distance = tick.position.distanceTo(enemy.getPositionVector());
                if (distance < currentDistance && distance >= minRange && distance <= maxRange
                        && !tick.isCollidedHorizontally && (!forceGround.getValue() || tick.onGround)) {
                    if (bestTick == -1) bestTick = i;
                    if (criticalTick == -1 && tick.fallDistance > 0.0F) criticalTick = i;
                }
            }
        }

        int selectedTick = criticalTick != -1 ? criticalTick : bestTick;
        if (selectedTick == 0) return;
        if (RandomUtil.nextInt(0, 100) > change.getValue() || (onlyOnKillAura.getValue() && !isKillAuraActive())) {
            ticksToSkip = 0;
            return;
        }

        int skipTicks = Math.min(selectedTick + pauseAfterTick.getValue(), maxTicksAtATime.getValue() + pauseAfterTick.getValue());
        duringTickModification = true;
        if (mode.getValue() == 0) {
            ticksToSkip = skipTicks;
            runExtraTicks(skipTicks);
        } else {
            runExtraTicks(skipTicks);
            ticksToSkip = skipTicks;
        }
        modificationFlag = true;
    }

    private void runExtraTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            mc.thePlayer.onUpdate();
            tickBalance -= 1.0F;
        }
    }

    private EntityLivingBase getNearestEntityInRange() {
        EntityLivingBase nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Object object : mc.theWorld.loadedEntityList) {
            if (!(object instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) object;
            if (!isSelected(entity)) continue;
            double distance = mc.thePlayer.getDistanceToEntity(entity);
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean isSelected(EntityLivingBase entity) {
        if (entity == null || entity == mc.thePlayer || entity.isDead || !entity.isEntityAlive()) return false;
        if (entity instanceof EntityPlayer && TeamUtil.isSameTeam((EntityPlayer) entity)) return false;
        return mc.thePlayer.getDistanceToEntity(entity) <= Math.max(minRangeToAttack.getValue(), maxRangeToAttack.getValue()) + 4.0F;
    }

    private boolean isKillAuraActive() {
        Module module = Myau.moduleManager.modules.get(KillAura.class);
        return module != null && module.isEnabled();
    }

    private boolean isBlinkEnabled() {
        Module module = Myau.moduleManager.modules.get(Blink.class);
        return module != null && module.isEnabled();
    }

    private static class TickData {
        private final Vec3 position;
        private final float fallDistance;
        private final boolean onGround;
        private final boolean isCollidedHorizontally;

        TickData(Vec3 position, float fallDistance, boolean onGround, boolean isCollidedHorizontally) {
            this.position = position;
            this.fallDistance = fallDistance;
            this.onGround = onGround;
            this.isCollidedHorizontally = isCollidedHorizontally;
        }
    }
}