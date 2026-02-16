package myau.anticheat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class AutoBlock {

    private final Map<String, Long> guardingStartTicks = new HashMap<>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && mc.thePlayer != null && mc.theWorld != null) {
            World world = mc.theWorld;
            long currentTick = world.getTotalWorldTime();
            for (EntityPlayer player : world.playerEntities) {
                if (player == mc.thePlayer) continue;
                ItemStack heldItem = player.getHeldItem();
                boolean isCurrentlyGuarding = false;
                if (heldItem != null && heldItem.getItem() instanceof ItemSword && player.isBlocking()) isCurrentlyGuarding = true;
                boolean isAttacking = (player.swingProgress > 0 && player.prevSwingProgress == 0);
                if (isCurrentlyGuarding) {
                    guardingStartTicks.putIfAbsent(player.getName(), currentTick);
                    long startTick = guardingStartTicks.get(player.getName());
                    long ticksGuarded = currentTick - startTick;
                    if (isAttacking && ticksGuarded > 4) {
                        flag.receiveSignal(player.getName(), "AutoBlock");
                    }
                } else {
                    guardingStartTicks.remove(player.getName());
                }
            }
        }
    }
}