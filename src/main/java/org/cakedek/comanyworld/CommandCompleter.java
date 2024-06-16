package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCompleter implements TabCompleter {

    private static final List<String> COMMANDS = Arrays.asList("create", "tp", "list", "del", "about");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("del"))) {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .map(this::getWorldDisplayName)
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }

    private String getWorldDisplayName(String folderName) {
        if (folderName.startsWith("world-many-")) {
            return folderName.substring("world-many-".length());
        }
        return folderName;
    }
}