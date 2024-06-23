package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class WorldDelete implements Listener {

    //  ลบโลกชั่วคราวลงถังขยะของท่าน
    public static void deleteWorlds(CommandSender sender, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        } else {
            sender.sendMessage("World not found: " + worldName);
            return;
        }

        List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
        if (worlds.contains(worldName)) {
            worlds.remove(worldName);
            CoManyWorld.getInstance().getConfig().set("worlds", worlds);
            CoManyWorld.getInstance().saveConfig();
        }

        File deleteWorldsFile = new File(CoManyWorld.getInstance().getDataFolder(), "delete_worlds.yml");
        FileConfiguration deleteWorldsConfig = YamlConfiguration.loadConfiguration(deleteWorldsFile);

        List<String> deleteWorlds = deleteWorldsConfig.getStringList("delete_worlds");
        if (!deleteWorlds.contains(worldName)) {
            deleteWorlds.add(worldName);
            deleteWorldsConfig.set("delete_worlds", deleteWorlds);
            try {
                deleteWorldsConfig.save(deleteWorldsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        sender.sendMessage("World " + worldName + " has been deleted and recorded.");
    }

}