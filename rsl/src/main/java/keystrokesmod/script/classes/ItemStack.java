package keystrokesmod.script.classes;

import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemStack {
    public String type;
    public String name;
    public String displayName;
    public int stackSize;
    public int maxStackSize;
    public int durability;
    public int maxDurability;
    public boolean isBlock;
    public net.minecraft.item.ItemStack itemStack;
    public int meta;

    public ItemStack(net.minecraft.item.ItemStack itemStack, byte f1) {
        if (itemStack == null) {
            return;
        }
        this.itemStack = itemStack;
        this.isBlock = itemStack.getItem() instanceof ItemBlock;
        this.type = isBlock ? ((ItemBlock) itemStack.getItem()).getBlock().getClass().getSimpleName() : itemStack.getItem().getClass().getSimpleName();
        this.name = itemStack.getItem().getRegistryName().substring(10); // substring 10 to remove "minecraft:"
        this.displayName = itemStack.getDisplayName();
        this.stackSize = itemStack.stackSize;
        this.maxStackSize = itemStack.getMaxStackSize();
        this.durability = itemStack.getMaxDamage() - itemStack.getItemDamage();
        this.maxDurability = itemStack.getMaxDamage();
        this.meta = itemStack.getMetadata();
    }

    public ItemStack(String name) {
        this(withMeta(name), (byte) 0);
    }

    private static net.minecraft.item.ItemStack withMeta(String name) {
        String[] parts = name.split(":");
        String itemName = parts[0];
        int meta = 0;

        if (parts.length > 1) {
            meta = parseMeta(parts[1]);
        }

        net.minecraft.item.Item item = Item.itemRegistry.getObject(new ResourceLocation("minecraft:" + itemName));
        return new net.minecraft.item.ItemStack(item, 1, meta);
    }

    private static int parseMeta(String metaStr) {
        try {
            return Integer.parseInt(metaStr);
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    public List<String> getTooltip() {
        return this.itemStack.getTooltip(Minecraft.getMinecraft().thePlayer, true);
    }

    public List<Object[]> getEnchantments() {
        Map<Integer, Integer> enchants = EnchantmentHelper.getEnchantments(this.itemStack);
        if (enchants.isEmpty()) {
            return null;
        }
        List<Object[]> enchantments = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : enchants.entrySet()) {
            Enchantment enchant = Enchantment.getEnchantmentById((int)entry.getKey());
            String name = StatCollector.translateToFallback(enchant.getName()).toLowerCase().replace(" ", "_");
            enchantments.add(new Object[] { name, entry.getValue() });
        }
        return enchantments;
    }

    public static ItemStack convert(net.minecraft.item.ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        return new ItemStack(itemStack, (byte) 0);
    }

    @Override
    public String toString() {
        return "ItemStack(" + this.type + "," + this.name + ")";
    }
}
