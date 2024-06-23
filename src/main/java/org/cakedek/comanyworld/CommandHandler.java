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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.cakedek.comanyworld.cm.ListWorlds;
import org.cakedek.comanyworld.cm.WorldDelete;
import org.cakedek.comanyworld.cm.WorldImport;

import java.util.List;

public class CommandHandler implements CommandExecutor {

    // COMMAND IN
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String se =     ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                        ChatColor.YELLOW + "/co-many create [worldName] [World] " + ChatColor.RESET + ">> Create a new world\n" +
                        ChatColor.YELLOW + "/co-many list " + ChatColor.RESET + ">> world list \n" +
                        ChatColor.YELLOW + "/co-many tp [worldName] " + ChatColor.RESET + ">> tp to world\n" +
                        ChatColor.YELLOW + "/co-many import [worldName] " + ChatColor.RESET + ">> Import World \n" +
                        ChatColor.YELLOW + "/co-many delete [worldName] " + ChatColor.RESET + ">> Delete world\n" +
                        ChatColor.YELLOW + "/co-many about " + ChatColor.RESET + ">> About \n";
        if (args.length == 0) {
            sender.sendMessage(se);
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
                ///////////////////////////////////////////////////////////////////////////////////////////////
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
                //////////////////////////////////////////////////////////////////////////////////////////////
                sender.sendMessage(     ChatColor.GOLD + "=== Many World > Teleport "+ ChatColor.RESET +
                                        "\nCommand: /co-many tp <worldName>");
                return true;
            }
            String worldName = args[1];
            teleportPlayer((Player) sender, worldName);
        }
        //////  LIST
        else if (args[0].equalsIgnoreCase("list")) {
            ListWorlds.listWorlds(sender);
        }
        //////  DELETE
        else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.GOLD +  "=== Many World > Delete World "+ ChatColor.RESET +
                                                        "\nCommand: /co-many delete <worldName>");
                return true;
            }
            String worldName = args[1];
            WorldDelete.deleteWorlds(sender, worldName);
        }
        //////  IMPORT
        else if (args[0].equalsIgnoreCase("import")) {
            if (args.length == 2) {
                String worldName = args[1];
                WorldImport.importWorld(sender ,worldName);
            } else {
                //////////////////////////////////////////////////////////////////////////////////////////////
                sender.sendMessage(     ChatColor.GOLD + "=== Many World > Import World "+ ChatColor.RESET +
                                                            "\nCommand: /co-many import <worldName>");
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
            sender.sendMessage(se);
        }
        return true;
    }

    ////// CREATE WORLD
    private void createWorld(CommandSender sender, String worldName, boolean createAll, int worldType) {
        String folderName = "many_world/" + worldName;
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
                        createWorldWithEnvironment(folderName + "/world", World.Environment.NORMAL);
                        createWorldWithEnvironment(folderName + "/world_nether", World.Environment.NETHER);
                        createWorldWithEnvironment(folderName + "/world_the_end", World.Environment.THE_END);
                    } else {
                        switch (worldType) {
                            case -12: // NETHER
                                createWorldWithEnvironment(folderName + "_nether", World.Environment.NETHER);
                                break;
                            case -13: // THE_END
                                createWorldWithEnvironment(folderName + "_the_end", World.Environment.THE_END);
                                break;
                            case -11:
                            default: // NORMAL
                                createWorldWithEnvironment(folderName, World.Environment.NORMAL);
                                break;
                        }
                    }

                    sender.sendMessage(ChatColor.GREEN + "World " + worldName + " created.");

                    // ADD WORLD TO CONFIG.YML
                    List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
                    if (createAll) {
                        if (!worlds.contains(folderName + "/world")) {
                            worlds.add(folderName + "/world");
                        }
                        if (!worlds.contains(folderName + "/world_nether")) {
                            worlds.add(folderName + "/world_nether");
                        }
                        if (!worlds.contains(folderName + "/world_the_end")) {
                            worlds.add(folderName + "/world_the_end");
                        }
                    } else {
                        switch (worldType) {
                            case -12: // NETHER
                                if (!worlds.contains(folderName + "_nether")) {
                                    worlds.add(folderName + "_nether");
                                }
                                break;
                            case -13: // THE_END
                                if (!worlds.contains(folderName + "_the_end")) {
                                    worlds.add(folderName + "_the_end");
                                }
                                break;
                            case -11:  // NORMAL
                            default:  // NORMAL
                                if (!worlds.contains(folderName)) {
                                    worlds.add(folderName);
                                }
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
        }.runTaskTimer(CoManyWorld.getInstance(), 0, 20); // Update every second (20 ticks)
    }

    private void createWorldWithEnvironment(String worldName, World.Environment environment) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(environment);
        Bukkit.createWorld(worldCreator);
    }

    // วาร์ปผู้เล่นไปยังโลกที่สร้างไว่้
    private void teleportPlayer(Player player, String worldName) {
        World world = Bukkit.getWorld((worldName));
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World " + ChatColor.WHITE + worldName + ChatColor.RED + " does not exist.");
            return;
        }
        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.WHITE + worldName + ChatColor.GREEN + ".");
    }
}
