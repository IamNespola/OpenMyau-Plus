# Raven Script Loader

Raven bS, with everything stripped, and a **FAR** more expansive scripting API.

### For support, issue reports, and feature suggestions, join [our discord](https://discord.gg/WCDMjEmxZE)!

## Usage

Download the jar from the [latest release](https://codeberg.org/monster-energy/raven-script-loader/releases/latest) and put it in your mods folder, it's a drop-in replacement for Raven bS in terms of script loading.

# API Changes

### Disclaimer

Script ABI compatibility with scripts for bS/b4 not guaranteed, this fork changes a lot.

## Unsafe API

RSL allows a lot more access than Raven bS in terms of modifying Minecraft. 
Enabling the `Allow unsafe scripts` option in the `Manager` in `scripts` allows loading of scripts that import Minecraft classes.
This functionality will be further expanded on in a future release, however it already allows importing of any loaded class in the current Java process.

**NOTE: Error line numbers are a bit broken with scripts that utilise `import`s and `static import`s. This will be fixed at some point, but it's not a priority.**

## `myau` module

### `myau.isEnabled(name: String): boolean`

Returns whether a module is enabled, `name` is case-insensitive.
Returns `false` if the module doesn't exist.

### `myau.setEnabled(name: String, state: boolean)`

Sets whether a module is enabled to the given state.

### `myau.toggle(name: String)`

Sets the module's state to the opposite of the current state.

### `myau.getProperty(moduleName: String, propertyName: String): String`

Gets the value of a property, `moduleName` is case-insensitive, `propertyName` is case-insensitive and `-`es are optional.
Returns `null` if the module or property do not exist.

### `myau.setProperty(moduleName: String, propertyName: String, value: Object)`

Sets the value of a property, `moduleName` is case-insensitive, `propertyName` is case-insensitive and `-`es are optional. `value` can be any type, but it must be a valid input as per the Myau command `.<moduleName> <propertyName> <value>`.

### `myau.runSilently(command: String): List<String>`

Runs the command without sending the output to the in-game chat, and instead returning it as a list of messages,

### `myau.isDiagonal(): boolean`

Myau's calculation for whether a player is going diagonally.

## `modules` module

### `modules.setCategory(category: Module.category)`

Sets the category of the script's module, should be run on `void onLoad()`.
Valid values for this are, `combat`, `movement`, `player`, `world`, `render`, `minigames`, `fun`, `other`, `client`, `profiles`, `scripts`.
They must be prefixed by `category`, so `category.combat`, etc.

## `client` module

### `client.getJumpHeight(): double`

Gets the current legal jump height of the local player.

## `world` module

### `world.isAir(x: int, y: int, z: int): boolean`, `world.isAir(x: double, y: double, z: double): boolean`

Gets whether the block at the given position is air.
The overload with doubles simply calls `Math.floor()` on them, and exists for convenience.

## `netty` module

### `isPlayingPacket(packet: Packet): boolean`

Checks whether the packet is a playing-stage packet.

### `handlePacket(packet: SPacket)`

Handles the packet via the minecraft network manager.

## `extra` module

### `extra.isDiagonal(): boolean`

Myau's calculation for whether a player is going diagonally but modified a little since at a perfect angle it may ban.

## `Entity` object

### `entity.getPrevX(): double`, `entity.getPrevY(): double`, `entity.getPrevZ(): double`

Gets the last positions, the ones last reported to the server in case of the local player.

### `entity.getX(): double`, `entity.getY(): double`, `entity.getZ(): double`

Gets the current positions of the entity, not yet reported to the server in case of the local player.

### `entity.setMotionX(motion: double)`, `entity.setMotionY(motion: double)`, `entity.setMotionZ(motion: double)`

Sets the respective motion of the entity, without affecting the other motions

### `entity.getServerPosition(): Vec3i`

Gets the current networked position of the entity.

### `entity.distanceEyesToClosest(target: Entity): double`

Gets the distance from the entity's eye position to the closest position on the target's hitbox.

### `entity.setPositionAndRotation2(x: double, y: double, z: double, yaw: float, pitch: float, positionIncrements: int, unused: boolean)`

Calls Minecraft's internal `setPositionAndRotation2` function on the entity. NOTE: the last boolean parameter is unused (?).

## `Vec3i` object

### `Vec3i.x: int`
X coordinate of the Vec3i

### `Vec3i.y: int`
Y coordinate of the Vec3i

### `Vec3i.z: int`
Z coordinate of the Vec3i

## Removals

### `modules` module

- `getBedAuraProgress`
- `getKillAuraTarget`
- `getBedAuraPosition`
- `isScaffolding`
- `isTowering`

### entire modules

- `render.blur` **(no-op stub exists)**
- `render.bloom` **(no-op stub exists)**

### Raven features

- `Settings`: `Rotate body`, `Full body`
- Bloom and blur everywhere.