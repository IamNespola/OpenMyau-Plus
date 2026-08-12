package keystrokesmod.script.classes;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;

public class Simulation {
    private final SimulatedPlayer raw;

    private Simulation(SimulatedPlayer raw) {
        this.raw = raw;
    }

    public static Simulation create() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.thePlayer.movementInput == null) {
            throw new IllegalStateException("Simulation requires an initialized client player");
        }
        MovementInput input = mc.thePlayer.movementInput;
        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(input);
        return new Simulation(sim);
    }

    public void setForward(float forward) {
        raw.movementInput.moveForward = forward;
    }

    public void setStrafe(float strafe) {
        raw.movementInput.moveStrafe = strafe;
    }

    public void setJump(boolean jump) {
        raw.movementInput.jump = jump;
    }

    public void setSneak(boolean sneak) {
        raw.movementInput.sneak = sneak;
    }

    public void setYaw(float yaw) {
        raw.rotationYaw = yaw;
    }

    public void setPitch(float pitch) {
        raw.rotationPitch = pitch;
    }

    public void setSprinting(boolean sprinting) {
        raw.setSprintRequested(sprinting);
    }

    public void resetState(double x, double y, double z, boolean onGround) {
        raw.resetSimulationState(x, y, z, onGround);
    }

    public void tick() {
        raw.tick();
    }

    public Vec3 getPosition() {
        return raw.getPos();
    }

    public Vec3 getMotion() {
        return new Vec3(raw.motionX, raw.motionY, raw.motionZ);
    }

    public boolean onGround() {
        return raw.onGround;
    }
}
