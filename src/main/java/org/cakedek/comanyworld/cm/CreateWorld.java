package org.cakedek.comanyworld.cm;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CreateWorld {
    private static final List<String> RESERVED_WORLD_NAMES = Arrays.asList("world", "world_nether", "world_the_end");

    ////// CREATE WORLD
    public static void createWorld(CommandSender sender, String worldName, boolean createAll, int worldType) {
        String folderName = "many_world/" + worldName;

        // Check if the world already exists
        if (isReservedWorldName(worldName) || worldExists(folderName)) {
            sender.sendMessage(ChatColor.RED + "This world name is reserved or already exists. Please choose a different name.");
            return;
        }

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
                            case -11:
                            default: // NORMAL
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

    private static boolean worldExists(String worldName) {
        // Check if world folder exists
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            return true;
        }

        // Check if world is loaded
        return Bukkit.getWorld(worldName) != null;
    }

    private static boolean isReservedWorldName(String worldName) {
        return RESERVED_WORLD_NAMES.contains(worldName.toLowerCase());
    }

    private static void createWorldWithEnvironment(String worldName, World.Environment environment) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(environment);
        Bukkit.createWorld(worldCreator);
    }
}
