package myau.management;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private float lastUpdate;
    private float yawDelta;
    private float pitchDelta;
    private int priority;
    private boolean rotated;

    public RotationManager() {
        this.lastUpdate = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.priority = Integer.MIN_VALUE;
        this.rotated = false;
    }

    private void applyRotation(float partialTicks) {
        if (mc.thePlayer != null && !Float.isNaN(this.yawDelta) && !Float.isNaN(this.pitchDelta) && !Float.isNaN(this.lastUpdate)) {
            float yaw = this.yawDelta * (partialTicks - this.lastUpdate);
            if (yaw != 0.0F) {
                mc.thePlayer.prevRotationYaw = mc.thePlayer.rotationYaw;
                mc.thePlayer.rotationYaw += yaw;
            }
            float pitch = this.pitchDelta * (partialTicks - this.lastUpdate);
            if (pitch != 0.0F) {
                mc.thePlayer.prevRotationPitch = mc.thePlayer.rotationPitch;
                mc.thePlayer.rotationPitch += pitch;
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90.0F, 90.0F);
            }
            this.lastUpdate = partialTicks;
        }
    }

    private void resetRotationState() {
        this.lastUpdate = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.priority = Integer.MIN_VALUE;
        this.rotated = false;
    }

    public void setRotation(float yaw, float pitch, int priority, boolean force) {
        if (this.priority <= priority) {
            this.priority = priority;

            float[] fixed = RotationUtil.gcd(new float[]{yaw, pitch}, new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch});
            float finalYaw = fixed[0];
            float finalPitch = fixed[1];

            this.yawDelta = MathHelper.wrapAngleTo180_float(finalYaw - mc.thePlayer.rotationYaw);
            this.pitchDelta = MathHelper.clamp_float(finalPitch - mc.thePlayer.rotationPitch, -90.0F, 90.0F);

            this.lastUpdate = 0.0F;
            this.rotated = force;
            this.applyRotation(0.0F);
        }
    }

    public boolean isRotated() {
        return this.rotated;
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        this.applyRotation(1.0F);
        this.resetRotationState();
    }

    @EventTarget(Priority.HIGHEST)
    public void onRender3D(Render3DEvent event) {
        this.applyRotation(event.getPartialTicks());
    }
}
