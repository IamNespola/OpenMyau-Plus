package myau.module.modules.misc;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.mixin.IAccessorGuiScreen;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Mouse;

public class InventoryClicker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty triggerTicks = new IntProperty("ticks", 2, 0, 20);
    public int ticks;

    public InventoryClicker() {
        super("InventoryClicker", Category.MISC,false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{triggerTicks.getValue().toString() + " ticks"};
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && mc.thePlayer != null && event.getType() == EventType.PRE) {
            if (mc.currentScreen instanceof GuiContainer) {
                GuiContainer screen = ((GuiContainer) mc.currentScreen);
                final int mouseX = Mouse.getEventX() * screen.width / mc.displayWidth;
                final int mouseY = screen.height - Mouse.getEventY() * screen.height / mc.displayHeight - 1;
                if (Mouse.isButtonDown(0)) {
                    ticks++;
                    if(ticks > triggerTicks.getValue())
                    {
                        ((IAccessorGuiScreen)screen).callMouseClicked(mouseX, mouseY, 0);
                    }
                }else {
                    ticks = 0;
                }
            }
        }
    }
}
