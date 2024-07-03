package org.cakedek.comanyworld.cm;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings({"ResultOfMethodCallIgnored", "CallToPrintStackTrace"})
public class WorldClear {

    public static void clearDeletedWorlds(CommandSender sender) {
        File deleteWorldsFile = new File(CoManyWorld.getInstance().getDataFolder(), "delete_worlds.yml");
        FileConfiguration deleteWorldsConfig = YamlConfiguration.loadConfiguration(deleteWorldsFile);
        List<String> deleteWorlds = deleteWorldsConfig.getStringList("delete_worlds");

        if (deleteWorlds.isEmpty()) {
            sender.sendMessage("There are no worlds to clear.");
            return;
        }

        Map<String, List<String>> worldGroups = groupWorlds(deleteWorlds);

        sender.sendMessage(   ChatColor.GOLD + "=== Many World > Clear \n" + ChatColor.RESET +
                                "The following worlds will be permanently deleted:");
        for (Map.Entry<String, List<String>> entry : worldGroups.entrySet()) {
            sender.sendMessage("- " + entry.getKey() + " (including: " + String.join(", ", entry.getValue()) + ")");
        }

        // Here you would typically wait for user input.
        // For the sake of this example, let's assume the confirmation is received.
        // In a real implementation, you'd need to handle this asynchronously.

        // Simulating user confirmation

        for (String mainFolder : worldGroups.keySet()) {
            File worldFolder = new File(Bukkit.getWorldContainer(), mainFolder);
            if (deleteWorldFolder(worldFolder)) {
                sender.sendMessage("Deleted world folder: " + mainFolder);
            } else {
                sender.sendMessage("Failed to delete world folder: " + mainFolder);
            }
        }

        // Clear the delete_worlds.yml file
        deleteWorldsConfig.set("delete_worlds", null);
        try {
            deleteWorldsConfig.save(deleteWorldsFile);
        } catch (IOException e) {
            sender.sendMessage("Failed to update delete_worlds.yml");
            e.printStackTrace();
        }
    }

    private static Map<String, List<String>> groupWorlds(List<String> worlds) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String world : worlds) {
            String[] parts = world.split("/");
            if (parts.length > 2) {
                String mainFolder = parts[0] + "/" + parts[1];
                groups.computeIfAbsent(mainFolder, k -> new ArrayList<>()).add(world);
            } else {
                groups.put(world, Collections.singletonList(world));
            }
        }
        return groups;
    }

    private static boolean deleteWorldFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorldFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return folder.delete();
    }
}