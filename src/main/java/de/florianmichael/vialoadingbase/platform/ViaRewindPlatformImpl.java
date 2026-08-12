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

package de.florianmichael.vialoadingbase.platform;

import com.viaversion.viarewind.api.ViaRewindPlatform;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import java.io.File;
import java.util.logging.Logger;

public class ViaRewindPlatformImpl implements ViaRewindPlatform {

  private final File directory;

  public ViaRewindPlatformImpl(final File directory) {
    this.init(new File(this.directory = directory, "viarewind.yml"));
  }

  @Override
  public Logger getLogger() {
    return ViaLoadingBase.LOGGER;
  }

  @Override
  public File getDataFolder() {
    return this.directory;
  }
}
