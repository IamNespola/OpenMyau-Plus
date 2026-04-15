package myau.module.modules.player;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public class AutoSwap extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private Item lastItem;
    private int lastSlot = -1;

    public final BooleanProperty blocks = new BooleanProperty("blocks", true);
    public final BooleanProperty projectiles = new BooleanProperty("projectiles", true);
    public final BooleanProperty pearls = new BooleanProperty("pearls", true);
    public final BooleanProperty swords = new BooleanProperty("swords", true);
    public final BooleanProperty tools = new BooleanProperty("tools", true);
    public final BooleanProperty resources = new BooleanProperty("resources", true);

    private final List<String> ALLOWED_BLOCKS = Arrays.asList("stone", "grass", "dirt", "planks", "wool", "wood", "glass", "leaves", "clay", "cloth", "cobblestone", "sand", "gravel", "netherrack");
    private final List<String> PROJECTILES = Arrays.asList("egg", "snowball", "ender_pearl", "fireball");
    private final List<String> PEARLS = Arrays.asList("pearl", "ender_pearl");
    private final List<String> SWORDS = Arrays.asList("sword", "axe");
    private final List<String> TOOLS = Arrays.asList("rod", "pickaxe", "axe", "shovel", "hoe", "flint_and_steel");
    private final List<String> RESOURCES = Arrays.asList("265", "266", "388", "264", "diamond", "gold", "iron", "emerald");

    public AutoSwap() {
        super("AutoSwap", Category.PLAYER,false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }

        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }

        int slot = mc.thePlayer.inventory.currentItem;
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(slot);

        if (this.lastItem != null && slot == this.lastSlot && (held == null || held.stackSize < 1)) {
            this.swapItem(this.lastItem);
        }

        this.lastItem = held != null ? held.getItem() : null;
        this.lastSlot = slot;
    }

    private void swapItem(Item lastItem) {
        if (lastItem == null) {
            return;
        }

        String lastId = lastItem.getUnlocalizedName().toLowerCase();
        boolean isBlock = lastItem instanceof ItemBlock;
        int current = mc.thePlayer.inventory.currentItem;
        List<String> category = null;

        if (!isBlock) {
            if (this.projectiles.getValue() && containsAny(lastId, PROJECTILES) && !lastId.contains("leggings")) {
                category = PROJECTILES;
            } else if (this.pearls.getValue() && containsAny(lastId, PEARLS)) {
                category = PEARLS;
            } else if (this.swords.getValue() && containsAny(lastId, SWORDS)) {
                category = SWORDS;
            } else if (this.tools.getValue() && containsAny(lastId, TOOLS)) {
                category = TOOLS;
            } else if (this.resources.getValue() && containsAny(lastId, RESOURCES)) {
                category = RESOURCES;
            }
        }

        // Loop through hotbar to find replacement
        for (int offset = 1; offset <= 9; ++offset) {
            int i = (current + offset) % 9;
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack != null && stack.stackSize >= 1) {
                Item item = stack.getItem();
                String id = item.getUnlocalizedName().toLowerCase();

                // Check if it's the same item
                if (item == lastItem) {
                    mc.thePlayer.inventory.currentItem = i;
                    return;
                }

                // Check for blocks
                if (isBlock && this.blocks.getValue() && isValidBlock(stack)) {
                    mc.thePlayer.inventory.currentItem = i;
                    return;
                }

                // Check for category matches
                if (category != null) {
                    if (containsAny(id, category) && !id.contains("leggings")) {
                        mc.thePlayer.inventory.currentItem = i;
                        return;
                    }
                }
            }
        }
    }

    private boolean isValidBlock(ItemStack stack) {
        if (!this.blocks.getValue()) {
            return false;
        }

        if (!(stack.getItem() instanceof ItemBlock)) {
            return false;
        }

        String id = stack.getItem().getUnlocalizedName().toLowerCase();
        return containsAny(id, ALLOWED_BLOCKS);
    }

    private boolean containsAny(String str, List<String> items) {
        for (String item : items) {
            if (str.contains(item)) {
                return true;
            }
        }
        return false;
    }
}

