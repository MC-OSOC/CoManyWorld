package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldManager implements Listener {

    private final JavaPlugin plugin;
    private final String defaultWorldName;

    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Load the default world name from the config
        this.defaultWorldName = plugin.getConfig().getString("default-world", "world");
        // Register this class as an event listener
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        Bukkit.getLogger().info("World loaded: " + world.getName());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        Bukkit.getLogger().info("World unloaded: " + world.getName());
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        World fromWorld = event.getFrom().getWorld();
        String fromWorldName = fromWorld.getName();

        // Extract base world path
        String baseWorldPath = fromWorldName.substring(0, fromWorldName.lastIndexOf('/') + 1);
        // Extract base world name
        String baseWorldName = fromWorldName.substring(fromWorldName.lastIndexOf('/') + 1);

        World toWorld = null;

        if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
            if (event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL) {
                // If traveling through an end portal
                toWorld = Bukkit.getWorld(baseWorldPath + "world_the_end");
                if (toWorld != null) {
                    // Set custom spawn location in The End
                    int highestY = toWorld.getHighestBlockYAt(100, 0); // Replace with your desired coordinates X and Z
                    Location customEndLocation = new Location(toWorld, 100, highestY + 2, 0); // Spawn 2 blocks above the highest block
                    event.setTo(customEndLocation);
                } else {
                    event.setTo(getDefaultWorld().getSpawnLocation());
                }
            } else {
                // If traveling through a nether portal
                toWorld = Bukkit.getWorld(baseWorldPath + "world_nether");
                if (toWorld != null) {
                    Location toLocation = calculateNetherLocation(event.getFrom(), toWorld);
                    event.setTo(toLocation);
                } else {
                    event.setTo(getDefaultWorld().getSpawnLocation());
                }
            }
        } else if (fromWorld.getEnvironment() == World.Environment.NETHER) {
            toWorld = Bukkit.getWorld(baseWorldPath + "world");
            if (toWorld != null) {
                Location toLocation = calculateOverworldLocation(event.getFrom(), toWorld);
                event.setTo(toLocation);
            } else {
                event.setTo(getDefaultWorld().getSpawnLocation());
            }
        } else if (fromWorld.getEnvironment() == World.Environment.THE_END) {
            toWorld = Bukkit.getWorld(baseWorldPath + "world");
            if (toWorld != null) {
                event.setTo(toWorld.getSpawnLocation());
            } else {
                event.setTo(getDefaultWorld().getSpawnLocation());
            }
        }
    }

    private World getDefaultWorld() {
        World defaultWorld = Bukkit.getWorld(defaultWorldName);
        if (defaultWorld == null) {
            // Fallback to "world" if the configured default world does not exist
            defaultWorld = Bukkit.getWorld("world");
        }
        return defaultWorld;
    }

    private Location calculateNetherLocation(Location fromLocation, World toWorld) {
        // Convert Overworld coordinates to Nether coordinates
        double toX = fromLocation.getX() / 8.0;
        double toZ = fromLocation.getZ() / 8.0;
        int highestY = toWorld.getHighestBlockYAt((int) toX, (int) toZ);
        return new Location(toWorld, toX, highestY + 2, toZ);
    }

    private Location calculateOverworldLocation(Location fromLocation, World toWorld) {
        // Convert Nether coordinates to Overworld coordinates
        double toX = fromLocation.getX() * 8.0;
        double toZ = fromLocation.getZ() * 8.0;
        int highestY = toWorld.getHighestBlockYAt((int) toX, (int) toZ);
        return new Location(toWorld, toX, highestY + 2, toZ);
    }
}
