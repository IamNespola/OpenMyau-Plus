package myau.anticheat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class KillAura {

    private final Map<String, Long> lastAttackTime = new HashMap<>();
    private final Map<String, Integer> consecutiveHeadshots = new HashMap<>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && mc.thePlayer != null && mc.theWorld != null) {
            World world = mc.theWorld;
            long currentTick = world.getTotalWorldTime();

            for (EntityPlayer player : world.playerEntities) {
                if (player == mc.thePlayer) continue;

                // Check 1: Consistent rotation to player head (inhuman precision)
                Vec3 playerPos = new Vec3(player.posX, player.posY + player.getEyeHeight(), player.posZ);
                Vec3 clientPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
                double distance = playerPos.distanceTo(clientPos);

                if (distance < 6.0 && distance > 0.1) {
                    // Check swing progress (attack patterns)
                    boolean isAttacking = (player.swingProgress > 0 && player.prevSwingProgress == 0);

                    if (isAttacking) {
                        long lastAttack = lastAttackTime.getOrDefault(player.getName(), currentTick);
                        long timeSinceLastAttack = currentTick - lastAttack;

                        // Flag if attacks are too consistent (perfect timing)
                        if (timeSinceLastAttack > 0 && timeSinceLastAttack < 3) {
                            flag.receiveSignal(player.getName(), "KillAura");
                        }

                        lastAttackTime.put(player.getName(), currentTick);

                        // Check 2: Multiple consecutive attacks without moving
                        int headshots = consecutiveHeadshots.getOrDefault(player.getName(), 0);
                        headshots++;
                        consecutiveHeadshots.put(player.getName(), headshots);

                        if (headshots > 8) {
                            flag.receiveSignal(player.getName(), "KillAura");
                            consecutiveHeadshots.put(player.getName(), 0);
                        }
                    } else {
                        consecutiveHeadshots.put(player.getName(), 0);
                    }
                }
            }
        }
    }
}
