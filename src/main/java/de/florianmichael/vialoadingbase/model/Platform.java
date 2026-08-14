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
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Platform {
  public static int COUNT = 0;

  private final String name;
  private final BooleanSupplier load;
  private final Runnable executor;
  private final Consumer<List<ProtocolVersion>> versionCallback;

  public Platform(String name, BooleanSupplier load, Runnable executor) {
    this(name, load, executor, null);
  }

  public Platform(
      String name,
      BooleanSupplier load,
      Runnable executor,
      Consumer<List<ProtocolVersion>> versionCallback) {
    this.name = name;
    this.load = load;
    this.executor = executor;
    this.versionCallback = versionCallback;
  }

  public String getName() {
    return name;
  }

  public void createProtocolPath() {
    if (this.versionCallback != null) {
      this.versionCallback.accept(ViaLoadingBase.PROTOCOLS);
    }
  }

  public void build(final Logger logger) {
    if (this.load.getAsBoolean()) {
      try {
        this.executor.run();
        logger.info("Loaded Platform " + this.name);
        COUNT++;
      } catch (Throwable t) {
        logger.severe("An error occurred while loading Platform " + this.name + ":");
        t.printStackTrace();
      }
      return;
    }
    logger.severe("Platform " + this.name + " is not present");
  }
}
