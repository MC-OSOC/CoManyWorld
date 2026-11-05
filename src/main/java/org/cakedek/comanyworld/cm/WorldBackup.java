package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WorldBackup {

    private static final Set<String> RUNNING = new HashSet<>();

    /**
     * Saves the world, then zips its folder in the background into
     * plugins/CoManyWorld/many_world_backup/&lt;timestamp&gt;-&lt;world&gt;.zip
     */
    public static void backupWorld(CommandSender sender, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World not found: " + worldName);
            return;
        }
        String name = world.getName();
        if (!RUNNING.add(name)) {
            sender.sendMessage(ChatColor.RED + "A backup of " + name + " is already running.");
            return;
        }

        CoManyWorld plugin = CoManyWorld.getInstance();
        Path source = world.getWorldFolder().toPath();
        Path backupFolder = plugin.getDataFolder().toPath().resolve("many_world_backup");
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String baseName = timestamp + "-" + name.replace('/', '_');
        Path backupFile = backupFolder.resolve(baseName + ".zip");
        // Two backups in the same second must not overwrite each other.
        for (int i = 2; Files.exists(backupFile); i++) {
            backupFile = backupFolder.resolve(baseName + "-" + i + ".zip");
        }
        Path finalBackupFile = backupFile;
        // Entries are stored as "<folder name>/..." so the zip can be imported again with /co-many import.
        String rootEntry = source.getFileName().toString();

        // Like a manual backup (save-off, save-all flush, copy, save-on): pause saving so the files don't
        // change while zipping, then write everything to disk and WAIT for it. world.save() alone only
        // queues the chunks; the server writes region files in the background, so zipping right after it
        // could copy half-written region files (chunks "in the wrong location" after an import).
        boolean autoSave = world.isAutoSave();
        world.setAutoSave(false);
        world.save();
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all flush");
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "save-all flush failed, the backup may contain unsaved chunks", e);
        }

        BossBar progressBar = Bukkit.createBossBar(ChatColor.GREEN + "Backing up world: " + name, BarColor.BLUE, BarStyle.SOLID);
        progressBar.setProgress(0.0);
        if (sender instanceof Player) {
            progressBar.addPlayer((Player) sender);
        }

        AtomicLong done = new AtomicLong();
        AtomicLong total = new AtomicLong(1);
        // The boss bar is only touched from the main thread.
        BukkitTask progressTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> progressBar.setProgress(Math.min(1.0, (double) done.get() / Math.max(1, total.get()))), 10L, 10L);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String error = null;
            try {
                zipFolder(source, finalBackupFile, rootEntry, done, total);
            } catch (IOException | RuntimeException e) {
                error = e.getMessage();
                plugin.getLogger().log(Level.SEVERE, "Failed to backup world " + name, e);
            }
            String finalError = error;
            Bukkit.getScheduler().runTask(plugin, () -> {
                progressTask.cancel();
                progressBar.removeAll();
                World stillLoaded = Bukkit.getWorld(name);
                if (stillLoaded != null) {
                    stillLoaded.setAutoSave(autoSave);
                }
                RUNNING.remove(name);
                if (finalError == null) {
                    sender.sendMessage(ChatColor.GREEN + "World " + name + " backed up successfully: "
                            + ChatColor.WHITE + "many_world_backup/" + finalBackupFile.getFileName());
                } else {
                    sender.sendMessage(ChatColor.RED + "Failed to backup world: " + finalError);
                }
            });
        });
    }

    private static void zipFolder(Path source, Path zipFile, String rootEntry, AtomicLong done, AtomicLong total) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(source)) {
            // Symbolic links are not followed, so nothing outside the world folder ends up in the zip.
            files = walk.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> !path.getFileName().toString().equals("session.lock")) // locked by the server
                    .collect(Collectors.toList());
        }
        long size = 0;
        for (Path file : files) {
            size += Files.size(file);
        }
        total.set(size);

        Files.createDirectories(zipFile.getParent());
        Path temp = zipFile.resolveSibling(zipFile.getFileName() + ".part");
        try (OutputStream out = Files.newOutputStream(temp);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Path file : files) {
                String relative = source.relativize(file).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(rootEntry + "/" + relative));
                try {
                    done.addAndGet(Files.copy(file, zos));
                } catch (IOException e) {
                    CoManyWorld.getInstance().getLogger().warning("Failed to copy file: " + file + ". Skipping...");
                }
                zos.closeEntry();
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
        Files.move(temp, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }
}
