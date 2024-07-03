package org.cakedek.comanyworld;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.cakedek.comanyworld.cm.*;

import java.util.List;

public class CommandHandler implements CommandExecutor {

    // COMMAND IN
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String se =     ChatColor.GOLD + "=== Many World \n" + ChatColor.RESET +
                        ChatColor.YELLOW + "/co-many create [worldName] [World] " + ChatColor.RESET + ">> Create a new world\n" +
                        ChatColor.YELLOW + "/co-many list " + ChatColor.RESET + ">> world list \n" +
                        ChatColor.YELLOW + "/co-many tp [worldName] " + ChatColor.RESET + ">> tp to world\n" +
                        ChatColor.YELLOW + "/co-many import [worldName] " + ChatColor.RESET + ">> Import World \n" +
                        ChatColor.YELLOW + "/co-many delete [worldName] " + ChatColor.RESET + ">> Delete world\n" +
                        ChatColor.YELLOW + "/co-many clear " + ChatColor.RESET + ">> Clear the permanent trash world\n" +
                        ChatColor.YELLOW + "/co-many backup [worldName] " + ChatColor.RESET + ">> Backup world\n" +
                        ChatColor.YELLOW + "/co-many about " + ChatColor.RESET + ">> About \n";
        if (args.length == 0) {
            sender.sendMessage(se);
            return true;
        }
        //////  CREATE
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length >= 3 && args[2].equalsIgnoreCase("-all")) {
                CreateWorld.createWorld(sender, args[1], true, -1);
            } else if (args.length == 3) {
                int worldType = Integer.parseInt(args[2]);
                CreateWorld.createWorld(sender, args[1], false, worldType);
            } else {
                ///////////////////////////////////////////////////////////////////////////////////////////////
                sender.sendMessage(ChatColor.GOLD+"=== Many World > Create "+ ChatColor.RESET +
                                                    "\n-11 [WORLD] " +
                                                    "\n-12 [NETHER] " +
                                                    "\n-13 [THE_END] " +
                                                    "\n-all [ALL_WORLD]" +
                                                    "\nCommand: /co-many create <worldName> [-11|-12|-13]");
            }
            return true;
        }
        //////  TP
        else if (args[0].equalsIgnoreCase("tp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            if (args.length < 2) {
                //////////////////////////////////////////////////////////////////////////////////////////////
                sender.sendMessage(     ChatColor.GOLD + "=== Many World > Teleport "+ ChatColor.RESET +
                                        "\nCommand: /co-many tp <worldName>");
                return true;
            }
            String worldName = args[1];
            teleportPlayer((Player) sender, worldName);
        }
        //////  LIST
        else if (args[0].equalsIgnoreCase("list")) {
            ListWorlds.listWorlds(sender);
        }
        //////  DELETE
        else if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.GOLD +  "=== Many World > Delete World "+ ChatColor.RESET +
                                                        "\nCommand: /co-many delete <worldName>");
                return true;
            }
            String worldName = args[1];
            WorldDelete.deleteWorlds(sender, worldName);
        }
        //////  IMPORT
        else if (args[0].equalsIgnoreCase("import")) {
            if (args.length == 2) {
                String worldName = args[1];
                WorldImport.importWorld(sender ,worldName);
            } else {
                //////////////////////////////////////////////////////////////////////////////////////////////
                sender.sendMessage(     ChatColor.GOLD + "=== Many World > Import World "+ ChatColor.RESET +
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
            String worldName = args[1];
            WorldBackup.backupWorld(sender, worldName);
        }
        //////  CLEAR WORLD
        else if (args[0].equalsIgnoreCase("clear")) {
            WorldClear.clearDeletedWorlds(sender);
        }
        //////  ABOUT
        else if (args[0].equalsIgnoreCase("about")) {
            sender.sendMessage(ChatColor.GOLD  + "=== Many World List > About \n" + ChatColor.RESET +
                    "Version >>> " + CoManyWorld.getInstance().getDescription().getVersion() + "\n" +
                    "Minecraft Server Version >>> " + Bukkit.getVersion() + "\n" +
                    "->->->\n" +
                    "MC-OSOC \n" +
                    "github >>> https://github.com/MC-OSOC\n" +
                    "*** Some of the code was developed by AI.\n" +
                    "===============================");
        }

        else {
            sender.sendMessage(se);
        }
        return true;
    }


    // วาร์ปผู้เล่นไปยังโลกที่สร้างไว่้
    private void teleportPlayer(Player player, String worldName) {
        World world = Bukkit.getWorld((worldName));
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World " + ChatColor.WHITE + worldName + ChatColor.RED + " does not exist.");
            return;
        }
        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.WHITE + worldName + ChatColor.GREEN + ".");
    }
}
