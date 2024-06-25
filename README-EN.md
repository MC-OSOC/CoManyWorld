# co-many-world

co-many-world is a plugin for Minecraft 1.21 and above that allows you to create and manage multiple worlds.

#### This is an experimental plugin for testing purposes only. It is not recommended for use on production servers as it is still in development and may not be fully functional.

## Features
- Easily create new worlds with commands
- Quickly teleport to different worlds
- Delete unwanted worlds
- List created worlds
- Supports EssentialsX

## Installation
1. Download the file from the release
2. Place the .jar file in the `plugins` folder of your Minecraft server

## Commands
#### Create a new world
`/co-many create <worldName> [-11|-12|-13|-all]`
- `-11`: Normal world (default)
- `-12`: Nether world
- `-13`: The End world
- `-all`: Create all world types

#### Teleport to a world
`/co-many tp <worldName>`

#### List worlds
`/co-many list`

#### Delete a world (removes the world from config.yml only)
`/co-many delete <worldName>`

#### Import a world
`/co-many import <worldName>`

## World Saving
- Worlds created by this plugin will be saved in the `many_world/custom_world` folder, separate from the server's default world.
-   ```text
    many_world/
    ├─custom_world/
- All worlds will be saved. If created with the `-all` option, they will be grouped in the same folder:
  ```text
  many_world/
  ├─custom_world/
     ├─world/
     ├─world_nether/
     ├─world_the_end/

## Permissions
- `co.many.worlds.admin`: Permission to manage worlds (create, teleport, list worlds)
- `co.many.worlds.admindel`: Permission to delete worlds

| Permissions             | Command                                                                                         | Properties                                                        |
|-------------------------|-------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| co.many.worlds.admin    | /co-many create <br/>/co-many tp <br/> /co-many list <br/> /co-many import <br/> /co-many about | Permission to manage worlds (create, teleport, list, import)      |
| co.many.worlds.admindel | /co-many delete <worldName>                                                                     | Permission to delete worlds                                       |

## Configuration
The `config.yml` file will be created when you start the server for the first time. You can adjust the settings as follows:

```yaml
# Configuration file for CoManyWorld
default-world-type: -11  # Default to normal world
default-world: 'World'
main-nether-world: 'world_nether'
main-end-world: 'world_the_end'
worlds: []   # List of worlds to be loaded on startup
```

The `delete_worlds.yml` file is used to store worlds that have been removed from `config.yml`:

```yaml
delete_worlds: []
```
