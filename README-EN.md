# co-many-world
![Description of image](backgrounds-01.png)

co-many-world is a plugin for Minecraft 1.21 and above that allows you to create and manage multiple worlds.
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

## Installation

1. Download the file from the release section
2. Place the .jar file in the `plugins` folder of your Minecraft server

## Commands

#### Create a new world
`/co-many create <worldName> [-11|-12|-13|-all]`
- `-11` : Normal world (default)
- `-12` : Nether world
- `-13` : The End world
- `-all`: Create all world types

#### Basic commands
- **Teleport to a world**: `/co-many tp <worldName>`
- **List worlds**: `/co-many list`
- **Delete a world** (removes from config.yml only): `/co-many delete <worldName>`
- **Import a world**: `/co-many import <worldName>`
- **Backup a world**: `/co-many backup <worldName>`
- **Permanently delete worlds** (worlds in the trash): `/co-many-clear`
- **Plugin information**: `/co-many about`

## World Storage
- Worlds created by this plugin will be saved in the format `many_world/custom_world/world`, except for the server's default world
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
| co.many.worlds.adminclear | /co-many-clear                                                                                | Permission to permanently delete worlds                    |

## Configuration

The `config.yml` file will be created when you start the server for the first time. You can adjust the settings as follows:

```yaml
# Configuration file for CoManyWorld
default-world-type: -11  # Default to normal world
default-world: 'World'
default-nether-world: 'world_nether'
default-end-world: 'world_the_end'
worlds: []   # List of worlds to be loaded on startup
```

The `delete_worlds.yml` file stores the names of worlds that have been removed from `config.yml` and are waiting for permanent deletion:
```yaml
delete_worlds: []
```

## Importing Worlds

The plugin can import worlds from ZIP files or folders. It will search for the `level.dat` file to identify the root location of the world.

## World Backup

You can backup a world using the command `/co-many backup <worldName>`. Backup files will be stored in `plugins/co-many-world/many_world_backup`