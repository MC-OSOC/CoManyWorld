# co-many-world
![Description of image](backgrounds-01.png)

co-many-world is a plugin for **Spigot and Paper** servers running Minecraft 1.21 and above (including 26.x) that allows you to create and manage multiple worlds.
#### This is an experimental plugin for testing purposes only. It is not recommended for use on live servers as it is still in development and may not be complete.

[Thai version](https://github.com/MC-OSOC/co-many-world/blob/main/README.md)
## Features

- Easily create new worlds with commands
- Quickly teleport between different worlds (regular players can use it too, with per-world permissions)
- Several world types: normal, flat, void (empty), large biomes, amplified, or a generator from another plugin
- Delete unwanted worlds
- List created worlds
- Import worlds from ZIP files or folders
- Backup world data
- Compatible with EssentialsX
- Works on both Spigot and Paper with the same .jar (on Paper, teleports load chunks asynchronously so the server doesn't freeze)

## Installation

1. Download the file from the release section
2. Place the .jar file in the `plugins` folder of your Minecraft server (Spigot or Paper, Java 21 or newer)

### Building
Requires JDK 21 and Maven.
```bash
mvn package
```
The jar is written to `target/CoManyWorld-<version>.jar` and works on both Spigot and Paper.
To check that the code also compiles against the Paper API and run the tests (MockBukkit), use `mvn package -Ppaper`.

Releases are built by GitHub Actions: see [docs/RELEASING.md](docs/RELEASING.md) (Thai).

Every pull request is also tested on real servers (Paper 1.21.11, Paper 26.1 and Spigot 1.21.11) by the [Gameplay test](.github/workflows/gameplay-test.yml) workflow (the [Server test](.github/workflows/server-test.yml) workflow checks create / backup / import / upgrades from the console): it creates every kind of world, checks the terrain, joins as an ordinary player with a [mineflayer](https://github.com/PrismarineJS/mineflayer) bot to check the `tp` permissions, and restarts the server to check that the worlds come back with the same type. The test code is in [server-test/](server-test/).

## Commands

#### Create a new world
`/co-many create <worldName> [-11|-12|-13|-all] [-s<seed>] [-flat|-void|-large|-amplified|-g<plugin[:id]>]`
- `-11` : Normal world (default; with no type, `default-world-type` from `config.yml` is used)
- `-12` : Nether world
- `-13` : The End world
- `-all`: Create all world types
- `-s<seed>` : Seed world

World type (pick one; only for the overworld, so use it with `-11` or `-all` — the Nether / End of an `-all` world are always normal):
- `-flat` : Flat world
- `-void` : Empty world with a small 3x3 stone platform to stand on (lobbies / mini-games)
- `-large` : Large Biomes
- `-amplified` : Amplified
- `-g<plugin[:id]>` : Use a generator from another plugin, e.g. `-gMyGenerator` or `-gMyGenerator:lobby` (that plugin must be installed and enabled)

Examples: `/co-many create lobby -11 -void` and `/co-many create survival -all -s12345 -large`

The world type is saved in `config.yml` (section `world-settings`) so the world is loaded the same way every time the server starts, and it is removed when the world is deleted.

#### Basic commands
- **Teleport to a world**: `/co-many tp <worldName>`
- **List worlds**: `/co-many list`
- **Delete a world** (moves it to the trash, files are kept): `/co-many delete <worldName>` or a whole group with `/co-many delete many_world/<name>/`
- **Import a world**: `/co-many import <worldName>`
- **Backup a world**: `/co-many backup <worldName>`
- **Permanently delete worlds** (worlds in the trash): `/co-many clear` to list them, then `/co-many clear confirm` to delete (or `/co-many-clear confirm`)
- **Plugin information**: `/co-many about`

## World Storage
- Worlds created by this plugin will be saved in the format `many_world/custom_world/world`, except for the server's default world
- World names may only use `a-z`, `0-9`, `_` and `-` (upper case is converted to lower case)
- delete / clear only work on worlds inside `many_world/`; the server's own worlds can never be deleted
  ```text
  many_world/
  └─custom_world/
    └─world/
  ```
- If created with the `-all` option, all world types will be in the same folder
  ```text
  many_world/
  └─custom_world/
     ├─world/
     ├─world_nether/
     └─world_the_end/
  ```
- **Paper 26.x** keeps every world inside the main world's folder instead: `world/dimensions/minecraft/many_world/custom_world/world`
  (the world's name in game is still `many_world/custom_world/world`; `/co-many about` shows where they are stored)
- Upgrading from Paper 1.21 to 26.x: Paper moves the old worlds from `many_world/` into `world/dimensions/...` by itself on the first start, and the plugin finds them in both places.
  **Always back up the whole server folder before upgrading.**

## Permissions

| Permissions             | Command                                                                                         | Properties                                                |
|-------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| co.many.worlds.admin    | /co-many create <br/>/co-many tp <br/> /co-many list <br/> /co-many import <br/> /co-many about <br/> /co-many backup | Permission to manage worlds (create, teleport, list, import, backup). Includes `co.many.worlds.tp` |
| co.many.worlds.tp       | /co-many tp <worldName>                                                                         | Permission to teleport to every world (no admin needed). Default: op |
| co.many.worlds.tp.&lt;world name&gt; | /co-many tp &lt;world name&gt;                                                       | Permission to teleport to that one world, e.g. `co.many.worlds.tp.many_world/lobby/world` |
| co.many.worlds.admindel | /co-many delete <worldName>                                                                     | Permission to delete worlds                                |
| co.many.worlds.adminclear | /co-many clear, /co-many-clear                                                                                | Permission to permanently delete worlds                    |

To let regular players teleport to a lobby, give them `co.many.worlds.tp.many_world/lobby/world` (with LuckPerms or another permissions plugin). Worlds a player may not enter are hidden from their tab-complete, and every other `/co-many` command stays admin-only.

## Configuration

The `config.yml` file will be created when you start the server for the first time. You can adjust the settings as follows:

```yaml
# Configuration file for CoManyWorld
default-world-type: -11  # Default to normal world
# Where players respawn when they die outside a CoManyWorld world (falls back to "world").
default-world: 'world'
# Used for portals when a CoManyWorld world has no Nether / End of its own
# (only when prevent-single-world-portals is false).
default-nether-world: 'world_nether'
default-end-world: 'world_the_end'
worlds: []   # List of worlds to be loaded on startup (managed by the plugin)
prevent-single-world-portals: true  # Prevent portal usage in worlds that don't have all 3 dimensions
# world-settings:   # added by the plugin when a world is created with -flat / -void / -large / -amplified / -g
#   many_world/lobby/world:
#     type: void
```

The `delete_worlds.yml` file stores the names of worlds that have been removed from `config.yml` and are waiting for permanent deletion:
```yaml
delete_worlds: []
```

## Importing Worlds

Put a world folder or ZIP file in `plugins/CoManyWorld/import` and run `/co-many import <file name>`. Copying runs in the background, so the server doesn't freeze.
- The plugin looks for a folder with `level.dat` (single player saves, Spigot, Paper 1.21) or a Paper 26.x world folder (with `data/paper/level_overrides.dat`).
- Paper 26.x worlds (e.g. backups made on Paper 26.x) can only be imported on Paper 26.x.
- `session.lock`, `uid.dat` and `data/paper/metadata.dat` are not copied (otherwise the server sees a duplicate world).
- Symbolic links are not copied, and a ZIP that unpacks to more than the free disk space is stopped.

## World Backup

You can backup a world using the command `/co-many backup <worldName>`. The world is saved first and then zipped in the background. Backup files are stored in `plugins/CoManyWorld/many_world_backup` and can be imported again.