package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldManager implements Listener {

    public WorldManager() {
        // Register this class as an event listener
        Bukkit.getServer().getPluginManager().registerEvents(this, JavaPlugin.getPlugin(CoManyWorld.class));
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
}
