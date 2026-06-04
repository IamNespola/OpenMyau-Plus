package myau.module.modules;

import myau.event.EventTarget;
import myau.events.AttackEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;
import net.minecraft.util.EnumParticleTypes;

public class AttackEffects extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty particle = new ModeProperty("particle", 1, new String[]{
            "None", "Blood", "Lighting", "Fire", "Heart", "Water", "Smoke", "Magic", "Crits"
    });
    public final IntProperty amount = new IntProperty("particle-amount", 5, 1, 20, () -> particle.getValue() != 0);
    public final ModeProperty sound = new ModeProperty("sound", 0, new String[]{
            "None", "Hit", "Orb", "Pop", "Splash", "Lightning"
    });
    public final FloatProperty volume = new FloatProperty("volume", 1.0F, 0.1F, 5.0F, () -> sound.getValue() != 0);
    public final FloatProperty pitch = new FloatProperty("pitch", 1.0F, 0.1F, 5.0F, () -> sound.getValue() != 0);

    public AttackEffects() {
        super("AttackEffects", false);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;

        EntityLivingBase target = (EntityLivingBase) event.getTarget();
        for (int i = 0; i < amount.getValue(); i++) {
            doEffect(target);
        }
        doSound();
    }

    private void doSound() {
        if (mc.thePlayer == null) return;
        switch (sound.getValue()) {
            case 1:
                mc.thePlayer.playSound("random.bowhit", volume.getValue(), pitch.getValue());
                break;
            case 2:
                mc.thePlayer.playSound("random.orb", volume.getValue(), pitch.getValue());
                break;
            case 3:
                mc.thePlayer.playSound("random.pop", volume.getValue(), pitch.getValue());
                break;
            case 4:
                mc.thePlayer.playSound("random.splash", volume.getValue(), pitch.getValue());
                break;
            case 5:
                mc.thePlayer.playSound("ambient.weather.thunder", volume.getValue(), pitch.getValue());
                break;
        }
    }

    private void doEffect(EntityLivingBase target) {
        switch (particle.getValue()) {
            case 1:
                spawnBloodParticle(target);
                break;
            case 2:
                spawnLightning(target);
                break;
            case 3:
                spawnEffectParticle(EnumParticleTypes.LAVA, target);
                break;
            case 4:
                spawnEffectParticle(EnumParticleTypes.HEART, target);
                break;
            case 5:
                spawnEffectParticle(EnumParticleTypes.WATER_DROP, target);
                break;
            case 6:
                spawnEffectParticle(EnumParticleTypes.SMOKE_NORMAL, target);
                break;
            case 7:
                spawnEffectParticle(EnumParticleTypes.CRIT_MAGIC, target);
                break;
            case 8:
                spawnEffectParticle(EnumParticleTypes.CRIT, target);
                break;
        }
    }

    private void spawnBloodParticle(EntityLivingBase target) {
        mc.theWorld.spawnParticle(
                EnumParticleTypes.BLOCK_CRACK,
                target.posX,
                target.posY + target.height - 0.75D,
                target.posZ,
                0.0D,
                0.0D,
                0.0D,
                Block.getStateId(Blocks.redstone_block.getDefaultState())
        );
    }

    private void spawnEffectParticle(EnumParticleTypes particleType, EntityLivingBase target) {
        mc.effectRenderer.spawnEffectParticle(
                particleType.getParticleID(),
                target.posX,
                target.posY,
                target.posZ,
                target.posX,
                target.posY,
                target.posZ
        );
    }

    private void spawnLightning(EntityLivingBase target) {
        mc.getNetHandler().handleSpawnGlobalEntity(
                new S2CPacketSpawnGlobalEntity(new EntityLightningBolt(mc.theWorld, target.posX, target.posY, target.posZ))
        );
    }
}
