package org.cakedek.comanyworld.cm;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.cakedek.comanyworld.CoManyWorld;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WorldImport implements Listener {

    public static void importWorld(CommandSender sender, String worldName) {
        File importFolder = new File(CoManyWorld.getInstance().getDataFolder(), "import");
        if (!importFolder.exists()) {
            importFolder.mkdirs();
            sender.sendMessage("Created import folder: " + importFolder.getAbsolutePath());
            return;
        }

        File worldFile = new File(importFolder, worldName);
        if (!worldFile.exists()) {
            sender.sendMessage("World file or folder not found: " + worldFile.getAbsolutePath());
            return;
        }

        String cleanWorldName = worldName.toLowerCase().endsWith(".zip")
                ? worldName.substring(0, worldName.length() - 4)
                : worldName;

        String newWorldName = "many_world/" + cleanWorldName;
        File newWorldFolder = new File(Bukkit.getWorldContainer(), newWorldName);

        try {
            File worldRootFolder;
            if (worldFile.isDirectory()) {
                worldRootFolder = findWorldRootFolder(worldFile);
                if (worldRootFolder == null) {
                    sender.sendMessage("level.dat not found in the provided folder.");
                    return;
                }
                copyFolder(worldRootFolder, new File(newWorldFolder, "world"));
            } else if (worldFile.getName().endsWith(".zip")) {
                worldRootFolder = unzipAndFindWorldRoot(worldFile, newWorldFolder);
                if (worldRootFolder == null) {
                    sender.sendMessage("Failed to find or extract world data from the zip file.");
                    return;
                }
            } else {
                sender.sendMessage("Unsupported file type. Please use a folder or a zip file.");
                return;
            }

            WorldCreator creator = new WorldCreator(newWorldName + "/world");
            World world = Bukkit.createWorld(creator);
            if (world != null) {
                sender.sendMessage("World " + cleanWorldName + " has been successfully imported to " + newWorldName + "/world");

                // Update config.yml
                List<String> worlds = CoManyWorld.getInstance().getConfig().getStringList("worlds");
                worlds.add(newWorldName + "/world");
                CoManyWorld.getInstance().getConfig().set("worlds", worlds);
                CoManyWorld.getInstance().saveConfig();
            } else {
                sender.sendMessage("Failed to create the world " + newWorldName + "/world.");
                deleteFolder(newWorldFolder);
            }
        } catch (IOException e) {
            sender.sendMessage("Failed to import world: " + e.getMessage());
            e.printStackTrace();
            deleteFolder(newWorldFolder);
        }
    }

    private static File findWorldRootFolder(File folder) {
        File levelDat = new File(folder, "level.dat");
        if (levelDat.exists()) {
            return folder;
        }

        File[] subFolders = folder.listFiles(File::isDirectory);
        if (subFolders != null) {
            for (File subFolder : subFolders) {
                File found = findWorldRootFolder(subFolder);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static File unzipAndFindWorldRoot(File zipFile, File destDir) throws IOException {
        File cacheFolder = new File(CoManyWorld.getInstance().getDataFolder(), "import/cache");
        cacheFolder.mkdirs();
        File tempDir = Files.createTempDirectory(cacheFolder.toPath(), "temp").toFile();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[1024];
            while (zipEntry != null) {
                File newFile = newFile(tempDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        File worldRoot = findWorldRootFolder(tempDir);
        if (worldRoot != null) {
            copyFolder(worldRoot, new File(destDir, "world"));
            deleteFolder(tempDir);
            return new File(destDir, "world");
        }

        deleteFolder(tempDir);
        return null;
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    private static void copyFolder(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyFolder(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files != null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}