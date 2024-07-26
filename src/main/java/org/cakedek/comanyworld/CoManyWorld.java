package org.cakedek.comanyworld;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.cakedek.comanyworld.cm.WorldCleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;

public class CoManyWorld extends JavaPlugin implements Listener {

    private static CoManyWorld instance;

    @Override
    public void onEnable() {
        // Save default configuration
        saveDefaultConfig();
        // Set the instance
        instance = this;

        // Initialize WorldManager
        WorldManager worldManager = new WorldManager(this);

        // Create Import Folder
        File importFolder = new File(getDataFolder(), "import");
        if (!importFolder.exists()) {
            importFolder.mkdirs();
            getLogger().info("Created import folder: " + importFolder.getAbsolutePath());
        }

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
        WorldCleanup.cleanupWorlds();
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

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();
        if (worldName.startsWith("many_world/")) {
            File worldFolder = world.getWorldFolder();
            File sessionLock = new File(worldFolder, "session.lock");
            if (sessionLock.exists()) {
                try {
                    Files.delete(sessionLock.toPath());
                    CoManyWorld.getInstance().getLogger().info("Deleted existing session.lock for world: " + worldName);
                } catch (IOException e) {
                    CoManyWorld.getInstance().getLogger().log(Level.WARNING, "Failed to delete existing session.lock for world: " + worldName, e);
                }
            }
        }
    }
}
