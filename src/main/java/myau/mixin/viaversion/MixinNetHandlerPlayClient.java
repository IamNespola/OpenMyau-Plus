package myau.mixin.viaversion;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NetHandlerPlayClient.class, priority = 1001)
public class MixinNetHandlerPlayClient {

  @Shadow private Minecraft gameController;

  @Shadow
  public void addToSendQueue(Packet<?> packet) {}

  /**
   * @author toidicakhia, FlorianMichael
   * @reason 1.17+ window confirmation translation
   */
  @Overwrite
  public void handleConfirmTransaction(S32PacketConfirmTransaction packetIn) {
    PacketThreadUtil.checkThreadAndEnqueue(
        packetIn, (NetHandlerPlayClient) (Object) this, this.gameController);

    if (ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
      this.addToSendQueue(
          new C0FPacketConfirmTransaction(packetIn.getWindowId(), (short) 0, false));
      return;
    }

    Container container = null;
    EntityPlayer entityplayer = this.gameController.thePlayer;

    if (entityplayer == null) return;

    if (packetIn.getWindowId() == 0) {
      container = entityplayer.inventoryContainer;
    } else if (packetIn.getWindowId() == entityplayer.openContainer.windowId) {
      container = entityplayer.openContainer;
    }

    if (container != null && !packetIn.func_148888_e()) {
      this.addToSendQueue(
          new C0FPacketConfirmTransaction(
              packetIn.getWindowId(), packetIn.getActionNumber(), true));
    }
  }
}
