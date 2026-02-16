package myau.anticheat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class Scaffold {

    private final Map<String, Long> scaffoldStartTime = new HashMap<>();
    private final Map<String, Integer> blockPlaceCount = new HashMap<>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && mc.thePlayer != null && mc.theWorld != null) {
            World world = mc.theWorld;
            long currentTick = world.getTotalWorldTime();

            for (EntityPlayer player : world.playerEntities) {
                if (player == mc.thePlayer) continue;

                // Check 1: Falling while placing blocks (bridge/scaffold detection)
                if (player.motionY < 0 && !player.onGround) {

                    // Check if player is placing blocks while falling
                    BlockPos playerPos = new BlockPos(player.posX, player.posY - 1, player.posZ);
                    Block blockBelow = world.getBlockState(playerPos).getBlock();

                    // If block exists where player is (recently placed)
                    if (!(blockBelow instanceof BlockAir)) {
                        scaffoldStartTime.putIfAbsent(player.getName(), currentTick);
                        int placeCount = blockPlaceCount.getOrDefault(player.getName(), 0) + 1;
                        blockPlaceCount.put(player.getName(), placeCount);

                        // Flag if too many blocks placed while falling
                        if (placeCount > 15) {
                            flag.receiveSignal(player.getName(), "Scaffold");
                            blockPlaceCount.put(player.getName(), 0);
                        }
                    }
                } else {
                    blockPlaceCount.put(player.getName(), 0);
                }

                // Check 2: Perfect timing for block placement while moving upward
                if (player.motionY > 0.1 && !player.onGround) {
                    int placeCount = blockPlaceCount.getOrDefault(player.getName(), 0);

                    // Flag if blocks are being placed too frequently while jumping
                    if (placeCount > 5) {
                        long startTime = scaffoldStartTime.getOrDefault(player.getName(), currentTick);
                        long duration = currentTick - startTime;

                        // If they place many blocks in very short time
                        if (duration < 20 && duration > 0) {
                            flag.receiveSignal(player.getName(), "Scaffold");
                        }
                    }
                }

                // Check 3: Player always has a block below while falling (impossible without scaffold)
                if (player.motionY < -0.1) {
                    BlockPos belowPlayer = new BlockPos(player.posX, player.posY - 1.5, player.posZ);
                    Block blockBelowCheck = world.getBlockState(belowPlayer).getBlock();

                    if (!(blockBelowCheck instanceof BlockAir) && !player.onGround) {
                        scaffoldStartTime.putIfAbsent(player.getName(), currentTick);
                        long scaffoldDuration = currentTick - scaffoldStartTime.getOrDefault(player.getName(), currentTick);

                        if (scaffoldDuration > 40) {
                            flag.receiveSignal(player.getName(), "Scaffold");
                            scaffoldStartTime.put(player.getName(), currentTick);
                        }
                    } else {
                        scaffoldStartTime.remove(player.getName());
                    }
                }
            }
        }
    }
}
