package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

public class WorldCleanup {

    public static void cleanupWorlds() {
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            if (worldName.startsWith("many_world/")) {
                File worldFolder = world.getWorldFolder();
                File sessionLock = new File(worldFolder, "session.lock");
                if (sessionLock.exists()) {
                    try {
                        Files.delete(sessionLock.toPath());
                        CoManyWorld.getInstance().getLogger().info("Deleted session.lock for world: " + worldName);
                    } catch (IOException e) {
                        CoManyWorld.getInstance().getLogger().log(Level.WARNING, "Failed to delete session.lock for world: " + worldName, e);
                    }
                }
            }
        }
    }
}