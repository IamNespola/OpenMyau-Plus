//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package myau.module.modules.movement;

import myau.event.EventTarget;
import myau.events.TickEvent;
import myau.mixin.IAccessorEntityPlayer;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class FastBow extends Module {
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"Instant", "NCP"});
    public final IntProperty packets = new IntProperty("packets", 20, 3, 20);

    public FastBow() {
        super("FastBow", Category.MOVEMENT,false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;

        if (mc.thePlayer != null && mc.theWorld != null) {
            if (mc.thePlayer.isEating() || mc.thePlayer.isUsingItem()) {
                ItemStack currentItem = mc.thePlayer.getCurrentEquippedItem();
                if (currentItem != null && currentItem.getItem() instanceof ItemBow) {
                    switch ((int) this.mode.getValue()) {
                        case 0:
                            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mc.thePlayer.getCurrentEquippedItem(), 0.0F, 0.0F, 0.0F));
                            float yaw = mc.thePlayer.rotationYaw;
                            float pitch = mc.thePlayer.rotationPitch;
                            int packetCount = (int) this.packets.getValue();

                            for (int i = 0; i < packetCount; ++i) {
                                PacketUtil.sendPacket(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround));
                            }

                            PacketUtil.sendPacket(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                            ((IAccessorEntityPlayer) mc.thePlayer).setItemInUseCount(currentItem.getMaxItemUseDuration() - 1);
                            break;
                        case 1:
                            if (mc.thePlayer.getItemInUseDuration() > 14) {
                                float ncpYaw = mc.thePlayer.rotationYaw;
                                float ncpPitch = mc.thePlayer.rotationPitch;
                                int ncpPackets = (int) this.packets.getValue();

                                for (int i = 0; i < ncpPackets; ++i) {
                                    PacketUtil.sendPacket(new C03PacketPlayer.C05PacketPlayerLook(ncpYaw, ncpPitch, mc.thePlayer.onGround));
                                }

                                PacketUtil.sendPacket(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                                mc.playerController.onStoppedUsingItem(mc.thePlayer);
                            }
                    }
                }

            }
        }
    }

    public String[] getSuffix() {
        String[] modes = new String[]{"Instant", "NCP"};
        return new String[]{modes[(int) this.mode.getValue()]};
    }
}
