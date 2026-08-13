package keystrokesmod.utility;

import keystrokesmod.Raven;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Debugger {
    private static Minecraft mc = Minecraft.getMinecraft();
    public static boolean MIXIN; // for debugging mixin related
    public static boolean BACKGROUND; // background processes like cache clearing and such

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Raven.debug || ev.phase != TickEvent.Phase.END || !Utils.nullCheck()) {
            return;
        }
        if (mc.currentScreen == null) {
            RenderUtils.renderBPS(true, true);
        }
    }

    public static void debugMixin(Object obj, String message) {
        if (!MIXIN) {
            return;
        }
        Utils.sendMessage("&d" + obj.getClass().getSimpleName() + "&7: " + message);
    }
}
