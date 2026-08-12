package keystrokesmod.mixin.impl.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin(Minecraft.class)
public interface IAccessorMinecraft {
    @Accessor("timer")
    Timer getTimer();

    @Accessor("rightClickDelayTimer")
    int getRightClickDelayTimer();

    @Accessor("rightClickDelayTimer")
    void setRightClickDelayTimer(int delay);

    @Accessor("leftClickCounter")
    void setLeftClickCounter(int delay);

    @Invoker("rightClickMouse")
    void callRightClickMouse();

    @Invoker("clickMouse")
    void callClickMouse();
}