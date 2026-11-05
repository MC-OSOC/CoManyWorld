package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.cakedek.comanyworld.cm.*;

import java.util.Locale;

public class CommandHandler implements CommandExecutor {

    static final String PERM_ADMIN = "co.many.worlds.admin";
    static final String PERM_DELETE = "co.many.worlds.admindel";
    static final String PERM_CLEAR = "co.many.worlds.adminclear";
    /** Teleport to any world. A single world is allowed with {@code co.many.worlds.tp.<world name>}. */
    static final String PERM_TP = "co.many.worlds.tp";

    /** True if the sender may teleport to this world (all worlds, or this one). */
    static boolean canTeleportTo(CommandSender sender, String worldName) {
        return sender.hasPermission(PERM_TP) || sender.hasPermission(PERM_TP + "." + worldName);
    }

    /** True if the sender may teleport to at least one world, so /co-many tp is worth showing. */
    static boolean canUseTp(CommandSender sender) {
        if (sender.hasPermission(PERM_TP)) {
            return true;
        }
        return sender.getEffectivePermissions().stream()
                .anyMatch(info -> info.getValue() && info.getPermission().startsWith(PERM_TP + "."));
    }

    // COMMAND IN
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String se = ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                ChatColor.YELLOW + "/co-many create [worldName] [-11|-12|-13|-all] [-s<seed>] [-flat|-void|-large|-amplified|-g<plugin[:id]>] " + ChatColor.RESET + ">> Create a new world\n" +
                ChatColor.YELLOW + "/co-many list " + ChatColor.RESET + ">> world list \n" +
                ChatColor.YELLOW + "/co-many tp [worldName] " + ChatColor.RESET + ">> tp to world\n" +
                ChatColor.YELLOW + "/co-many import [worldName] " + ChatColor.RESET + ">> Import World \n" +
                ChatColor.YELLOW + "/co-many delete [worldName] " + ChatColor.RESET + ">> Delete world (move to trash)\n" +
                ChatColor.YELLOW + "/co-many clear [confirm] " + ChatColor.RESET + ">> Clear the permanent trash world\n" +
                ChatColor.YELLOW + "/co-many backup [worldName] " + ChatColor.RESET + ">> Backup world\n" +
                ChatColor.YELLOW + "/co-many about " + ChatColor.RESET + ">> About \n";
        boolean admin = sender.hasPermission(PERM_ADMIN);
        if (!admin) {
            // Everyone else may only use /co-many tp (and /co-many clear with its own permission)
            boolean clear = args.length >= 1 && args[0].equalsIgnoreCase("clear");
            boolean tp = args.length >= 1 && args[0].equalsIgnoreCase("tp");
            if (!clear && !tp) {
                if (args.length == 0 && canUseTp(sender)) {
                    // Plain /co-many: only list what they may use
                    sender.sendMessage(ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                            ChatColor.YELLOW + "/co-many tp [worldName] " + ChatColor.RESET + ">> tp to world");
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                return true;
            }
        }
        if (args.length == 0) {
            sender.sendMessage(se);
            return true;
        }
        //////  CREATE
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.GOLD + "=== Many World > Create " + ChatColor.RESET +
                        "\n-11 [WORLD] " +
                        "\n-12 [NETHER] " +
                        "\n-13 [THE_END] " +
                        "\n-all [ALL_WORLD]" +
                        "\n-s<seed> [SEED]" +
                        "\nOverworld only (pick one):" +
                        "\n-flat [FLAT] " +
                        "\n-void [EMPTY WORLD WITH A SMALL PLATFORM] " +
                        "\n-large [LARGE BIOMES] " +
                        "\n-amplified [AMPLIFIED] " +
                        "\n-g<plugin[:id]> [GENERATOR FROM ANOTHER PLUGIN]" +
                        "\nCommand: /co-many create <worldName> [-11|-12|-13|-all] [-s<seed>] [-flat|-void|-large|-amplified|-g<plugin[:id]>]");
                return true;
            }

            String worldName = args[1];
            boolean createAll = false;
            int worldType = -11;
            Long seed = null;
            WorldOptions.Kind kind = WorldOptions.Kind.NORMAL;
            String generator = null;
            boolean overworldFlag = false; // one of -flat / -void / -large / -amplified / -g was given

            for (int i = 2; i < args.length; i++) {
                String arg = args[i];
                WorldOptions.Kind flagKind = WorldOptions.kindOfFlag(arg);
                if (flagKind != null || arg.toLowerCase(Locale.ROOT).startsWith("-g")) {
                    if (overworldFlag) {
                        sender.sendMessage(ChatColor.RED + "Use only one of -flat, -void, -large, -amplified or -g<plugin[:id]>.");
                        return true;
                    }
                    overworldFlag = true;
                    if (flagKind != null) {
                        kind = flagKind;
                    } else {
                        generator = arg.substring(2);
                        String error = checkGenerator(generator);
                        if (error != null) {
                            sender.sendMessage(ChatColor.RED + error);
                            return true;
                        }
                    }
                } else if (arg.equalsIgnoreCase("-all")) {
                    createAll = true;
                } else if (arg.startsWith("-s")) {
                    try {
                        seed = Long.parseLong(arg.substring(2));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid seed format. Please use a number.");
                        return true;
                    }
                } else if (arg.equals("-11") || arg.equals("-12") || arg.equals("-13")) {
                    worldType = Integer.parseInt(arg);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid world type. Use -11, -12, -13, or -all.");
                    return true;
                }
            }

            if (overworldFlag && !createAll && worldType != -11) {
                sender.sendMessage(ChatColor.RED + "-flat, -void, -large, -amplified and -g only change the overworld: "
                        + "use them with -11 or -all.");
                return true;
            }
            CreateWorld.createWorld(sender, worldName, createAll, worldType, seed, new WorldOptions(kind, generator));
            return true;
        }
        //////  TP
        else if (args[0].equalsIgnoreCase("tp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.GOLD + "=== Many World > Teleport " + ChatColor.RESET +
                        "\nCommand: /co-many tp <worldName>");
                return true;
            }
            teleportPlayer((Player) sender, args[1]);
        }
        //////  LIST
        else if (args[0].equalsIgnoreCase("list")) {
            ListWorlds.listWorlds(sender);
        }
        //////  DELETE
        else if (args[0].equalsIgnoreCase("delete")) {
            if (!sender.hasPermission(PERM_DELETE)) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to delete worlds.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.GOLD + "=== Many World > Delete World " + ChatColor.RESET +
                        "\nCommand: /co-many delete <worldName>");
                return true;
            }
            WorldDelete.deleteWorlds(sender, args[1]);
        }
        //////  IMPORT
        else if (args[0].equalsIgnoreCase("import")) {
            if (args.length == 2) {
                WorldImport.importWorld(sender, args[1]);
            } else {
                sender.sendMessage(ChatColor.GOLD + "=== Many World > Import World " + ChatColor.RESET +
                        "\nPut a world folder or .zip in plugins/" + CoManyWorld.getInstance().getName() + "/import" +
                        "\nCommand: /co-many import <worldName>");
            }
            return true;
        }
        //////  BACKUP
        else if (args[0].equalsIgnoreCase("backup")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /co-many backup <worldName>");
                return true;
            }
            WorldBackup.backupWorld(sender, args[1]);
        }
        //////  CLEAR WORLD
        else if (args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission(PERM_CLEAR)) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to clear deleted worlds.");
                return true;
            }
            boolean confirm = args.length >= 2 && args[1].equalsIgnoreCase("confirm");
            WorldClear.clearDeletedWorlds(sender, confirm);
        }
        //////  ABOUT
        else if (args[0].equalsIgnoreCase("about")) {
            sender.sendMessage(ChatColor.GOLD + "=== Many World List > About \n" + ChatColor.RESET +
                    "Version >>> " + CoManyWorld.getInstance().getDescription().getVersion() + "\n" +
                    "Minecraft Server Version >>> " + Platform.describe() + "\n" +
                    "->->->\n" +
                    "MC-OSOC \n" +
                    "github >>> https://github.com/MC-OSOC\n" +
                    "*** Some of the code was developed by AI.\n" +
                    "===============================");
        } else {
            sender.sendMessage(se);
        }
        return true;
    }


    /** Error text if "-g<plugin[:id]>" can't be used, or null if the generator plugin is available. */
    private static String checkGenerator(String generator) {
        String pluginName = generator.split(":", 2)[0];
        if (pluginName.isEmpty()) {
            return "Usage: -g<plugin[:id]>, for example -gMyGenerator or -gMyGenerator:flat";
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) {
            return "Generator plugin '" + pluginName + "' is not installed or not enabled.";
        }
        return null;
    }

    // วาร์ปผู้เล่นไปยังโลกที่สร้างไว่้
    private void teleportPlayer(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World " + ChatColor.WHITE + worldName + ChatColor.RED + " does not exist.");
            return;
        }
        if (!canTeleportTo(player, world.getName())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to teleport to " + ChatColor.WHITE
                    + world.getName() + ChatColor.RED + ".");
            return;
        }
        Platform.teleport(player, world.getSpawnLocation()).thenAccept(success -> {
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.WHITE + world.getName() + ChatColor.GREEN + ".");
            } else {
                player.sendMessage(ChatColor.RED + "Teleport to " + ChatColor.WHITE + world.getName() + ChatColor.RED + " was cancelled.");
            }
        });
    }
}
