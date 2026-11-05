package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.cakedek.comanyworld.CoManyWorld;
import org.cakedek.comanyworld.WorldFolders;
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
        // A world can have files in two places (see WorldFolders), both are deleted.
        Map<String, List<File>> folders = new LinkedHashMap<>();
        List<String> keep = new ArrayList<>();
        for (String worldName : deleteWorlds) {
            if (!WorldFolders.isSafeManagedName(worldName)) {
                sender.sendMessage(ChatColor.RED + "Skipped " + worldName + ": not inside " + WorldNames.ROOT);
                keep.add(worldName);
                continue;
            }
            if (Bukkit.getWorld(worldName) != null) {
                sender.sendMessage(ChatColor.RED + "Skipped " + worldName + ": the world is loaded again.");
                keep.add(worldName);
                continue;
            }
            List<File> worldFolders = WorldFolders.managedFolders(worldName);
            World loadedInside = loadedWorldInside(worldFolders);
            if (loadedInside != null) {
                // e.g. a hand-edited trash entry "many_world/foo" while many_world/foo/world is loaded
                sender.sendMessage(ChatColor.RED + "Skipped " + worldName + ": the loaded world "
                        + loadedInside.getName() + " is inside it.");
                keep.add(worldName);
                continue;
            }
            folders.put(worldName, worldFolders);
        }

        if (folders.isEmpty()) {
            // Nothing to delete (all skipped): no need for a background task.
            finish(sender, new LinkedHashMap<>(), keep, deleteWorldsConfig, deleteWorldsFile);
            return;
        }

        // Asked here: the server's world list must only be read on the main thread.
        List<File> roots = WorldFolders.managedRoots();
        running = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Boolean> results = new LinkedHashMap<>();
            for (Map.Entry<String, List<File>> entry : folders.entrySet()) {
                boolean deleted = true;
                for (File folder : entry.getValue()) {
                    deleted &= deleteRecursively(folder.toPath());
                    deleteParentIfEmpty(folder, roots);
                }
                results.put(entry.getKey(), deleted);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                running = false;
                finish(sender, results, keep, deleteWorldsConfig, deleteWorldsFile);
            });
        });
    }

    /** Called when the plugin is disabled, so a clear that was cut off doesn't block the next one. */
    public static void reset() {
        running = false;
    }

    private static void finish(CommandSender sender, Map<String, Boolean> results, List<String> keep,
                               FileConfiguration deleteWorldsConfig, File deleteWorldsFile) {
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
            CoManyWorld.getInstance().getLogger().log(Level.SEVERE, "Failed to update delete_worlds.yml", e);
        }
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

    private static World loadedWorldInside(List<File> folders) {
        for (File folder : folders) {
            Path target = folder.toPath();
            for (World world : Bukkit.getWorlds()) {
                Path worldFolder;
                try {
                    worldFolder = world.getWorldFolder().getCanonicalFile().toPath();
                } catch (IOException e) {
                    worldFolder = world.getWorldFolder().toPath().toAbsolutePath().normalize();
                }
                if (worldFolder.startsWith(target)) {
                    return world;
                }
            }
        }
        return null;
    }

    /** Removes many_world/<name>/ once all of its worlds are gone (never many_world/ itself). */
    private static void deleteParentIfEmpty(File worldFolder, List<File> roots) {
        File parent = worldFolder.getParentFile();
        try {
            if (parent != null && WorldFolders.rootOf(parent, roots) != null) {
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
