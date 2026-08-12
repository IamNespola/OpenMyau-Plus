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

package de.florianmichael.vialoadingbase.platform.viaversion;

import com.viaversion.viaversion.api.platform.ViaInjector;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectLinkedOpenHashSet;
import com.viaversion.viaversion.libs.gson.JsonObject;
import de.florianmichael.vialoadingbase.netty.VLBPipeline;
import java.util.SortedSet;

public class VLBViaInjector implements ViaInjector {

  @Override
  public void inject() {}

  @Override
  public void uninject() {}

  @Override
  public String getDecoderName() {
    return VLBPipeline.VIA_DECODER_HANDLER_NAME;
  }

  @Override
  public String getEncoderName() {
    return VLBPipeline.VIA_ENCODER_HANDLER_NAME;
  }

  @Override
  public SortedSet<ProtocolVersion> getServerProtocolVersions() {
    final SortedSet<ProtocolVersion> versions = new ObjectLinkedOpenHashSet<>();
    versions.addAll(ProtocolVersion.getProtocols());
    return versions;
  }

  @Override
  public ProtocolVersion getServerProtocolVersion() {
    return this.getServerProtocolVersions().first();
  }

  @Override
  public JsonObject getDump() {
    return new JsonObject();
  }
}
