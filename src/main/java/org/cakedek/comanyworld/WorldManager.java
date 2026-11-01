package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldManager implements Listener {

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
        plugin.getLogger().info("World loaded: " + event.getWorld().getName());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.getLogger().info("World unloaded: " + event.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }
        World playerWorld = event.getPlayer().getWorld();
        if (WorldNames.isManaged(playerWorld.getName())) {
            // Died in (or left the End of) a CoManyWorld world: respawn in that world group's overworld
            // when there is one, otherwise at the spawn of the world they were in.
            World overworld = WorldNames.overworldOf(WorldNames.groupPrefix(playerWorld.getName()));
            if (overworld != null) {
                event.setRespawnLocation(overworld.getSpawnLocation());
            } else if (playerWorld.getEnvironment() != World.Environment.THE_END) {
                event.setRespawnLocation(playerWorld.getSpawnLocation());
            } else {
                World fallback = getDefaultWorld();
                if (fallback != null) {
                    event.setRespawnLocation(fallback.getSpawnLocation());
                }
            }
        } else {
            World defaultWorld = getDefaultWorld();
            if (defaultWorld != null) {
                event.setRespawnLocation(defaultWorld.getSpawnLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        World fromWorld = event.getFrom().getWorld();
        if (fromWorld == null || !WorldNames.isManaged(fromWorld.getName())) {
            // Portals in the server's own worlds keep their vanilla behaviour.
            return;
        }
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
                && cause != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return; // e.g. end gateways inside the End
        }

        String prefix = WorldNames.groupPrefix(fromWorld.getName());
        World overworld = WorldNames.overworldOf(prefix);
        World nether = WorldNames.netherOf(prefix);
        World end = WorldNames.endOf(prefix);

        if (plugin.getConfig().getBoolean("prevent-single-world-portals", true)
                && (overworld == null || nether == null || end == null)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Portal travel is disabled in single worlds.");
            return;
        }

        World.Environment environment = fromWorld.getEnvironment();
        if (environment == World.Environment.NORMAL) {
            if (cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                World toWorld = end != null ? end : Bukkit.getWorld(mainEndWorld);
                if (toWorld != null) {
                    event.setTo(endPlatform(toWorld));
                } else {
                    cancel(event, "There is no End world to travel to.");
                }
            } else {
                World toWorld = nether != null ? nether : Bukkit.getWorld(mainNetherWorld);
                if (toWorld != null) {
                    event.setTo(scaled(event.getFrom(), toWorld, 1.0 / 8.0));
                } else {
                    cancel(event, "There is no Nether world to travel to.");
                }
            }
        } else if (environment == World.Environment.NETHER) {
            // When returning from Nether, try to find the corresponding overworld
            World toWorld = overworld != null ? overworld : getDefaultWorld();
            if (toWorld != null) {
                event.setTo(scaled(event.getFrom(), toWorld, 8.0));
            } else {
                cancel(event, "There is no overworld to travel to.");
            }
        } else if (environment == World.Environment.THE_END) {
            // When returning from The End, try to find the corresponding overworld
            World toWorld = overworld != null ? overworld : getDefaultWorld();
            if (toWorld != null) {
                event.setTo(toWorld.getSpawnLocation());
            } else {
                cancel(event, "There is no overworld to travel to.");
            }
        }
    }

    private static void cancel(PlayerPortalEvent event, String message) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(message);
    }

    private World getDefaultWorld() {
        World defaultWorld = Bukkit.getWorld(defaultWorldName);
        if (defaultWorld == null) {
            // Fallback to the server's main world if the configured default world does not exist
            defaultWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        }
        return defaultWorld;
    }

    /**
     * Nether <-> overworld coordinates. The server then searches for (or builds) a portal near this
     * location, like vanilla does, so the Y only needs to be inside the usable height of the target world.
     */
    private static Location scaled(Location from, World toWorld, double scale) {
        double x = from.getX() * scale;
        double z = from.getZ() * scale;
        int minY = toWorld.getMinHeight() + 1;
        // Nether: stay below the bedrock roof (logical height 128)
        int maxY = toWorld.getEnvironment() == World.Environment.NETHER
                ? Math.min(toWorld.getMaxHeight(), toWorld.getMinHeight() + 128) - 8
                : toWorld.getMaxHeight() - 2;
        double y = Math.max(minY, Math.min(maxY, from.getY()));
        return new Location(toWorld, x, y, z, from.getYaw(), from.getPitch());
    }

    /**
     * Vanilla End arrival point (100, 49, 0) with the 5x5 obsidian platform under it,
     * so players never land over the void in a freshly generated End.
     */
    private static Location endPlatform(World end) {
        int px = 100;
        int py = 49;
        int pz = 0;
        for (int x = px - 2; x <= px + 2; x++) {
            for (int z = pz - 2; z <= pz + 2; z++) {
                end.getBlockAt(x, py - 1, z).setType(Material.OBSIDIAN, false);
                for (int y = py; y <= py + 2; y++) {
                    end.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
        return new Location(end, px + 0.5, py, pz + 0.5, 90f, 0f);
    }
}
