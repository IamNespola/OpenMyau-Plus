package keystrokesmod.script.classes;

import keystrokesmod.event.PrePlayerInputEvent;

public class MovementInput {
    public float forward;
    public float strafe;
    public boolean jump;
    public boolean sneak;

    public MovementInput(PrePlayerInputEvent event, byte f1) {
        this.forward = event.getForward();
        this.strafe = event.getStrafe();
        this.jump = event.isJump();
        this.sneak = event.isSneak();
    }

    public MovementInput(float forward, float strafe, boolean jump, boolean sneak) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
    }

    public MovementInput(Object[] state) {
        this.forward = (float) state[0];
        this.strafe = (float) state[1];
        this.jump = (boolean) state[2];
        this.sneak = (boolean) state[3];
    }

    public Object[] asArray() {
        return new Object[] { this.forward, this.strafe, this.jump, this.sneak };
    }

    public boolean equals(MovementInput input) {
        if (input == null) {
            return false;
        }
        return this.forward == input.forward && this.strafe == input.strafe && this.jump == input.jump && this.sneak == input.sneak;
    }

}