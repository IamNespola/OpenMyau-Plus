package myau.module.modules;

import java.awt.Color;
import myau.event.EventTarget;
import myau.events.PacketEvent;
import myau.events.Render3DEvent;
import myau.events.UpdateEvent;
import myau.event.types.EventType;
import myau.module.Module;
import myau.property.properties.ColorProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.biome.BiomeGenBase;

public final class Ambience extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty time = new IntProperty("Time", 0, 0, 22999);
    public final IntProperty speed = new IntProperty("Time Speed", 0, 0, 20);

    public final ModeProperty weather =
            new ModeProperty(
                    "Weather",
                    0,
                    new String[] {
                            "Unchanged", "Clear", "Rain", "Heavy Snow", "Light Snow", "Nether Particles"
                    });

    public final ColorProperty snowColor =
            new ColorProperty(
                    "Snow Color",
                    Color.WHITE.getRGB(),
                    () ->
                            !weather.getModeString().equals("Heavy Snow")
                                    && !weather.getModeString().equals("Light Snow"));

    public Ambience() {
        super("Ambience", false);
    }

    @Override
    public void onDisabled() {
        if (mc.theWorld != null) {
            mc.theWorld.setRainStrength(0);
            mc.theWorld.getWorldInfo().setCleanWeatherTime(Integer.MAX_VALUE);
            mc.theWorld.getWorldInfo().setRainTime(0);
            mc.theWorld.getWorldInfo().setThunderTime(0);
            mc.theWorld.getWorldInfo().setRaining(false);
            mc.theWorld.getWorldInfo().setThundering(false);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld != null) {
            mc.theWorld.setWorldTime(
                    (long) (time.getValue() + (System.currentTimeMillis() * speed.getValue())));
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.thePlayer != null && mc.thePlayer.ticksExisted % 20 == 0) {
                switch (this.weather.getModeString()) {
                    case "Clear":
                    {
                        mc.theWorld.setRainStrength(0);
                        mc.theWorld.getWorldInfo().setCleanWeatherTime(Integer.MAX_VALUE);
                        mc.theWorld.getWorldInfo().setRainTime(0);
                        mc.theWorld.getWorldInfo().setThunderTime(0);
                        mc.theWorld.getWorldInfo().setRaining(false);
                        mc.theWorld.getWorldInfo().setThundering(false);
                        break;
                    }
                    case "Nether Particles":
                    case "Light Snow":
                    case "Heavy Snow":
                    case "Rain":
                    {
                        mc.theWorld.setRainStrength(1);
                        mc.theWorld.getWorldInfo().setCleanWeatherTime(0);
                        mc.theWorld.getWorldInfo().setRainTime(Integer.MAX_VALUE);
                        mc.theWorld.getWorldInfo().setThunderTime(Integer.MAX_VALUE);
                        mc.theWorld.getWorldInfo().setRaining(true);
                        mc.theWorld.getWorldInfo().setThundering(false);
                        break;
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof S03PacketTimeUpdate) {
            event.setCancelled(true);
        } else if (event.getPacket() instanceof S2BPacketChangeGameState
                && !this.weather.getModeString().equals("Unchanged")) {
            S2BPacketChangeGameState s2b = (S2BPacketChangeGameState) event.getPacket();

            if (s2b.getGameState() == 1 || s2b.getGameState() == 2) {
                event.setCancelled(true);
            }
        }
    }

    public float getFloatTemperature(BlockPos blockPos, BiomeGenBase biomeGenBase) {
        if (this.isEnabled()) {
            switch (this.weather.getModeString()) {
                case "Nether Particles":
                case "Light Snow":
                case "Heavy Snow":
                    return 0.1F;
                case "Rain":
                    return 0.2F;
            }
        }
        return biomeGenBase.getFloatTemperature(blockPos);
    }

    public boolean skipRainParticles() {
        final String name = this.weather.getModeString();
        return this.isEnabled()
                && (name.equals("Light Snow")
                || name.equals("Heavy Snow")
                || name.equals("Nether Particles"));
    }
}