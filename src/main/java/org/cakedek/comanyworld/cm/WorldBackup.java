package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WorldBackup {

    public static void backupWorld(CommandSender sender, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World not found: " + worldName);
            return;
        }

        String backupFolderPath = CoManyWorld.getInstance().getDataFolder().getParent() + "co-many-world/many_world_backup";
        File backupFolder = new File(backupFolderPath);
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String timestamp = dateFormat.format(new Date());
        String backupFileName = timestamp + "-" + worldName + "-" + System.currentTimeMillis() + ".zip";

        // Create a subfolder for this backup
        File backupSubFolder = new File(backupFolder, timestamp + "-" + worldName);
        if (!backupSubFolder.exists()) {
            backupSubFolder.mkdirs();
        }
        File tempFolder = new File(backupFolder, "temp");
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }

        File backupFile = new File(backupSubFolder, backupFileName);
        BossBar progressBar = Bukkit.createBossBar(ChatColor.GREEN + "Backing up world: " + worldName, BarColor.BLUE, BarStyle.SOLID);
        if (sender instanceof Player) {
            progressBar.addPlayer((Player) sender);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Step 1: Copy world files to temp folder
                    copyWorldToTemp(world.getWorldFolder(), tempFolder, progressBar, 0.0, 0.5);

                    // Step 2: Create zip from temp folder
                    zipWorld(tempFolder, backupFile, progressBar, 0.5, 1.0);

                    sender.sendMessage(ChatColor.GREEN + "World " + worldName + " backed up successfully.");
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Failed to backup world: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // Clean up temp folder
                    deleteFolder(tempFolder);
                    progressBar.removeAll();
                }
            }
        }.runTaskAsynchronously(CoManyWorld.getInstance());
    }

    private static void copyWorldToTemp(File sourceFolder, File tempFolder, BossBar progressBar, double startProgress, double endProgress) throws IOException {
        long totalSize = getFolderSize(sourceFolder);
        long copiedSize = 0;

        for (File sourceFile : sourceFolder.listFiles()) {
            if (sourceFile.getName().equals("session.lock")) {
                continue; // Skip session.lock file
            }

            File destFile = new File(tempFolder, sourceFile.getName());
            if (sourceFile.isDirectory()) {
                destFile.mkdirs();
                copyWorldToTemp(sourceFile, destFile, progressBar, startProgress, endProgress);
            } else {
                try {
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.out.println("Failed to copy file: " + sourceFile.getName() + ". Skipping...");
                    continue;
                }
                copiedSize += sourceFile.length();
                updateProgressBar(progressBar, copiedSize, totalSize, startProgress, endProgress);
            }
        }
    }

    private static void zipWorld(File tempFolder, File zipFile, BossBar progressBar, double startProgress, double endProgress) throws IOException {
        // Ensure the parent directory of the zip file exists
        zipFile.getParentFile().mkdirs();

        long totalSize = getFolderSize(tempFolder);
        long zippedSize = 0;

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zipFolder(tempFolder, tempFolder.getName(), zos, progressBar, totalSize, zippedSize, startProgress, endProgress);
        }
    }

    private static void zipFolder(File folder, String parentFolder, ZipOutputStream zos, BossBar progressBar,
                                  long totalSize, long zippedSize, double startProgress, double endProgress) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipFolder(file, parentFolder + "/" + file.getName(), zos, progressBar, totalSize, zippedSize, startProgress, endProgress);
                continue;
            }

            zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));
            Files.copy(file.toPath(), zos);
            zippedSize += file.length();
            updateProgressBar(progressBar, zippedSize, totalSize, startProgress, endProgress);
            zos.closeEntry();
        }
    }

    private static void updateProgressBar(BossBar progressBar, long currentSize, long totalSize, double startProgress, double endProgress) {
        double progress = startProgress + (endProgress - startProgress) * ((double) currentSize / totalSize);
        progressBar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
    }



    private static long getFolderSize(File folder) {
        long size = 0;
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                size += getFolderSize(file);
            } else {
                size += file.length();
            }
        }
        return size;
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }
}