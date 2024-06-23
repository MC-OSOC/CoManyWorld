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

    private static final List<String> COMMANDS = Arrays.asList("create", "tp", "list", "delete", "about","import");

    private static final List<String> DEFAULT_WORLDS = Arrays.asList("world", "world_nether", "world_the_end");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("tp"))) {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return getCustomWorlds().stream()
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }


    private List<String> getCustomWorlds() {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(name -> !DEFAULT_WORLDS.contains(name))
                .filter(name -> !isRootWorld(name))
                .collect(Collectors.toList());
    }

    private boolean isRootWorld(String worldName) {
        return worldName.equalsIgnoreCase("world") ||
                worldName.equalsIgnoreCase("world_nether") ||
                worldName.equalsIgnoreCase("world_the_end") ||
                worldName.equalsIgnoreCase("many_world/world")||
                worldName.equalsIgnoreCase("many_world/world_nether")||
                worldName.equalsIgnoreCase("many_world/world_the_end");
    }
}