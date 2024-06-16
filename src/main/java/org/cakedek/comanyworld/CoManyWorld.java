package org.cakedek.comanyworld;

import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CoManyWorld extends JavaPlugin {

    private static CoManyWorld instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getCommand("co-many").setExecutor(new CommandHandler());
        getCommand("co-many").setTabCompleter(new CommandCompleter());
        getServer().getPluginManager().registerEvents(new WorldManager(), this);

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
            getLogger().info(">[co_many] Loading world: " + worldName);
            new WorldCreator(worldName).createWorld();
        }
    }
}
