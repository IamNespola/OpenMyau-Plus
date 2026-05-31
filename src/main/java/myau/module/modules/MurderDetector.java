package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.LoadWorldEvent;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MurderDetector extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int NOTIFY_COLOR = 0xFF5555;
    private static final Set<Integer> MURDER_ITEMS = new HashSet<>();
    private static final Set<UUID> MURDERER_IDS = new HashSet<>();
    private static final float DEFAULT_TEXT_WIDTH = 130.0F;
    public static int textX = -1;
    public static int textY = 66;

    static {
        int[] itemIds = new int[]{
                267, 272, 256, 280, 271, 268, 273, 369, 277, 359,
                400, 285, 398, 357, 279, 283, 276, 293, 421, 333,
                409, 349, 364, 382, 351, 340, 406, 396, 260, 2258,
                76, 32, 19, 122, 175, 405, 130
        };
        for (int id : itemIds) {
            MURDER_ITEMS.add(id);
        }
    }

    public final BooleanProperty showText = new BooleanProperty("show-text", true);
    public final BooleanProperty chat = new BooleanProperty("chat", true);
    public final BooleanProperty notification = new BooleanProperty("notification", true);

    private final Set<UUID> notifiedIds = new HashSet<>();
    private EntityPlayer murder1;
    private EntityPlayer murder2;

    public MurderDetector() {
        super("MurdererDetector", false, false, "Detects Murder Mystery murderers and marks them");
    }

    public static boolean isMurderer(EntityPlayer player) {
        return player != null && MURDERER_IDS.contains(player.getUniqueID());
    }

    public static void setTextPosition(int x, int y) {
        textX = x;
        textY = y;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        if (mc.thePlayer.ticksExisted % 2 != 0) {
            return;
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            this.checkPlayer(player);
        }

        if (mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                EntityPlayer player = mc.theWorld.getPlayerEntityByUUID(info.getGameProfile().getId());
                this.checkPlayer(player);
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || !this.showText.getValue()) {
            return;
        }

        float x = getTextX();
        if (this.murder1 == null && this.murder2 == null) {
            this.drawText("Murderers: §cNone", x, textY);
            return;
        }

        this.drawText("Murderers:", x, textY);
        int row = 1;
        if (this.murder1 != null) {
            this.drawText("- §e" + this.murder1.getName(), x, textY + 11.0F * row++);
        }
        if (this.murder2 != null) {
            this.drawText("- §e" + this.murder2.getName(), x, textY + 11.0F * row);
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.clearDetections();
    }

    private void checkPlayer(EntityPlayer player) {
        if (player == null || player == mc.thePlayer || isMurderer(player)) {
            return;
        }

        ItemStack heldItem = player.getHeldItem();
        if (!this.isMurderItem(heldItem)) {
            return;
        }

        this.addMurderer(player);
    }

    private boolean isMurderItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }

        String displayName = stack.getDisplayName();
        if (displayName != null && displayName.toLowerCase().contains("knife")) {
            return true;
        }

        return MURDER_ITEMS.contains(Item.getIdFromItem(stack.getItem()));
    }

    private void addMurderer(EntityPlayer player) {
        MURDERER_IDS.add(player.getUniqueID());

        if (this.murder1 == null) {
            this.murder1 = player;
        } else if (this.murder2 == null && !player.getUniqueID().equals(this.murder1.getUniqueID())) {
            this.murder2 = player;
        }

        if (!this.notifiedIds.add(player.getUniqueID())) {
            return;
        }

        String message = player.getName() + " is Murderer!";
        if (this.chat.getValue()) {
            ChatUtil.sendFormatted("&7[&cMurdererDetector&7] &e" + player.getName() + " &fis Murderer!");
        }
        if (this.notification.getValue() && Myau.notificationManager != null) {
            Myau.notificationManager.add(message, 6000L, NOTIFY_COLOR);
        }
    }

    private void drawText(String text, float x, float y) {
        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
    }

    private float getTextX() {
        return textX < 0 ? new ScaledResolution(mc).getScaledWidth() / 2.0F - DEFAULT_TEXT_WIDTH / 2.0F : textX;
    }

    private void clearDetections() {
        this.murder1 = null;
        this.murder2 = null;
        this.notifiedIds.clear();
        MURDERER_IDS.clear();
    }

    @Override
    public void onDisabled() {
        this.clearDetections();
    }
}
