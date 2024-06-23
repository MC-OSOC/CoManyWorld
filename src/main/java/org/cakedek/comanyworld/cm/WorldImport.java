package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.File;
import java.util.List;

public class WorldImport implements Listener {

    public static void importWorld(CommandSender sender, String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists() || !worldFolder.isDirectory()) {
            sender.sendMessage("World " + worldName + " does not exist.");
            return;
        }

        String newWorldName = worldName.startsWith("many_world/") ? worldName : "many_world/" + worldName;
        File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);

        if (worldFolder.renameTo(newWorldFolder)) {
            WorldCreator creator = new WorldCreator(newWorldName);
            World world = Bukkit.createWorld(creator);
            if (world != null) {
                sender.sendMessage("World " + worldName + " has been successfully imported and renamed to " + newWorldName);

                // Update config.yml
                List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
                worlds.add(newWorldName);
                CoManyWorld.getInstance().getConfig().set("worlds", worlds);
                CoManyWorld.getInstance().saveConfig();

            } else {
                sender.sendMessage("Failed to create the world " + newWorldName + ".");
            }
        } else {
            sender.sendMessage("Failed to rename the world folder.");
        }
    }
}
