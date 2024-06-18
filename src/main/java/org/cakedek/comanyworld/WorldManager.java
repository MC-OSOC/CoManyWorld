package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldManager implements Listener {

    private final JavaPlugin plugin;

    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
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

        if (fromWorldName.contains("/")) {
            String baseWorldPath = fromWorldName.substring(0, fromWorldName.lastIndexOf('/'));
            String baseWorldName = fromWorldName.substring(fromWorldName.lastIndexOf('/') + 1).replaceFirst("_nether", "").replaceFirst("_the_end", "");
            World toWorld;

            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                toWorld = Bukkit.getWorld(baseWorldPath + "/" + baseWorldName + "_nether");
                if (toWorld != null) {
                    event.setTo(toWorld.getSpawnLocation());
                }
            } else if (fromWorld.getEnvironment() == World.Environment.NETHER) {
                toWorld = Bukkit.getWorld(baseWorldPath + "/" + baseWorldName);
                if (toWorld != null) {
                    event.setTo(toWorld.getSpawnLocation());
                }
            } else if (fromWorld.getEnvironment() == World.Environment.THE_END) {
                toWorld = Bukkit.getWorld(baseWorldPath + "/" + baseWorldName + "_the_end");
                if (toWorld != null) {
                    event.setTo(toWorld.getSpawnLocation());
                }
            }
        }
    }
}
