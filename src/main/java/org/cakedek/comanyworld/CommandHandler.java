package org.cakedek.comanyworld;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;

public class CommandHandler implements CommandExecutor {

    private final WorldManager worldManager;
    private final FileConfiguration config;
    private final JavaPlugin plugin;

    public CommandHandler(WorldManager worldManager, FileConfiguration config, JavaPlugin plugin) {
        this.worldManager = worldManager;
        this.config = config;
        this.plugin = plugin;
    }


    // COMMAND IN
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                    "/co-many create [worldName] [World]  >> Create a new world\n" +
                    "/co-many list >> world list \n" +
                    "/co-many tp [worldName] >> tp to world\n" +
                    "/co-many import [worldName] >> Import World \n"+
                    "/co-many del [worldName] >> Delete world\n" +
                    "/co-many about >> About \n");
            return true;
        }
        //////  CREATE
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length >= 3 && args[2].equalsIgnoreCase("-all")) {
                createWorld(sender, args[1], true, -1);
            } else if (args.length == 3) {
                int worldType = Integer.parseInt(args[2]);
                createWorld(sender, args[1], false, worldType);
            } else {
                sender.sendMessage(ChatColor.GOLD+"=== Many World > Create "+ ChatColor.RESET +
                                                    "\n-11 [WORLD] " +
                                                    "\n-12 [NETHER] " +
                                                    "\n-13 [THE_END] " +
                                                    "\n-all [ALL_WORLD]" +
                                                    "\nCommand: /co-many create <worldName> [-11|-12|-13]");
            }
            return true;
        }
        //////  TP
        else if (args[0].equalsIgnoreCase("tp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("Usage: /co-many tp <worldName>");
                return true;
            }
            String worldName = args[1];
            teleportPlayer((Player) sender, worldName);
        }
        //////  LIST
        else if (args[0].equalsIgnoreCase("list")) {
            listWorlds(sender);
        }
        //////  DEL
        else if (args[0].equalsIgnoreCase("del")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /co-many del <worldName>");
                return true;
            }
            String worldName = args[1];
            deleteWorld(sender, worldName);
        }
        //////  IMPORT
        else if (args[0].equalsIgnoreCase("import")) {
            if (args.length == 2) {
                String worldName = args[1];
                importWorld(sender, worldName);
            } else {
                sender.sendMessage("Usage: /co-many import <worldName>");
            }
            return true;
        }
        //////  ABOUT
        else if (args[0].equalsIgnoreCase("about")) {
            sender.sendMessage(ChatColor.GOLD  + "=== Many World List > About \n" + ChatColor.RESET +
                    "Version >>> " + CoManyWorld.getInstance().getDescription().getVersion() + "\n" +
                    "Minecraft Server Version >>> " + Bukkit.getVersion() + "\n" +
                    "->->->\n" +
                    "MC-OSOC \n" +
                    "github >>> https://github.com/MC-OSOC\n" +
                    "*** Some of the code was developed by AI.\n" +
                    "===============================");
        }

        else {
            sender.sendMessage(ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                    "/co-many create [worldName] [World]  >> Create a new world\n" +
                    "/co-many list >> world list \n" +
                    "/co-many tp [worldName] >> tp to world\n" +
                    "/co-many import [worldName] >> Import World \n"+
                    "/co-many del [worldName] >> Delete world\n" +
                    "/co-many about >> About \n");
        }
        return true;
    }



    ///// IMPORTWORLD
    private void importWorld(CommandSender sender, String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists() || !worldFolder.isDirectory()) {
            sender.sendMessage("World " + worldName + " does not exist.");
            return;
        }

        String newWorldName = worldName.startsWith("world-many_") ? worldName : "world_many_" + worldName;
        File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);

        if (worldFolder.renameTo(newWorldFolder)) {
            WorldCreator creator = new WorldCreator(newWorldName);
            World world = Bukkit.createWorld(creator);
            if (world != null) {
                sender.sendMessage("World " + worldName + " has been successfully imported and renamed to " + newWorldName);

                // Update config.yml
                List<String> worlds = config.getStringList("worlds");
                worlds.add(newWorldName);
                config.set("worlds", worlds);
                plugin.saveConfig();

            } else {
                sender.sendMessage("Failed to create the world " + newWorldName + ".");
            }
        } else {
            sender.sendMessage("Failed to rename the world folder.");
        }
    }



    ////// CREATE WORLD
    private void createWorld(CommandSender sender, String worldName, boolean createAll, int worldType) {
        String folderName = "world_many_" + worldName;
        BossBar progressBar = Bukkit.createBossBar(ChatColor.GREEN + "Creating world: " + worldName, BarColor.BLUE, BarStyle.SOLID);

        if (sender instanceof Player) {
            progressBar.addPlayer((Player) sender);
        }

        new BukkitRunnable() {
            int progress = 0;

            @Override
            public void run() {
                if (progress >= 100) {
                    cancel();
                    progressBar.removeAll();

                    if (createAll) {
                        createWorldWithEnvironment(folderName + "/" + worldName, World.Environment.NORMAL);
                        createWorldWithEnvironment(folderName + "/" + worldName + "_nether", World.Environment.NETHER);
                        createWorldWithEnvironment(folderName + "/" + worldName + "_the_end", World.Environment.THE_END);
                    } else {
                        World.Environment environment;
                        switch (worldType) {
                            case -12: //NETHER
                                createWorldWithEnvironment( folderName  + "_nether", World.Environment.NETHER);
                                break;
                            case -13: //THE_END
                                createWorldWithEnvironment(folderName + "_the_end", World.Environment.THE_END);
                                break;
                            case -11:
                            default: //NORMAL
                                createWorldWithEnvironment(folderName, World.Environment.NORMAL);
                                break;
                        }
                    }

                    sender.sendMessage(ChatColor.GREEN + "World " + worldName + " created.");

                    // ADD WORLD TO CONFIG.YML
                    List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
                    if (createAll) {
                        if (!worlds.contains(folderName + "/" + worldName)) {
                            worlds.add(folderName + "/" + worldName);
                        }
                        if (!worlds.contains(folderName + "/" + worldName + "_nether")) {
                            worlds.add(folderName + "/" + worldName + "_nether");
                        }
                        if (!worlds.contains(folderName + "/" + worldName + "_the_end")) {
                            worlds.add(folderName + "/" + worldName + "_the_end");
                        }
                    } else {
                        switch (worldType) {
                            case -12: //NETHER
                                worlds.contains(folderName + "_nether");
                                worlds.add(folderName + "_nether");
                                break;
                            case -13: //THE_END
                                worlds.contains(folderName + "_the_end");
                                worlds.add(folderName + "_the_end");
                                break;
                            case -11:  //NORMAL
                            default:  //NORMAL
                                worlds.contains(folderName);
                                worlds.add(folderName);
                                break;
                        }
                    }
                    CoManyWorld.getInstance().getConfig().set("worlds", worlds);
                    CoManyWorld.getInstance().saveConfig();
                    return;
                }
                progress += 10;
                progressBar.setProgress(progress / 100.0);
            }
        }.runTaskTimer(CoManyWorld.getInstance(), 0, 10); // Update every second (20 ticks)
    }

    private void createWorldWithEnvironment(String worldName, World.Environment environment) {
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(environment);
        Bukkit.createWorld(creator);
    }



    private void teleportPlayer(Player player, String worldName) {
        String folderName = getFolderName(worldName);
        World world = Bukkit.getWorld(folderName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World " + ChatColor.WHITE + worldName + ChatColor.RED + " does not exist.");
            return;
        }
        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.WHITE + worldName + ChatColor.GREEN + ".");
    }

    private void listWorlds(CommandSender sender) {
        StringBuilder worldList = new StringBuilder(ChatColor.GOLD + "=== Many World > List\n" + ChatColor.RESET);
        for (World world : Bukkit.getWorlds()) {
            String worldName = getWorldDisplayName(world.getName());
            String worldType = getWorldType(world.getEnvironment());
            worldList.append(ChatColor.YELLOW + "- " + ChatColor.WHITE + worldName + ChatColor.GRAY + " [" + ChatColor.GREEN + worldType + ChatColor.GRAY + "]\n");
        }
        sender.sendMessage(worldList.toString());
    }

    private String getWorldType(World.Environment environment) {
        switch (environment) {
            case NETHER:
                return "Nether";
            case THE_END:
                return "The End";
            case NORMAL:
            default:
                return "World";
        }
    }

    private void deleteWorld(CommandSender sender, String worldName) {
        String folderName = getFolderName(worldName);
        World world = Bukkit.getWorld(folderName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World " + ChatColor.WHITE + worldName + ChatColor.RED + " does not exist.");
            return;
        }
        Bukkit.unloadWorld(world, false);
        sender.sendMessage(ChatColor.GREEN + "World " + ChatColor.WHITE + worldName + ChatColor.GREEN + " deleted.");

        // Remove world from config and save
        List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
        if (worlds.contains(worldName)) {
            worlds.remove(worldName);
            CoManyWorld.getInstance().getConfig().set("worlds", worlds);
            CoManyWorld.getInstance().saveConfig();
        }
    }








    // การแสดงผลชื่อโลก ?
    private String getFolderName(String worldName) {
        if (worldName.equals("world") || worldName.equals("world_nether") || worldName.equals("world_the_end")) {
            return worldName;
        }
        return "world_many_" + worldName;
    }
    private String getWorldDisplayName(String folderName) {
        if (folderName.startsWith("world_many_")) {
            return folderName.substring("world_many_".length());
        }
        return folderName;
    }
}
