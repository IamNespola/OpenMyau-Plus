package myau.anticheat;

import myau.Myau;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class flag {
    private static final String CHEAT_AUTOBLOCK = "AutoBlock";
    private static final String CHEAT_NOSLOW = "Noslow";
    private static final String CHEAT_KILLAURA = "KillAura";
    private static final String CHEAT_SCAFFOLD = "Scaffold";
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean acflag = true;
    public static final List<String> whitelist = new ArrayList<>();
    private static final Map<String, int[]> flagMap = new HashMap<>();

    public static void receiveSignal(String playerName, String cheatName) {
        if (!acflag) return;
        if (whitelist.contains(playerName)) return;
        if (!(cheatName.equals(CHEAT_AUTOBLOCK) || cheatName.equals(CHEAT_NOSLOW) || cheatName.equals(CHEAT_KILLAURA) || cheatName.equals(CHEAT_SCAFFOLD))) return;

        int[] flagData = flagMap.getOrDefault(playerName, new int[]{0, (int) (Minecraft.getMinecraft().theWorld.getTotalWorldTime() / 20)});

        flagData[0] += 1;
        flagData[1] = (int) (Minecraft.getMinecraft().theWorld.getTotalWorldTime() / 20);

        flagMap.put(playerName, flagData);
        int MAX_FLAG_COUNT = 2;
        if (cheatName.equals(CHEAT_AUTOBLOCK)) MAX_FLAG_COUNT = 5;
        if (cheatName.equals(CHEAT_NOSLOW)) MAX_FLAG_COUNT = 3;
        if (cheatName.equals(CHEAT_KILLAURA)) MAX_FLAG_COUNT = 4;
        if (cheatName.equals(CHEAT_SCAFFOLD)) MAX_FLAG_COUNT = 4;

        if (flagData[0] >= MAX_FLAG_COUNT) {
            ChatUtil.sendFormatted(
                    String.format(
                            "%s%s%s%s failed %s%s",
                            Myau.clientName,
                            EnumChatFormatting.RED,
                            playerName,
                            EnumChatFormatting.GRAY,
                            EnumChatFormatting.RED,
                            cheatName
                    )
            );
            mc.thePlayer.playSound("random.orb", 0.3f, 1);
            String added = Myau.targetManager.add(playerName);
            if (added != null) ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your enemy list&r", Myau.clientName, added));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getMinecraft().theWorld != null) {
            int currentTimeInSeconds = (int) (Minecraft.getMinecraft().theWorld.getTotalWorldTime() / 20);

            Map<String, int[]> nextFlagMap = new HashMap<>();

            for (Map.Entry<String, int[]> entry : flagMap.entrySet()) {
                String playerName = entry.getKey();
                int[] flagData = entry.getValue();

                if (currentTimeInSeconds - flagData[1] < 1) {
                    nextFlagMap.put(playerName, flagData);
                }
            }

            flagMap.clear();
            flagMap.putAll(nextFlagMap);
        }
    }
}