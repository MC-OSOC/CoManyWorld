# co-many-world
![Description of image](backgrounds-01.png)

co-many-world is a plugin for **Spigot and Paper** servers running Minecraft 1.21 and above (including 26.x) that allows you to create and manage multiple worlds.
#### This is an experimental plugin for testing purposes only. It is not recommended for use on live servers as it is still in development and may not be complete.

[Thai version](https://github.com/MC-OSOC/co-many-world/blob/main/README.md)
## Features

- Easily create new worlds with commands
- Quickly teleport between different worlds
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
To check that the code also compiles against the Paper API, run `mvn package -Ppaper`.

## Commands

#### Create a new world
`/co-many create <worldName> [-11|-12|-13|-all]`
- `-11` : Normal world (default)
- `-12` : Nether world
- `-13` : The End world
- `-all`: Create all world types
- `-s` : Seed world

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

## Permissions

| Permissions             | Command                                                                                         | Properties                                                |
|-------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| co.many.worlds.admin    | /co-many create <br/>/co-many tp <br/> /co-many list <br/> /co-many import <br/> /co-many about <br/> /co-many backup | Permission to manage worlds (create, teleport, list, import, backup) |
| co.many.worlds.admindel | /co-many delete <worldName>                                                                     | Permission to delete worlds                                |
| co.many.worlds.adminclear | /co-many clear, /co-many-clear                                                                                | Permission to permanently delete worlds                    |

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
```

The `delete_worlds.yml` file stores the names of worlds that have been removed from `config.yml` and are waiting for permanent deletion:
```yaml
delete_worlds: []
```

## Importing Worlds

Put a world folder or ZIP file in `plugins/CoManyWorld/import` and run `/co-many import <file name>`. The plugin searches for `level.dat` to find the root of the world (`session.lock` and `uid.dat` are not copied). Copying runs in the background, so the server doesn't freeze.

## World Backup

You can backup a world using the command `/co-many backup <worldName>`. The world is saved first and then zipped in the background. Backup files are stored in `plugins/CoManyWorld/many_world_backup` and can be imported again.