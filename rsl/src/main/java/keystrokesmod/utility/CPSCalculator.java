package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.module.impl.client.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class CPSCalculator {
    private static Minecraft mc = Minecraft.getMinecraft();
    private static List<Long> a = new ArrayList();
    private static List<Long> b = new ArrayList();
    public static long LL = 0L;
    public static long LR = 0L;

    @SubscribeEvent
    public void onMouseUpdate(MouseEvent d) {
        if (d.buttonstate) {
            if (d.button == 0) {
                aL();
                if (Raven.debug && mc.objectMouseOver != null) {
                    Entity en = mc.objectMouseOver.entityHit;
                    if (en == null) {
                        return;
                    }

                    Utils.sendMessage("&7&m-------------------------");
                    Utils.sendMessage("&7n: " + en.getName());
                    Utils.sendMessage("&7rn: " + en.getName().replace("§", "%"));
                    Utils.sendMessage("&7d: " + en.getDisplayName().getUnformattedText());
                    Utils.sendMessage("&7rd: " + en.getDisplayName().getUnformattedText().replace("§", "%"));
                    Utils.sendMessage("&7type: " + en.getClass().getSimpleName());
                }
            } else if (d.button == 1) {
                aR();
            }
            else if (d.button == 2 && Settings.middleClickFriends.isToggled()) {
                EntityLivingBase g = Utils.raytrace(200);
                if (g != null && !Utils.addFriend(g.getName())) {
                    Utils.removeFriend(g.getName());
                }
            }
        }
    }

    public static void aL() {
        a.add(LL = System.currentTimeMillis());
    }

    public static void aR() {
        b.add(LR = System.currentTimeMillis());
    }

    public static int f() {
        a.removeIf(o -> (Long) o < System.currentTimeMillis() - 1000L);
        return a.size();
    }

    public static int i() {
        b.removeIf(o -> (Long) o < System.currentTimeMillis() - 1000L);
        return b.size();
    }
}
