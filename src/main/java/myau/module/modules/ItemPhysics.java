package myau.module.modules;

import myau.module.Module;
import myau.property.properties.FloatProperty;

public class ItemPhysics extends Module {
    public static ItemPhysics instance;

    public final FloatProperty rotationSpeed = new FloatProperty("rotation-speed", 1.0F, 0.0F, 5.0F);

    public ItemPhysics() {
        super("ItemPhysics", false);
    }

    @Override
    public void onEnabled() {
        instance = this;
    }

    @Override
    public void onDisabled() {
        instance = null;
    }

    public float getRotationSpeed() {
        return this.rotationSpeed.getValue();
    }
}