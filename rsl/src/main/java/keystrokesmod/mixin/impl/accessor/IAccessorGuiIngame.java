package keystrokesmod.mixin.impl.accessor;

import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(GuiIngame.class)
public interface IAccessorGuiIngame {
    @Accessor("recordPlaying")
    String getRecordPlaying();

    @Accessor("displayedTitle")
    String getDisplayedTitle();

    @Accessor("displayedSubTitle")
    String getDisplayedSubTitle();
}
