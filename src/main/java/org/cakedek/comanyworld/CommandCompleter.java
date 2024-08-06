package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("NullableProblems")
public class CommandCompleter implements TabCompleter {

    private static final List<String> COMMANDS = Arrays.asList("create", "tp", "list", "delete", "about", "import", "backup");
    private static final List<String> WORLD_TYPES = Arrays.asList("-11", "-12", "-13", "-all");
    private static final List<String> DEFAULT_WORLDS = Arrays.asList("world", "world_nether", "world_the_end");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("backup"))) {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            return getCustomWorldsForDeletion().stream()
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("create")) {
            List<String> suggestions = new ArrayList<>(WORLD_TYPES);
            suggestions.add("-s");
            return suggestions.stream()
                    .filter(type -> type.startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }

    private List<String> getCustomWorldsForDeletion() {
        Map<String, List<String>> worldGroups = new HashMap<>();

        for (String worldName : getCustomWorlds()) {
            String[] parts = worldName.split("/");
            if (parts.length > 2) {
                String parentWorld = parts[0] + "/" + parts[1] + "/";
                worldGroups.computeIfAbsent(parentWorld, k -> new ArrayList<>()).add(worldName);
            } else {
                worldGroups.put(worldName, Collections.singletonList(worldName));
            }
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : worldGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                // If there are multiple worlds in this group, add the parent world with a trailing slash
                result.add(entry.getKey());
            } else {
                // If there's only one world in this group, add it without a trailing slash
                result.add(entry.getValue().get(0));
            }
        }
        
        return result;
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
    private String getMainWorldFolder(String worldName) {
        if (worldName.startsWith("many_world/")) {
            String[] parts = worldName.split("/");
            if (parts.length > 2) {
                String baseName = "many_world/" + parts[1];
                if (parts[2].equals("world") || parts[2].equals("world_nether") || parts[2].equals("world_the_end")) {
                    return baseName;
                } else {
                    // ในกรณีที่เป็นโครงสร้างเก่า หรือ โครงสร้างที่ไม่ทราบ
                    return worldName;
                }
            }
        }
        return worldName;
    }
}