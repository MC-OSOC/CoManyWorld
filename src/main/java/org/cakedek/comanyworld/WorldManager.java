package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldManager implements Listener {

    private final JavaPlugin plugin;
    private final String defaultWorldName;
    private final String mainNetherWorld;
    private final String mainEndWorld;

    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defaultWorldName = plugin.getConfig().getString("default-world", "world");
        this.mainNetherWorld = plugin.getConfig().getString("default-nether-world", "world_nether");
        this.mainEndWorld = plugin.getConfig().getString("default-end-world", "world_the_end");
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
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }
        World playerWorld = event.getPlayer().getWorld();
        if (playerWorld.getName().startsWith("many_world/")) {
            event.setRespawnLocation(playerWorld.getSpawnLocation());
        } else {
            event.setRespawnLocation(getDefaultWorld().getSpawnLocation());
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        World fromWorld = event.getFrom().getWorld();
        String fromWorldName = fromWorld.getName();

        // Extract base world path and name
        String baseWorldPath = fromWorldName.contains("/") ? fromWorldName.substring(0, fromWorldName.lastIndexOf('/') + 1) : "";
        String baseWorldName = fromWorldName.contains("/") ? fromWorldName.substring(fromWorldName.lastIndexOf('/') + 1) : fromWorldName;

        World toWorld = null;

        if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
            if (event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL) {
                // If traveling through an end portal
                toWorld = Bukkit.getWorld(baseWorldPath + "world_the_end");
                if (toWorld == null) {
                    // If custom End world doesn't exist, use main End world
                    toWorld = Bukkit.getWorld(mainEndWorld);
                }
                if (toWorld != null) {
                    int highestY = toWorld.getHighestBlockYAt(100, 0);
                    Location customEndLocation = new Location(toWorld, 200, highestY + 2, 0);
                    event.setTo(customEndLocation);
                } else {
                    event.setTo(getDefaultWorld().getSpawnLocation());
                }
            } else {
                // If traveling through a nether portal
                toWorld = Bukkit.getWorld(baseWorldPath + "world_nether");
                if (toWorld == null) {
                    // If custom Nether world doesn't exist, use main Nether world
                    toWorld = Bukkit.getWorld(mainNetherWorld);
                }
                if (toWorld != null) {
                    Location toLocation = calculateNetherLocation(event.getFrom(), toWorld);
                    event.setTo(toLocation);
                } else {
                    event.setTo(getDefaultWorld().getSpawnLocation());
                }
            }
        } else if (fromWorld.getEnvironment() == World.Environment.NETHER) {
            // When returning from Nether, try to find the corresponding overworld
            toWorld = Bukkit.getWorld(baseWorldPath + "world");
            if (toWorld == null) {
                // If corresponding overworld doesn't exist, use default world
                toWorld = getDefaultWorld();
            }
            if (toWorld != null) {
                Location toLocation = calculateOverworldLocation(event.getFrom(), toWorld);
                event.setTo(toLocation);
            } else {
                event.setTo(getDefaultWorld().getSpawnLocation());
            }
        } else if (fromWorld.getEnvironment() == World.Environment.THE_END) {
            // When returning from The End, try to find the corresponding overworld
            toWorld = Bukkit.getWorld(baseWorldPath + "world");
            if (toWorld == null) {
                // If corresponding overworld doesn't exist, use default world
                toWorld = getDefaultWorld();
            }
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