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

package de.florianmichael.vialoadingbase.model;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

@Deprecated
public class ProtocolRange {
  private final ProtocolVersion lowerBound;
  private final ProtocolVersion upperBound;

  public ProtocolRange(ProtocolVersion lowerBound, ProtocolVersion upperBound) {
    if (lowerBound == null && upperBound == null) {
      throw new RuntimeException("Invalid protocol range");
    }
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  public static ProtocolRange andNewer(final ProtocolVersion version) {
    return new ProtocolRange(null, version);
  }

  public static ProtocolRange singleton(final ProtocolVersion version) {
    return new ProtocolRange(version, version);
  }

  public static ProtocolRange andOlder(final ProtocolVersion version) {
    return new ProtocolRange(version, null);
  }

  public boolean contains(final ProtocolVersion protocolVersion) {
    if (this.lowerBound != null && protocolVersion.olderThan(lowerBound)) return false;

    return this.upperBound == null || protocolVersion.olderThanOrEqualTo(upperBound);
  }

  @Override
  public String toString() {
    if (lowerBound == null) return upperBound.getName() + "+";
    if (upperBound == null) return lowerBound.getName() + "-";
    if (lowerBound == upperBound) return lowerBound.getName();

    return lowerBound.getName() + " - " + upperBound.getName();
  }
}
