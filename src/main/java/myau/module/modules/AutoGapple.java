package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemFood;
import myau.mixin.IAccessorKeyBinding; // Import IAccessorKeyBinding


public class AutoGapple extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final PercentProperty healthPercentage = new PercentProperty("Health-Percent", 50); // Default to 50%
    public final BooleanProperty eatInCombat = new BooleanProperty("Eat-In-Combat", false); // Default to false

    // New fields for continuous eating simulation
    private boolean isEating = false;
    private int eatTickCount = 0;
    private static final int EAT_DURATION_TICKS = 32; // Roughly 1.6 seconds (20 ticks/sec * 1.6)
    private int originalHotbarSlot = -1;

    public AutoGapple() {
        super("Gapple", false, false); // Name, default enabled (false), default hidden (false)
    }

    @Override
    public void onEnabled() {
        // Reset state when module is enabled
        isEating = false;
        eatTickCount = 0;
        originalHotbarSlot = -1;
        super.onEnabled();
    }

    @Override
    public void onDisabled() {
        // Ensure right-click is released if module is disabled during eating
        if (isEating) {
            ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(false);

        }
        isEating = false;
        eatTickCount = 0;
        originalHotbarSlot = -1;
        super.onDisabled();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }


        if (event.getType() != EventType.PRE) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        // --- Continuous Eating Logic ---
        if (isEating) {
            // Check if player is still holding the golden apple
            ItemStack heldItem = mc.thePlayer.getHeldItem();
            if (heldItem == null || heldItem.getItem() != Items.golden_apple) {
                // Golden apple was consumed or switched away mid-eating, stop eating simulation
                ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(false);
                isEating = false;

                // Restore original slot if it was valid
                if (originalHotbarSlot != -1 && mc.thePlayer.inventory.currentItem != originalHotbarSlot) {
                    mc.thePlayer.inventory.currentItem = originalHotbarSlot;
                    mc.playerController.updateController();
                }
                originalHotbarSlot = -1;
                eatTickCount = 0;
                return;
            }

            eatTickCount++;


            if (eatTickCount >= EAT_DURATION_TICKS) {
                ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(false);
                isEating = false;

                // Restore original slot
                if (originalHotbarSlot != -1 && mc.thePlayer.inventory.currentItem != originalHotbarSlot) {
                    mc.thePlayer.inventory.currentItem = originalHotbarSlot;
                    mc.playerController.updateController();
                }
                originalHotbarSlot = -1;
                eatTickCount = 0;
            }
            return; // Don't try to start eating again if already eating
        }
        // --- End Continuous Eating Logic ---

        // Only proceed if not currently eating
        if (!isEating) {
            if (!eatInCombat.getValue() && Myau.playerStateManager.isInCombat()) {

                return;
            }

            float currentHealth = mc.thePlayer.getHealth();
            float maxHealth = mc.thePlayer.getMaxHealth();
            float healthPercent = (currentHealth / maxHealth) * 100.0F;

            if (healthPercent <= healthPercentage.getValue().floatValue()) {

                int gappleSlot = findGappleInHotbar();

                if (gappleSlot != -1) {


                    // Start eating process
                    originalHotbarSlot = mc.thePlayer.inventory.currentItem; // Store original slot
                    mc.thePlayer.inventory.currentItem = gappleSlot; // Select GApple
                    mc.playerController.updateController(); // Update controller

                    ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(true); // Simulate holding right-click
                    isEating = true;
                    eatTickCount = 0; // Reset tick count for new eating process

                } else {

                }
            } else {

            }
        }
    }

    private int findGappleInHotbar() {
        for (int i = 0; i < 9; i++) { // Iterate through hotbar slots (0-8)
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemFood) {
                if (stack.getItem() == Items.golden_apple) {
                    return i;
                }
            }
        }
        return -1; // No Golden Apple found
    }
}
