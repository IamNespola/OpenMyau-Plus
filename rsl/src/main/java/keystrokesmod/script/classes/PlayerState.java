package keystrokesmod.script.classes;

import keystrokesmod.event.PreMotionEvent;

public class PlayerState {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public boolean onGround;
    public boolean isSprinting;
    public boolean isSneaking;

    public PlayerState(PreMotionEvent e, byte f1) {
        this.x = e.getPosX();
        this.y = e.getPosY();
        this.z = e.getPosZ();
        this.yaw = e.getYaw();
        this.pitch = e.getPitch();
        this.onGround = e.isOnGround();
        this.isSprinting = e.isSprinting();
        this.isSneaking = e.isSneaking();
    }

    public PlayerState(Object[] state) {
        this.x = (double) state[0];
        this.y = (double) state[1];
        this.z = (double) state[2];
        this.yaw = (float) state[3];
        this.pitch = (float) state[4];
        this.onGround = (boolean) state[5];
        this.isSprinting = (boolean) state[6];
        this.isSneaking = (boolean) state[7];
    }

    public Object[] asArray() {
        return new Object[] { this.x, this.y, this.z, this.yaw, this.pitch, this.onGround, this.isSprinting, this.isSneaking };
    }

    public boolean equals(PlayerState playerState) {
        if (playerState == null) {
            return false;
        }
        return this.x == playerState.x && this.y == playerState.y && this.z == playerState.z && this.yaw == playerState.yaw && this.pitch == playerState.pitch && this.onGround == playerState.onGround && this.isSprinting == playerState.isSprinting && this.isSneaking == playerState.isSneaking;
    }
}
