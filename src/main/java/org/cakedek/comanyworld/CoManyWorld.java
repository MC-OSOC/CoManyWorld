package org.cakedek.comanyworld;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

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
        getCommand("co-many").setExecutor(new CommandHandler());
        getCommand("co-many").setTabCompleter(new CommandCompleter());

        // Register events
        getServer().getPluginManager().registerEvents(new WorldManager(this), this);

        // Load worlds on startup
        loadWorldsOnStartup();
    }

    @Override
    public void onDisable() {

    }

    public static CoManyWorld getInstance() {
        return instance;
    }

    // โหลดโลกขี้นมา
    private void loadWorldsOnStartup() {
        List<String> worlds = getConfig().getStringList("worlds");
        for (String worldName : worlds) {
            String fullWorldPath;
            fullWorldPath = worldName;

            World.Environment environment = World.Environment.NORMAL;

            if (worldName.endsWith("nether")) {
                environment = World.Environment.NETHER;
            } else if (worldName.endsWith("the_end")) {
                environment = World.Environment.THE_END;
            }

            getLogger().info(">co_many Loading world: " + fullWorldPath + " with environment: " + environment);
            new WorldCreator(fullWorldPath).environment(environment).createWorld();
        }
    }
}
