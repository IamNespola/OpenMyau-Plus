package keystrokesmod.script.classes;

import net.minecraft.util.MathHelper;

public class Vec3i {

    public int x, y, z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3i(Vec3 vec3) {
        this.x = MathHelper.floor_double(vec3.x);
        this.y = MathHelper.floor_double(vec3.y);
        this.z = MathHelper.floor_double(vec3.z);
    }

}