package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorldDelete implements Listener {

    public static void deleteWorlds(CommandSender sender, String worldPath) {
        List<String> worldsToDelete = new ArrayList<>();

        // Check if it's a directory (potential parent world)
        if (worldPath.endsWith("/")) {
            worldsToDelete = getWorldsInDirectory(worldPath);
        } else {
            World world = Bukkit.getWorld(worldPath);
            if (world != null) {
                worldsToDelete.add(worldPath);
            } else {
                sender.sendMessage("World not found: " + worldPath);
                return;
            }
        }

        if (worldsToDelete.isEmpty()) {
            sender.sendMessage("No worlds found to delete in: " + worldPath);
            return;
        }

        // Unload worlds
        for (String worldName : worldsToDelete) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }
        }

        // Remove from config.yml
        List<String> configWorlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
        configWorlds.removeAll(worldsToDelete);
        CoManyWorld.getInstance().getConfig().set("worlds", configWorlds);
        CoManyWorld.getInstance().saveConfig();

        // Add to delete_worlds.yml
        File deleteWorldsFile = new File(CoManyWorld.getInstance().getDataFolder(), "delete_worlds.yml");
        FileConfiguration deleteWorldsConfig = YamlConfiguration.loadConfiguration(deleteWorldsFile);
        List<String> deleteWorlds = deleteWorldsConfig.getStringList("delete_worlds");
        deleteWorlds.addAll(worldsToDelete);
        deleteWorldsConfig.set("delete_worlds", deleteWorlds);
        try {
            deleteWorldsConfig.save(deleteWorldsFile);
        } catch (IOException e) {
            e.printStackTrace();
            sender.sendMessage("Error saving delete_worlds.yml");
            return;
        }

        sender.sendMessage("The following worlds have been marked for deletion:");
        for (String world : worldsToDelete) {
            sender.sendMessage("- " + world);
        }
        sender.sendMessage("Use /co-many-clear to permanently delete these worlds.");
    }

    private static List<String> getWorldsInDirectory(String directoryPath) {
        List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
        return worlds.stream()
                .filter(world -> world.startsWith(directoryPath))
                .collect(Collectors.toList());
    }
}