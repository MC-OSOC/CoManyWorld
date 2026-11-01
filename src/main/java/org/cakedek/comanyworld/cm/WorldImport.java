package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.cakedek.comanyworld.CoManyWorld;
import org.cakedek.comanyworld.WorldNames;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WorldImport {

    /**
     * Files that must not be copied: session.lock belongs to whoever had the world open,
     * and uid.dat makes the server refuse to load a second copy of the same world.
     */
    private static final Set<String> SKIPPED_FILES = new HashSet<>(Arrays.asList("session.lock", "uid.dat"));
    private static final Set<String> IN_PROGRESS = new HashSet<>();

    public static void importWorld(CommandSender sender, String fileName) {
        CoManyWorld plugin = CoManyWorld.getInstance();
        File importFolder = new File(plugin.getDataFolder(), "import");
        if (!importFolder.exists()) {
            importFolder.mkdirs();
            sender.sendMessage("Created import folder: " + importFolder.getAbsolutePath());
            return;
        }

        // Only plain names of files/folders directly inside the import folder
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            sender.sendMessage(ChatColor.RED + "Use the name of a folder or .zip file inside " + importFolder.getPath());
            return;
        }
        File worldFile = new File(importFolder, fileName);
        if (!worldFile.exists()) {
            sender.sendMessage("World file or folder not found: " + worldFile.getAbsolutePath());
            return;
        }
        boolean isZip = worldFile.isFile() && fileName.toLowerCase(Locale.ROOT).endsWith(".zip");
        if (!worldFile.isDirectory() && !isZip) {
            sender.sendMessage("Unsupported file type. Please use a folder or a zip file.");
            return;
        }

        String cleanWorldName = WorldNames.sanitize(isZip ? fileName.substring(0, fileName.length() - 4) : fileName);
        if (cleanWorldName == null) {
            sender.sendMessage(ChatColor.RED + "Invalid world name. Rename the file/folder to use only letters, numbers, '_' and '-'.");
            return;
        }

        String newWorldName = WorldNames.ROOT + cleanWorldName;
        String worldName = newWorldName + "/" + WorldNames.OVERWORLD;
        File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);
        if (newWorldFolder.exists() || Bukkit.getWorld(worldName) != null || IN_PROGRESS.contains(newWorldName)) {
            sender.sendMessage(ChatColor.RED + "A world named " + cleanWorldName + " already exists.");
            return;
        }
        File target = new File(newWorldFolder, WorldNames.OVERWORLD);

        IN_PROGRESS.add(newWorldName);
        sender.sendMessage("Importing " + fileName + "...");
        // Copying a world can take a while, so do it off the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String error = null;
            try {
                if (isZip) {
                    if (!unzipAndCopyWorldRoot(worldFile, target)) {
                        error = "Failed to find or extract world data from the zip file (no level.dat).";
                    }
                } else {
                    File worldRootFolder = findWorldRootFolder(worldFile);
                    if (worldRootFolder == null) {
                        error = "level.dat not found in the provided folder.";
                    } else {
                        copyFolder(worldRootFolder.toPath(), target.toPath());
                    }
                }
            } catch (IOException | RuntimeException e) {
                error = "Failed to import world: " + e.getMessage();
                plugin.getLogger().log(Level.SEVERE, "Failed to import world " + fileName, e);
            }
            if (error != null) {
                deleteFolder(newWorldFolder.toPath());
            }

            String finalError = error;
            // Worlds can only be loaded on the main thread.
            Bukkit.getScheduler().runTask(plugin, () -> {
                IN_PROGRESS.remove(newWorldName);
                if (finalError != null) {
                    sender.sendMessage(finalError);
                    return;
                }
                World world = null;
                try {
                    world = Bukkit.createWorld(new WorldCreator(worldName));
                } catch (RuntimeException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load imported world " + worldName, e);
                }
                if (world == null) {
                    sender.sendMessage("Failed to create the world " + worldName + ".");
                    deleteFolder(newWorldFolder.toPath());
                    return;
                }
                sender.sendMessage("World " + cleanWorldName + " has been successfully imported to " + worldName);

                // Update config.yml
                List<String> worlds = plugin.getConfig().getStringList("worlds");
                if (!worlds.contains(worldName)) {
                    worlds.add(worldName);
                }
                plugin.getConfig().set("worlds", worlds);
                plugin.saveConfig();
            });
        });
    }

    private static File findWorldRootFolder(File folder) {
        File levelDat = new File(folder, "level.dat");
        if (levelDat.exists()) {
            return folder;
        }

        File[] subFolders = folder.listFiles(File::isDirectory);
        if (subFolders != null) {
            Arrays.sort(subFolders);
            for (File subFolder : subFolders) {
                File found = findWorldRootFolder(subFolder);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static boolean unzipAndCopyWorldRoot(File zipFile, File destDir) throws IOException {
        File cacheFolder = new File(CoManyWorld.getInstance().getDataFolder(), "import/cache");
        cacheFolder.mkdirs();
        Path tempDir = Files.createTempDirectory(cacheFolder.toPath(), "temp");

        try {
            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    Path newFile = newFile(tempDir, zipEntry);
                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(newFile);
                    } else {
                        Files.createDirectories(newFile.getParent());
                        Files.copy(zis, newFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            File worldRoot = findWorldRootFolder(tempDir.toFile());
            if (worldRoot == null) {
                return false;
            }
            copyFolder(worldRoot.toPath(), destDir.toPath());
            return true;
        } finally {
            deleteFolder(tempDir);
        }
    }

    /** Protects against "zip slip" entries such as ../../plugins/evil.jar */
    private static Path newFile(Path tempDir, ZipEntry zipEntry) throws IOException {
        Path destinationDir = tempDir.toAbsolutePath().normalize();
        Path destFile = destinationDir.resolve(zipEntry.getName()).normalize();
        if (!destFile.startsWith(destinationDir)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    private static void copyFolder(Path source, Path destination) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                Path target = destination.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else if (!SKIPPED_FILES.contains(path.getFileName().toString())) {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteFolder(Path folder) {
        if (!Files.exists(folder)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            CoManyWorld.getInstance().getLogger().log(Level.WARNING, "Failed to delete " + folder, e);
        }
    }
}
