package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cakedek.comanyworld.cm.*;

public class CommandHandler implements CommandExecutor {

    static final String PERM_DELETE = "co.many.worlds.admindel";
    static final String PERM_CLEAR = "co.many.worlds.adminclear";

    // COMMAND IN
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String se = ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                ChatColor.YELLOW + "/co-many create [worldName] [-11|-12|-13|-all] [-s<seed>] " + ChatColor.RESET + ">> Create a new world\n" +
                ChatColor.YELLOW + "/co-many list " + ChatColor.RESET + ">> world list \n" +
                ChatColor.YELLOW + "/co-many tp [worldName] " + ChatColor.RESET + ">> tp to world\n" +
                ChatColor.YELLOW + "/co-many import [worldName] " + ChatColor.RESET + ">> Import World \n" +
                ChatColor.YELLOW + "/co-many delete [worldName] " + ChatColor.RESET + ">> Delete world (move to trash)\n" +
                ChatColor.YELLOW + "/co-many clear [confirm] " + ChatColor.RESET + ">> Clear the permanent trash world\n" +
                ChatColor.YELLOW + "/co-many backup [worldName] " + ChatColor.RESET + ">> Backup world\n" +
                ChatColor.YELLOW + "/co-many about " + ChatColor.RESET + ">> About \n";
        if (args.length == 0) {
            sender.sendMessage(se);
            return true;
        }
        //////  CREATE
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length == 2) {
                // No type given: default-world-type from config.yml (-11 unless changed)
                String defaultType = CoManyWorld.getInstance().getConfig().getString("default-world-type", "-11").trim();
                args = new String[]{args[0], args[1], defaultType};
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.GOLD + "=== Many World > Create " + ChatColor.RESET +
                        "\n-11 [WORLD] " +
                        "\n-12 [NETHER] " +
                        "\n-13 [THE_END] " +
                        "\n-all [ALL_WORLD]" +
                        "\n-s<seed> [SEED]" +
                        "\nCommand: /co-many create <worldName> [-11|-12|-13|-all] [-s<seed>]");
                return true;
            }

            String worldName = args[1];
            boolean createAll = false;
            int worldType = -11;
            Long seed = null;

            for (int i = 2; i < args.length; i++) {
                String arg = args[i];
                if (arg.equalsIgnoreCase("-all")) {
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

            CreateWorld.createWorld(sender, worldName, createAll, worldType, seed);
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
                    "World folders >>> " + WorldFolders.describe() + "\n" +
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


    // วาร์ปผู้เล่นไปยังโลกที่สร้างไว่้
    private void teleportPlayer(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World " + ChatColor.WHITE + worldName + ChatColor.RED + " does not exist.");
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
