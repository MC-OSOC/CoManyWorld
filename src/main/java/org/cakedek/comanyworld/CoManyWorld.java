package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class CoManyWorld extends JavaPlugin {

    private static CoManyWorld instance;

    @Override
    public void onEnable() {
        instance = this;
        // Save default configuration
        saveDefaultConfig();

        getLogger().info("Running on " + Platform.describe()
                + (Platform.isPaper() ? " (Paper features enabled)" : ""));

        // Create Import Folder
        File importFolder = new File(getDataFolder(), "import");
        if (!importFolder.exists() && importFolder.mkdirs()) {
            getLogger().info("Created import folder: " + importFolder.getAbsolutePath());
        }

        // Register commands and tab completer
        CommandHandler handler = new CommandHandler();
        CommandCompleter completer = new CommandCompleter();
        PluginCommand coMany = getCommand("co-many");
        if (coMany != null) {
            coMany.setExecutor(handler);
            coMany.setTabCompleter(completer);
        }
        // /co-many-clear is the same as /co-many clear
        PluginCommand coManyClear = getCommand("co-many-clear");
        if (coManyClear != null) {
            coManyClear.setExecutor((sender, command, label, args) ->
                    handler.onCommand(sender, command, label, prepend("clear", args)));
            coManyClear.setTabCompleter((sender, command, alias, args) ->
                    completer.onTabComplete(sender, command, alias, prepend("clear", args)));
        }

        // Register events (the WorldManager registers itself)
        new WorldManager(this);

        // Load worlds on startup
        loadWorldsOnStartup();
    }

    public static CoManyWorld getInstance() {
        return instance;
    }

    // โหลดโลกขี้นมา
    private void loadWorldsOnStartup() {
        List<String> worlds = getConfig().getStringList("worlds");
        for (String worldName : worlds) {
            if (Bukkit.getWorld(worldName) != null) {
                continue; // already loaded (e.g. by another plugin or a /reload)
            }
            File folder = WorldNames.managedFolder(worldName);
            if (folder == null || !folder.isDirectory()) {
                // Don't silently generate a brand new world when the folder was removed or moved.
                getLogger().warning("Skipping world '" + worldName + "' from config.yml: its folder does not exist"
                        + " or is not inside " + WorldNames.ROOT);
                continue;
            }

            World.Environment environment = WorldNames.environmentOf(worldName);
            getLogger().info(">co_many Loading world: " + worldName + " with environment: " + environment);
            WorldCreator creator = new WorldCreator(worldName).environment(environment);
            if (environment == World.Environment.NORMAL) {
                // flat / void / large / amplified / custom generator chosen when the world was created
                WorldOptions.load(getConfig(), worldName).applyTo(creator);
            }
            if (creator.createWorld() == null) {
                getLogger().warning("Failed to load world: " + worldName);
            }
        }
    }

    private static String[] prepend(String first, String[] rest) {
        String[] result = new String[rest.length + 1];
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }
}
