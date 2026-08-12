/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viamcp;

import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import java.io.File;

public class ViaMCP {
  public static final int NATIVE_VERSION = 47;
  public static ViaMCP INSTANCE;

  public static void create() {
    INSTANCE = new ViaMCP();
  }

  public ViaMCP() {
    ViaLoadingBase.ViaLoadingBaseBuilder.create()
        .runDirectory(new File("ViaMCP"))
        .nativeVersion(NATIVE_VERSION)
        .build();
  }

  private void fixTransactions() {
    final Protocol1_16_4To1_17 protocol =
        Via.getManager().getProtocolManager().getProtocol(Protocol1_16_4To1_17.class);
    protocol.registerClientbound(
        ClientboundPackets1_17.PING,
        ClientboundPackets1_16_2.WINDOW_CONFIRMATION,
        wrapper -> {},
        true);
    protocol.registerServerbound(
        ServerboundPackets1_16_2.WINDOW_CONFIRMATION,
        ServerboundPackets1_17.PONG,
        wrapper -> {},
        true);
  }
}
