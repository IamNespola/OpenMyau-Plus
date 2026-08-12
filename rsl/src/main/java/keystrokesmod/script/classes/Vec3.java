package keystrokesmod.script.classes;

import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class Vec3 {
    public double x, y, z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(net.minecraft.util.Vec3 minecraftVec3) {
        this.x = minecraftVec3.xCoord;
        this.y = minecraftVec3.yCoord;
        this.z = minecraftVec3.zCoord;
    }

    public Vec3(BlockPos blockPos) {
        this.x = blockPos.getX();
        this.y = blockPos.getY();
        this.z = blockPos.getZ();
    }

    public boolean equals(Vec3 vector2) {
        if (this == vector2) {
            return true;
        }
        else if (this.x == vector2.x && this.y == vector2.y && this.z == vector2.z) {
            return true;
        }
        return false;
    }

    public Vec3 offset(Vec3 position) {
        return new Vec3(this.x + position.x, this.y + position.y, this.z + position.z);
    }

    public Vec3 offset(double x, double y, double z) {
        return new Vec3(this.x + x, this.y + y, this.z + z);
    }

    public Vec3 ceil() {
        return new Vec3(Math.ceil(this.x), Math.ceil(this.y), Math.ceil(this.z));
    }

    public Vec3 floor() {
        return new Vec3(Math.floor(this.x), Math.floor(this.y), Math.floor(this.z));
    }

    public Vec3 inverse() {
        return new Vec3(-this.x, -this.y, -this.z);
    }

    public Vec3 translate(Vec3 position) {
        return this.offset(position.x, position.y, position.z);
    }

    public Vec3 translate(double x, double y, double z) {
        return this.offset(x, y, z);
    }

    public static Vec3 convert(BlockPos blockPos) {
        if (blockPos == null) {
            return null;
        }
        return new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public double distanceTo(Vec3 vec3) {
        double deltaX = this.x - vec3.x;
        double deltaY = this.y - vec3.y;
        double deltaZ = this.z - vec3.z;
        return MathHelper.sqrt_double(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public double distanceToSq(Vec3 vec3) {
        double deltaX = this.x - vec3.x;
        double deltaY = this.y - vec3.y;
        double deltaZ = this.z - vec3.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public static BlockPos getBlockPos(Vec3 blockPos) {
        return new BlockPos(blockPos.x, blockPos.y, blockPos.z);
    }

    public static net.minecraft.util.Vec3 getVec3(Vec3 vec3) {
        return new net.minecraft.util.Vec3(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public String toString() {
        return "Vec3(" + this.x + "," + this.y + "," + this.z + ")";
    }
}
