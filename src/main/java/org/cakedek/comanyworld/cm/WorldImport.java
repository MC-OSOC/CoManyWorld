package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.cakedek.comanyworld.CoManyWorld;
import org.cakedek.comanyworld.WorldFolders;
import org.cakedek.comanyworld.WorldNames;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Imports a world folder or .zip from plugins/CoManyWorld/import as many_world/&lt;name&gt;/world.
 * <p>
 * Two kinds of world data are recognised:
 * <ul>
 *   <li>a folder with level.dat: single player saves, Spigot, Paper 1.21 and older
 *       (and backups made there). Copied to &lt;server&gt;/many_world/...; Paper 26.x moves it into
 *       its dimensions folder by itself when the world is loaded.</li>
 *   <li>a Paper 26.x world folder (data/paper/level_overrides.dat, no level.dat), e.g. a backup made
 *       with /co-many backup on Paper 26.x. Copied straight into the dimensions folder; only possible
 *       on a server that uses that layout.</li>
 * </ul>
 */
public class WorldImport {

    /**
     * Files that must not be copied: session.lock belongs to whoever had the world open, and the
     * world's id (uid.dat, or data/paper/metadata.dat on Paper 26.x) makes the server refuse to load a
     * second copy of the same world.
     */
    private static final Set<String> SKIPPED_FILES = new HashSet<>(Arrays.asList("session.lock", "uid.dat"));
    private static final String PAPER_METADATA = "data/paper/metadata.dat";
    /** Disk space that must stay free after an import. */
    private static final long SPARE_BYTES = 512L * 1024 * 1024;
    private static final int MAX_SEARCH_DEPTH = 8;
    private static final Set<String> IN_PROGRESS = new HashSet<>();

    private enum Format { LEVEL_DAT, PAPER_DIMENSION }

    private record Found(File folder, Format format) {
    }

