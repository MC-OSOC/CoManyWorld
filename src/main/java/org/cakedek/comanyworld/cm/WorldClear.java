package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.cakedek.comanyworld.CoManyWorld;
import org.cakedek.comanyworld.WorldNames;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;

public class WorldClear {

    private static boolean running = false;

    /**
     * Without confirm: lists the worlds in the trash.
     * With confirm: permanently deletes exactly those world folders (in the background).
     */
    public static void clearDeletedWorlds(CommandSender sender, boolean confirm) {
        CoManyWorld plugin = CoManyWorld.getInstance();
        File deleteWorldsFile = new File(plugin.getDataFolder(), "delete_worlds.yml");
        FileConfiguration deleteWorldsConfig = YamlConfiguration.loadConfiguration(deleteWorldsFile);
        List<String> deleteWorlds = deleteWorldsConfig.getStringList("delete_worlds");

        if (deleteWorlds.isEmpty()) {
            sender.sendMessage("There are no worlds to clear.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Many World > Clear \n" + ChatColor.RESET +
                (confirm ? "Permanently deleting:" : "The following worlds will be permanently deleted:"));
        for (String world : deleteWorlds) {
            sender.sendMessage("- " + world);
        }
        if (!confirm) {
            sender.sendMessage(ChatColor.YELLOW + "Run " + ChatColor.WHITE + "/co-many clear confirm"
                    + ChatColor.YELLOW + " to delete them. This cannot be undone.");
            return;
        }
        if (running) {
            sender.sendMessage(ChatColor.RED + "A clear is already running.");
            return;
        }

        // Decide on the main thread what is safe to delete.
        Map<String, File> folders = new LinkedHashMap<>();
        List<String> keep = new ArrayList<>();
        for (String worldName : deleteWorlds) {
            File folder = WorldNames.managedFolder(worldName);
            if (folder == null) {
                sender.sendMessage(ChatColor.RED + "Skipped " + worldName + ": not inside " + WorldNames.ROOT);
                keep.add(worldName);
            } else if (Bukkit.getWorld(worldName) != null) {
                sender.sendMessage(ChatColor.RED + "Skipped " + worldName + ": the world is loaded again.");
                keep.add(worldName);
            } else {
                folders.put(worldName, folder);
            }
        }

        running = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Boolean> results = new LinkedHashMap<>();
            for (Map.Entry<String, File> entry : folders.entrySet()) {
                results.put(entry.getKey(), deleteRecursively(entry.getValue().toPath()));
                deleteParentIfEmpty(entry.getValue());
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                running = false;
                for (Map.Entry<String, Boolean> result : results.entrySet()) {
                    if (result.getValue()) {
                        sender.sendMessage("Deleted world folder: " + result.getKey());
                    } else {
                        sender.sendMessage("Failed to delete world folder: " + result.getKey());
                        keep.add(result.getKey());
                    }
                }
                // Keep anything that could not be deleted in delete_worlds.yml
                deleteWorldsConfig.set("delete_worlds", keep);
                try {
                    deleteWorldsConfig.save(deleteWorldsFile);
                } catch (IOException e) {
                    sender.sendMessage("Failed to update delete_worlds.yml");
                    plugin.getLogger().log(Level.SEVERE, "Failed to update delete_worlds.yml", e);
                }
            });
        });
    }

    private static boolean deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return true;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (IOException | RuntimeException e) {
            CoManyWorld.getInstance().getLogger().log(Level.WARNING, "Failed to delete " + root, e);
            return false;
        }
    }

    /** Removes many_world/<name>/ once all of its worlds are gone. */
    private static void deleteParentIfEmpty(File worldFolder) {
        File parent = worldFolder.getParentFile();
        File root = new File(Bukkit.getWorldContainer(), WorldNames.ROOT);
        try {
            if (parent != null && !parent.getCanonicalFile().equals(root.getCanonicalFile())) {
                String[] remaining = parent.list();
                if (remaining != null && remaining.length == 0) {
                    Files.deleteIfExists(parent.toPath());
                }
            }
        } catch (IOException ignored) {
            // Leaving an empty folder behind is harmless.
        }
    }
}
