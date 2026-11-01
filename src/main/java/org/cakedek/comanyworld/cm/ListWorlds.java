package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class ListWorlds {
    public static void listWorlds(CommandSender sender) {
        StringBuilder worldList = new StringBuilder(ChatColor.GOLD + "=== Many World > List\n" + ChatColor.RESET);
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            String worldType = getWorldType(world.getEnvironment());
            worldList.append(ChatColor.YELLOW).append("- ").append(ChatColor.WHITE).append(worldName)
                    .append(ChatColor.GRAY).append(" [").append(ChatColor.GREEN).append(worldType).append(ChatColor.GRAY).append("]")
                    .append(ChatColor.GRAY).append(" (").append(world.getPlayers().size()).append(" players)\n");
        }
        sender.sendMessage(worldList.toString());
    }

    private static String getWorldType(World.Environment environment) {
        switch (environment) {
            case NETHER:
                return "Nether";
            case THE_END:
                return "The End";
            case NORMAL:
                return "World";
            default:
                return environment.name();
        }
    }
}
