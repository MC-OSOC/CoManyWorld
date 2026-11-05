package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.cakedek.comanyworld.CoManyWorld;
import org.cakedek.comanyworld.WorldNames;
import org.cakedek.comanyworld.WorldOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public class WorldDelete {

    /**
     * Unloads the world(s) and moves them to the trash (delete_worlds.yml).
     * Nothing is removed from disk until /co-many clear confirm.
     * Only worlds inside many_world/ can be deleted, never the server's own worlds.
     */
    public static void deleteWorlds(CommandSender sender, String worldPath) {
        if (!WorldNames.isManaged(worldPath) || worldPath.contains("..") || worldPath.equalsIgnoreCase(WorldNames.ROOT)) {
            sender.sendMessage(ChatColor.RED + "Only worlds created or imported by CoManyWorld (" + WorldNames.ROOT
                    + "<name>/...) can be deleted.");
            return;
        }

        List<String> configWorlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
        Set<String> worldsToDelete = new LinkedHashSet<>();

        if (worldPath.endsWith("/")) {
            // A whole group, e.g. many_world/foo/
            String prefix = worldPath.toLowerCase(Locale.ROOT);
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    worldsToDelete.add(world.getName());
                }
            }
            for (String name : configWorlds) {
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    worldsToDelete.add(name);
                }
            }
        } else {
            World world = Bukkit.getWorld(worldPath);
            if (world != null) {
                worldsToDelete.add(world.getName());
            } else {
                configWorlds.stream().filter(name -> name.equalsIgnoreCase(worldPath)).forEach(worldsToDelete::add);
            }
        }

        if (worldsToDelete.isEmpty()) {
            sender.sendMessage("World not found: " + worldPath);
            return;
        }

        // Unload worlds. Players are moved out first, otherwise the server refuses to unload.
        World fallback = Bukkit.getWorlds().get(0);
        List<String> failed = new ArrayList<>();
        for (String worldName : worldsToDelete) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            for (Player player : world.getPlayers()) {
                player.teleport(fallback.getSpawnLocation());
                player.sendMessage(ChatColor.YELLOW + "The world you were in is being deleted.");
            }
            // Save on unload: the world goes to the trash, it can still be restored until it is cleared.
            if (!Bukkit.unloadWorld(world, true)) {
                failed.add(worldName);
            }
        }
        if (!failed.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Could not unload " + String.join(", ", failed)
                    + ". Nothing was deleted.");
            return;
        }

        // Remove from config.yml
        configWorlds.removeIf(name -> worldsToDelete.stream().anyMatch(name::equalsIgnoreCase));
        CoManyWorld.getInstance().getConfig().set("worlds", configWorlds);
        worldsToDelete.forEach(name -> WorldOptions.remove(CoManyWorld.getInstance().getConfig(), name));
        CoManyWorld.getInstance().saveConfig();

        // Add to delete_worlds.yml
        File deleteWorldsFile = new File(CoManyWorld.getInstance().getDataFolder(), "delete_worlds.yml");
        FileConfiguration deleteWorldsConfig = YamlConfiguration.loadConfiguration(deleteWorldsFile);
        List<String> deleteWorlds = deleteWorldsConfig.getStringList("delete_worlds");
        for (String world : worldsToDelete) {
            if (!deleteWorlds.contains(world)) {
                deleteWorlds.add(world);
            }
        }
        deleteWorldsConfig.set("delete_worlds", deleteWorlds);
        try {
            deleteWorldsConfig.save(deleteWorldsFile);
        } catch (IOException e) {
            CoManyWorld.getInstance().getLogger().log(Level.SEVERE, "Error saving delete_worlds.yml", e);
            sender.sendMessage("Error saving delete_worlds.yml");
            return;
        }

        sender.sendMessage("The following worlds have been marked for deletion:");
        for (String world : worldsToDelete) {
            sender.sendMessage("- " + world);
        }
        sender.sendMessage("Use /co-many clear confirm to permanently delete these worlds.");
    }
}