    /** True while a world group is being imported (main thread only). */
    public static boolean isImporting(String groupName) {
        return IN_PROGRESS.contains(groupName);
    }

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
        if (Files.isSymbolicLink(worldFile.toPath())) {
            sender.sendMessage(ChatColor.RED + "Symbolic links are not supported. Copy the world into the import folder.");
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

        String groupName = WorldNames.ROOT + cleanWorldName;
        String worldName = groupName + "/" + WorldNames.OVERWORLD;
        if (WorldFolders.existsOnDisk(groupName) || Bukkit.getWorld(worldName) != null || IN_PROGRESS.contains(groupName)
                || CreateWorld.isCreating(groupName)
                || plugin.getConfig().getStringList("worlds").stream().anyMatch(worldName::equalsIgnoreCase)) {
            sender.sendMessage(ChatColor.RED + "A world named " + cleanWorldName + " already exists.");
            return;
        }
        // Worked out here, on the main thread, because it asks the server about its worlds.
        File levelDatTarget = WorldFolders.legacyFolder(worldName);
        File dimensionTarget = WorldFolders.usesDimensionFolders() ? WorldFolders.dimensionFolder(worldName) : null;
        // The group folders don't exist yet (checked above), so on failure they can be removed entirely.
        List<File> createdGroups = new ArrayList<>();
        createdGroups.add(WorldFolders.legacyFolder(groupName));
        if (WorldFolders.dimensionFolder(groupName) != null) {
            createdGroups.add(WorldFolders.dimensionFolder(groupName));
        }

        IN_PROGRESS.add(groupName);
        sender.sendMessage("Importing " + fileName + "...");
        // Copying a world can take a while, so do it off the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String error = null;
            try {
                error = isZip
                        ? copyFromZip(worldFile, levelDatTarget, dimensionTarget)
                        : copyFromFolder(worldFile, levelDatTarget, dimensionTarget);
            } catch (IOException | RuntimeException e) {
                error = "Failed to import world: " + e.getMessage();
                plugin.getLogger().log(Level.SEVERE, "Failed to import world " + fileName, e);
            }
            if (error != null) {
                createdGroups.forEach(folder -> deleteFolder(folder.toPath()));
            }

            String finalError = error;
            // Worlds can only be loaded on the main thread.
            Bukkit.getScheduler().runTask(plugin, () -> {
                IN_PROGRESS.remove(groupName);
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
                    createdGroups.forEach(folder -> deleteFolder(folder.toPath()));
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

    /** Returns an error message, or null when the world was copied. */
    private static String copyFromFolder(File source, File levelDatTarget, File dimensionTarget) throws IOException {
        Found found = findWorldRoot(source, 0);
        if (found == null) {
            return "level.dat not found in the provided folder (and it is not a Paper 26 world folder either).";
        }
        File target = targetFor(found, levelDatTarget, dimensionTarget);
        if (target == null) {
            return paperOnlyMessage();
        }
        long size = sizeOf(found.folder().toPath());
        if (size > freeSpace(target)) {
            return "Not enough free disk space to import this world (" + (size / (1024 * 1024)) + " MB).";
        }
        copyFolder(found.folder().toPath(), target.toPath(), found.format());
        return null;
    }

    private static String copyFromZip(File zipFile, File levelDatTarget, File dimensionTarget) throws IOException {
        File cacheFolder = new File(CoManyWorld.getInstance().getDataFolder(), "import/cache");
        cacheFolder.mkdirs();
        Path tempDir = Files.createTempDirectory(cacheFolder.toPath(), "temp");
        try {
            // The world is unpacked here and then copied once more, so it may use half of the free space.
            unzip(zipFile, tempDir, freeSpace(cacheFolder) / 2);
            Found found = findWorldRoot(tempDir.toFile(), 0);
            if (found == null) {
                return "Failed to find or extract world data from the zip file (no level.dat, and not a Paper 26 world folder).";
            }
            File target = targetFor(found, levelDatTarget, dimensionTarget);
            if (target == null) {
                return paperOnlyMessage();
            }
            copyFolder(found.folder().toPath(), target.toPath(), found.format());
            return null;
        } finally {
            deleteFolder(tempDir);
        }
    }

    private static File targetFor(Found found, File levelDatTarget, File dimensionTarget) {
        return found.format() == Format.LEVEL_DAT ? levelDatTarget : dimensionTarget;
    }

    private static String paperOnlyMessage() {
        return "This world was saved by Paper 26 or newer (it has no level.dat), so it can only be imported on such a server.";
    }

    private static Found findWorldRoot(File folder, int depth) {
        if (new File(folder, "level.dat").isFile()) {
            return new Found(folder, Format.LEVEL_DAT);
        }
        if (new File(folder, "data/paper/level_overrides.dat").isFile()) {
            return new Found(folder, Format.PAPER_DIMENSION);
        }
        if (depth >= MAX_SEARCH_DEPTH) {
            return null;
        }
        // Symbolic links are not followed: they could point anywhere on the machine, or in a circle.
        File[] subFolders = folder.listFiles(file -> file.isDirectory() && !Files.isSymbolicLink(file.toPath()));
        if (subFolders != null) {
            Arrays.sort(subFolders);
            for (File subFolder : subFolders) {
                Found found = findWorldRoot(subFolder, depth + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Unzips into tempDir, refusing entries outside it and stopping once more than maxBytes were written. */
    private static void unzip(File zipFile, Path tempDir, long maxBytes) throws IOException {
        long written = 0;
        byte[] buffer = new byte[64 * 1024];
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Path newFile = newFile(tempDir, zipEntry);
                if (newFile == null) {
                    continue; // an entry for the zip's root itself, e.g. "./"
                }
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newFile);
                } else {
                    Files.createDirectories(newFile.getParent());
                    try (OutputStream out = Files.newOutputStream(newFile)) {
                        int read;
                        while ((read = zis.read(buffer)) > 0) {
                            written += read;
                            if (written > maxBytes) {
                                throw new IOException("the zip file unpacks to more than the free disk space allows");
                            }
                            out.write(buffer, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /** Protects against "zip slip" entries such as ../../plugins/evil.jar. Null for the root itself. */
    private static Path newFile(Path tempDir, ZipEntry zipEntry) throws IOException {
        Path destinationDir = tempDir.toAbsolutePath().normalize();
        Path destFile = destinationDir.resolve(zipEntry.getName()).normalize();
        if (!destFile.startsWith(destinationDir)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile.equals(destinationDir) ? null : destFile;
    }

    private static void copyFolder(Path source, Path destination, Format format) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : (Iterable<Path>) walk::iterator) {
                if (Files.isSymbolicLink(path)) {
                    continue; // never copy what a link points to
                }
                String relative = source.relativize(path).toString().replace('\\', '/');
                Path target = destination.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else if (!SKIPPED_FILES.contains(path.getFileName().toString())
                        && !(format == Format.PAPER_DIMENSION && relative.equals(PAPER_METADATA))) {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static long sizeOf(Path folder) throws IOException {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .mapToLong(path -> path.toFile().length())
                    .sum();
        }
    }

    /** Free space on the disk the file will be written to, minus what must stay free. */
    private static long freeSpace(File file) {
        File existing = file.getAbsoluteFile();
        while (existing != null && !existing.exists()) {
            existing = existing.getParentFile();
        }
        long usable = existing == null ? 0 : existing.getUsableSpace();
        return Math.max(0, usable - SPARE_BYTES);
    }

    private static void deleteFolder(Path folder) {
        if (!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            CoManyWorld.getInstance().getLogger().log(Level.WARNING, "Failed to delete " + folder, e);
        }
    }
}
