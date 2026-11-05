package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandCompleter implements TabCompleter {

    private static final List<String> COMMANDS = Arrays.asList("create", "tp", "list", "delete", "about", "import", "backup", "clear");
    private static final List<String> WORLD_TYPES = Arrays.asList("-11", "-12", "-13", "-all", "-flat", "-void", "-large", "-amplified", "-s", "-g");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean admin = sender.hasPermission(CommandHandler.PERM_ADMIN);
        if (args.length == 1) {
            if (admin) {
                return filter(COMMANDS.stream(), args[0]);
            }
            // Without admin only tp (and clear, which has its own permission) can be used
            Stream<String> allowed = COMMANDS.stream().filter(name ->
                    (name.equals("tp") && CommandHandler.canUseTp(sender))
                            || (name.equals("clear") && sender.hasPermission(CommandHandler.PERM_CLEAR)));
            return filter(allowed, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!admin && !(sub.equals("tp") || sub.equals("clear"))) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            switch (sub) {
                case "tp":
                    return filter(Bukkit.getWorlds().stream().map(World::getName)
                            .filter(name -> CommandHandler.canTeleportTo(sender, name)), args[1]);
                case "backup":
                    return filter(Bukkit.getWorlds().stream().map(World::getName), args[1]);
                case "delete":
                    return filter(getCustomWorldsForDeletion().stream(), args[1]);
                case "import":
                    return filter(getImportCandidates().stream(), args[1]);
                case "clear":
                    return filter(Stream.of("confirm"), args[1]);
                default:
                    break;
            }
        }
        if (args.length >= 3 && sub.equals("create")) {
            return filter(WORLD_TYPES.stream(), args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    private static List<String> filter(Stream<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList());
    }

    /** Managed worlds, plus "many_world/<name>/" for groups with more than one world. */
    private List<String> getCustomWorldsForDeletion() {
        Map<String, List<String>> groups = new TreeMap<>();
        for (World world : Bukkit.getWorlds()) {
            String name = world.getName();
            if (WorldNames.isManaged(name)) {
                groups.computeIfAbsent(WorldNames.groupPrefix(name), k -> new ArrayList<>()).add(name);
            }
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            if (entry.getValue().size() > 1) {
                result.add(entry.getKey());
            }
            result.addAll(entry.getValue());
        }
        return result;
    }

    /** Folders and .zip files waiting in plugins/CoManyWorld/import */
    private List<String> getImportCandidates() {
        File[] files = new File(CoManyWorld.getInstance().getDataFolder(), "import").listFiles(
                file -> !file.getName().equals("cache")
                        && (file.isDirectory() || file.getName().toLowerCase(Locale.ROOT).endsWith(".zip")));
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());
    }
}
