package myau.module.modules;

import myau.module.Module;

import myau.event.EventTarget;
import myau.events.AttackEvent;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;

import java.awt.*;
import java.util.Random;

public class HitParticleEffects extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Vanilla", "Critical", "Redstone", "Custom"});
    public final IntProperty amount = new IntProperty("amount", 5, 1, 20);
    public final ColorProperty customColor = new ColorProperty("custom-color", new Color(255, 0, 0).getRGB(), () -> this.mode.getValue() == 2 || this.mode.getValue() == 3); // Only show if mode is Redstone or Custom
    public final BooleanProperty onlyCrits = new BooleanProperty("only-crits", false);

    public HitParticleEffects() {
        super("HitParticleEffects", false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) event.getTarget();

        if (this.onlyCrits.getValue()) {
            // A more robust critical check might involve looking at the Criticals module state or damage type.
            // For simplicity, this is a basic vanilla-like critical hit indicator.
            boolean isCritical = mc.thePlayer.fallDistance > 0.0F && !mc.thePlayer.onGround && !mc.thePlayer.isInWater() && !mc.thePlayer.isRiding();
            if (!isCritical) return;
        }

        double x = target.posX;
        double y = target.posY + target.getEyeHeight() / 2.0;
        double z = target.posZ;

        for (int i = 0; i < this.amount.getValue(); i++) {
            double offsetX = this.random.nextGaussian() * 0.2;
            double offsetY = this.random.nextGaussian() * 0.2;
            double offsetZ = this.random.nextGaussian() * 0.2;

            switch (this.mode.getValue()) {
                case 0: // Vanilla (CRIT)
                    mc.theWorld.spawnParticle(EnumParticleTypes.CRIT, x + offsetX, y + offsetY, z + offsetZ, 0.0, 0.0, 0.0);
                    break;
                case 1: // Critical (CRIT_MAGIC - for more visual distinction)
                    mc.theWorld.spawnParticle(EnumParticleTypes.CRIT_MAGIC, x + offsetX, y + offsetY, z + offsetZ, 0.0, 0.0, 0.0);
                    break;
                case 2: // Redstone (colored by customColor)
                    Color redstoneColor = new Color(this.customColor.getValue());
                    double redstoneMotionX = (double)redstoneColor.getRed()/255.0;
                    double redstoneMotionY = (double)redstoneColor.getGreen()/255.0;
                    double redstoneMotionZ = (double)redstoneColor.getBlue()/255.0;
                    // For REDSTONE particles, motionX/Y/Z are interpreted as RGB color if very small
                    if (redstoneMotionX == 0.0 && redstoneMotionY == 0.0 && redstoneMotionZ == 0.0) { // Avoid black particles
                        redstoneMotionX = 1.0; // Default to red if color is black
                    }
                    mc.theWorld.spawnParticle(EnumParticleTypes.REDSTONE, x + offsetX, y + offsetY, z + offsetZ, redstoneMotionX, redstoneMotionY, redstoneMotionZ);
                    break;
                case 3: // Custom (Using Redstone for custom coloring, FIREWORKS_SPARK is harder to color directly)
                    Color customParticleColor = new Color(this.customColor.getValue());
                    double customMotionX = (double)customParticleColor.getRed()/255.0;
                    double customMotionY = (double)customParticleColor.getGreen()/255.0;
                    double customMotionZ = (double)customParticleColor.getBlue()/255.0;
                    if (customMotionX == 0.0 && customMotionY == 0.0 && customMotionZ == 0.0) { // Avoid black particles
                        customMotionX = 1.0; // Default to red if color is black
                    }
                    mc.theWorld.spawnParticle(EnumParticleTypes.REDSTONE, x + offsetX, y + offsetY, z + offsetZ, customMotionX, customMotionY, customMotionZ);
                    break;
            }
        }
    }
}

