package myau.mixin.viaversion;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(S32PacketConfirmTransaction.class)
public class MixinS32PacketConfirmTransaction {

  @Shadow private int windowId;

  @Shadow private short actionNumber;

  @Shadow private boolean field_148893_c;

  /**
   * @author FlorianMichael
   * @reason 1.17+ window confirmation packet layout
   */
  @Overwrite
  public void readPacketData(PacketBuffer buf) {
    if (ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
      this.windowId = buf.readInt();
    } else {
      this.windowId = buf.readUnsignedByte();
      this.actionNumber = buf.readShort();
      this.field_148893_c = buf.readBoolean();
    }
  }
}
