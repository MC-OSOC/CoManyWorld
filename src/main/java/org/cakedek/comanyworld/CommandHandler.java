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

import java.util.List;

public class CommandHandler implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                    "/co-many create [worldName] [World]  >> Create a new world\n" +
                    "/co-many list >> world list \n" +
                    "/co-many tp [worldName] >> tp to world\n" +
                    "/co-many del [worldName] >> Delete world\n" +
                    "/co-many about >> About \n");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.GOLD+"=== Many World > Create "+ ChatColor.RESET +"\n-11 [WORLD] \n-12 [NETHER] \n-13 [THE_END] \nCommand: /co-many create <worldName> [-11|-12|-13]");
                return true;
            }
            String worldName = args[1];
            int worldType = args.length > 2 ? Integer.parseInt(args[2]) : -11;
            createWorld(sender, worldName, worldType);
        } else if (args[0].equalsIgnoreCase("tp")) {
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
        } else if (args[0].equalsIgnoreCase("list")) {
            listWorlds(sender);
        } else if (args[0].equalsIgnoreCase("del")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /co-many del <worldName>");
                return true;
            }
            String worldName = args[1];
            deleteWorld(sender, worldName);
        } else if (args[0].equalsIgnoreCase("about")) {
            co_about(sender);
        }
        else {
            sender.sendMessage(ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                                "/co-many create [worldName] [World]  >> Create a new world\n" +
                                "/co-many list >> world list \n" +
                                "/co-many tp [worldName] >> tp to world\n" +
                                "/co-many del [worldName] >> Delete world\n" +
                                "/co-many about >> About \n");
        }
        return true;
    }

    private void createWorld(CommandSender sender, String worldName, int worldType) {
        String folderName = "world-many-" + worldName;
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
                    WorldCreator creator = new WorldCreator(folderName);
                    switch (worldType) {
                        case -12:
                            creator.environment(World.Environment.NETHER);
                            break;
                        case -13:
                            creator.environment(World.Environment.THE_END);
                            break;
                        case -11:
                        default:
                            creator.environment(World.Environment.NORMAL);
                            break;
                    }
                    Bukkit.createWorld(creator);
                    sender.sendMessage(ChatColor.GREEN + "World " + worldName + " created.");

                    // Add world to config and save
                    List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
                    if (!worlds.contains(worldName)) {
                        worlds.add(worldName);
                        CoManyWorld.getInstance().getConfig().set("worlds", worlds);
                        CoManyWorld.getInstance().saveConfig();
                    }
                    return;
                }
                progress += 10;
                progressBar.setProgress(progress / 100.0);
            }
        }.runTaskTimer(CoManyWorld.getInstance(), 0, 20); // Update every second (20 ticks)
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









    private String getFolderName(String worldName) {
        if (worldName.equals("world") || worldName.equals("world_nether") || worldName.equals("world_the_end")) {
            return worldName;
        }
        return "world-many-" + worldName;
    }

    private String getWorldDisplayName(String folderName) {
        if (folderName.startsWith("world-many-")) {
            return folderName.substring("world-many-".length());
        }
        return folderName;
    }

    //สว่นเกี่ยวกับ
    private void co_about(CommandSender sender) {
        String aboutInfo = ChatColor.GOLD  + "=== Many World List > About \n" + ChatColor.RESET +
                "Version >>> " + CoManyWorld.getInstance().getDescription().getVersion() + "\n" +
                "Minecraft Server Version >>> " + Bukkit.getVersion() + "\n" +
                "->->->\n" +
                "MC-OSOC \n" +
                "github >>> https://github.com/MC-OSOC\n" +
                "*** Some of the code was developed by AI.\n" +
                "===============================";
        sender.sendMessage(aboutInfo);
    }
}
