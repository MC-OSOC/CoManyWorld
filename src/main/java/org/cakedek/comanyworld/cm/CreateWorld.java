package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.cakedek.comanyworld.CoManyWorld;
import org.cakedek.comanyworld.WorldFolders;
import org.cakedek.comanyworld.WorldNames;

import java.util.*;

public class CreateWorld {
    private static final List<String> RESERVED_WORLD_NAMES = Arrays.asList("world", "world_nether", "world_the_end");
    /** Groups currently being created, so running the command twice can't create the same world twice. */
    private static final Set<String> IN_PROGRESS = new HashSet<>();

    /** True while a world group is being created (main thread only). */
    public static boolean isCreating(String groupName) {
        return IN_PROGRESS.contains(groupName);
    }

    ////// CREATE WORLD
    public static void createWorld(CommandSender sender, String requestedName, boolean createAll, int worldType, Long seed) {
        String worldName = WorldNames.sanitize(requestedName);
        if (worldName == null) {
            sender.sendMessage(ChatColor.RED + "Invalid world name. Use only letters, numbers, '_' and '-' (max 48).");
            return;
        }
        String folderName = WorldNames.ROOT + worldName;

        // Check if the world already exists
        if (RESERVED_WORLD_NAMES.contains(worldName) || worldExists(folderName) || IN_PROGRESS.contains(folderName)
                || WorldImport.isImporting(folderName)) {
            sender.sendMessage(ChatColor.RED + "This world name is reserved or already exists. Please choose a different name.");
            return;
        }

        // Every world uses the same folder names, so portals can find the matching Nether / End.
        Map<String, World.Environment> toCreate = new LinkedHashMap<>();
        if (createAll || worldType == -11) {
            toCreate.put(folderName + "/" + WorldNames.OVERWORLD, World.Environment.NORMAL);
        }
        if (createAll || worldType == -12) {
            toCreate.put(folderName + "/" + WorldNames.NETHER, World.Environment.NETHER);
        }
        if (createAll || worldType == -13) {
            toCreate.put(folderName + "/" + WorldNames.THE_END, World.Environment.THE_END);
        }

        BossBar progressBar = Bukkit.createBossBar(ChatColor.GREEN + "Creating world: " + worldName, BarColor.BLUE, BarStyle.SOLID);
        progressBar.setProgress(0.0);
        if (sender instanceof Player) {
            progressBar.addPlayer((Player) sender);
        }
        IN_PROGRESS.add(folderName);

        // Create one world per tick so the boss bar shows real progress for -all.
        Iterator<Map.Entry<String, World.Environment>> pending = toCreate.entrySet().iterator();
        List<String> created = new ArrayList<>();
        new BukkitRunnable() {
            int done = 0;

            @Override
            public void run() {
                if (pending.hasNext()) {
                    Map.Entry<String, World.Environment> next = pending.next();
                    if (createWorldWithEnvironment(next.getKey(), next.getValue(), seed)) {
                        created.add(next.getKey());
                    } else {
                        sender.sendMessage(ChatColor.RED + "Failed to create world " + next.getKey());
                    }
                    done++;
                    progressBar.setProgress((double) done / toCreate.size());
                    return;
                }
                cancel();
                progressBar.removeAll();
                IN_PROGRESS.remove(folderName);

                if (created.isEmpty()) {
                    return;
                }
                sender.sendMessage(ChatColor.GREEN + "World " + worldName + " created: " + String.join(", ", created)
                        + (seed != null ? " Seed: " + seed : ""));

                // ADD WORLD TO CONFIG.YML
                List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
                for (String name : created) {
                    if (!worlds.contains(name)) {
                        worlds.add(name);
                    }
                }
                CoManyWorld.getInstance().getConfig().set("worlds", worlds);
                CoManyWorld.getInstance().saveConfig();
            }
        }.runTaskTimer(CoManyWorld.getInstance(), 1L, 1L);
    }

    private static boolean worldExists(String worldName) {
        // Check if the group's folder exists, in the old place or in Paper 26.x's dimensions folder
        if (WorldFolders.existsOnDisk(worldName)) {
            return true;
        }
        // Check if a world with this name is loaded or listed in the config
        if (Bukkit.getWorld(worldName) != null) {
            return true;
        }
        String prefix = worldName + "/";
        return CoManyWorld.getInstance().getConfig().getStringList("worlds").stream()
                .anyMatch(name -> name.equalsIgnoreCase(worldName) || name.toLowerCase(Locale.ROOT).startsWith(prefix));
    }

    private static boolean createWorldWithEnvironment(String worldName, World.Environment environment, Long seed) {
        WorldCreator worldCreator = new WorldCreator(worldName);
        worldCreator.environment(environment);
        if (seed != null) {
            worldCreator.seed(seed);
        }
        try {
            return Bukkit.createWorld(worldCreator) != null;
        } catch (RuntimeException e) {
            CoManyWorld.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Failed to create world " + worldName, e);
            return false;
        }
    }
}
