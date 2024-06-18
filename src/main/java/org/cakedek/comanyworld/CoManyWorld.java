package org.cakedek.comanyworld;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CoManyWorld extends JavaPlugin {

    private static CoManyWorld instance;
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        // Save default configuration
        saveDefaultConfig();

        // Set the instance
        instance = this;

        // Initialize WorldManager
        worldManager = new WorldManager(this);

        // Register commands and tab completer
        getCommand("co-many").setExecutor(new CommandHandler(worldManager, getConfig(), this));
        getCommand("co-many").setTabCompleter(new CommandCompleter());

        // Register events
        getServer().getPluginManager().registerEvents(new WorldManager(this), this);

        // Load worlds on startup
        loadWorldsOnStartup();
    }

    @Override
    public void onDisable() {
        // Cleanup logic
    }

    public static CoManyWorld getInstance() {
        return instance;
    }

    private void loadWorldsOnStartup() {
        List<String> worlds = getConfig().getStringList("worlds");
        for (String worldName : worlds) {
            World.Environment environment = World.Environment.NORMAL;

            if (worldName.endsWith("_nether")) {
                environment = World.Environment.NETHER;
            } else if (worldName.endsWith("_the_end")) {
                environment = World.Environment.THE_END;
            }

            getLogger().info(">[co_many] Loading world: " + worldName + " with environment: " + environment);
            new WorldCreator(worldName).environment(environment).createWorld();
        }
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }
}
