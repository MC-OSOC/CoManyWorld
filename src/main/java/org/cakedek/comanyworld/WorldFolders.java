package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Where the files of a world are on disk. This depends on the server:
 * <pre>
 * Spigot, Paper 1.21 and older:  &lt;server&gt;/many_world/foo/world
 * Paper 26.x:                    &lt;server&gt;/world/dimensions/minecraft/many_world/foo/world
 * </pre>
 * ("world" being the main world, level-name in server.properties.)
 * Paper 26.x moves a world from the old place to the new one by itself the first time it loads it,
 * so after an upgrade the files can be in either place.
 * <p>
 * Never trust a folder computed from a name before checking it with {@link #managedFolders(String)}
 * when it is about to be deleted.
 */
public final class WorldFolders {

    private static final String DIMENSIONS = "dimensions/minecraft";

    private WorldFolders() {
    }

    /** &lt;server&gt;/&lt;name&gt; */
    public static File legacyFolder(String worldName) {
        return new File(Bukkit.getWorldContainer(), worldName);
    }

    /** &lt;server&gt;/&lt;main world&gt;/dimensions/minecraft/&lt;name&gt;, or null before the main world is loaded. */
    public static File dimensionFolder(String worldName) {
        File root = dimensionsRoot();
        return root == null ? null : new File(root, worldName.toLowerCase(Locale.ROOT));
    }

    private static File dimensionsRoot() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            return null;
        }
        return new File(new File(Bukkit.getWorldContainer(), worlds.get(0).getName()), DIMENSIONS);
    }

    /**
     * True on servers that keep every world inside the main world's dimensions folder (Paper 26.x).
     * Decided from where the server put the worlds it already loaded (the main Nether and End);
     * only if there are none, from the server version.
     */
    public static boolean usesDimensionFolders() {
        File root = dimensionsRoot();
        if (root == null) {
            return false;
        }
        Path dimensions = normalize(root);
        Path mainWorld = normalize(new File(Bukkit.getWorldContainer(), Bukkit.getWorlds().get(0).getName()));
        List<World> worlds = Bukkit.getWorlds();
        // The main world itself is skipped: on Spigot 26.x its own data is in world/dimensions too.
        for (int i = 1; i < worlds.size(); i++) {
            Path folder = normalize(worlds.get(i).getWorldFolder());
            if (folder.startsWith(dimensions)) {
                return true;
            }
            if (!folder.startsWith(mainWorld)) {
                return false; // e.g. <server>/world_nether
            }
        }
        return Platform.isPaper() && Platform.minecraftMajorVersion() >= 26;
    }

    /** Both possible folders, the one this server uses for new worlds first. */
    public static List<File> candidates(String worldName) {
        List<File> result = new ArrayList<>(2);
        File dimension = dimensionFolder(worldName);
        if (dimension != null && usesDimensionFolders()) {
            result.add(dimension);
            result.add(legacyFolder(worldName));
        } else {
            result.add(legacyFolder(worldName));
            if (dimension != null) {
                result.add(dimension);
            }
        }
        return result;
    }

    /** Where a new world with this name will be stored. */
    public static File newWorldFolder(String worldName) {
        return candidates(worldName).get(0);
    }

    /** True if anything exists on disk under this name, in either place. */
    public static boolean existsOnDisk(String worldName) {
        return candidates(worldName).stream().anyMatch(File::exists);
    }

    /** The folders that contain all worlds of this plugin: &lt;server&gt;/many_world and .../dimensions/minecraft/many_world */
    public static List<File> managedRoots() {
        List<File> roots = new ArrayList<>(2);
        roots.add(legacyFolder(WorldNames.ROOT));
        File dimension = dimensionFolder(WorldNames.ROOT);
        if (dimension != null) {
            roots.add(dimension);
        }
        return roots;
    }

    /**
     * True if the name is a world of this plugin and none of its possible folders lead outside
     * the plugin's folders (e.g. "many_world/../world" or a symbolic link).
     */
    public static boolean isSafeManagedName(String worldName) {
        if (!WorldNames.isManaged(worldName)) {
            return false;
        }
        for (File folder : candidates(worldName)) {
            if (rootOf(folder) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * The existing folders of a managed world, for deleting it. Empty if the world has no files any more,
     * or if the name is not safe (see {@link #isSafeManagedName(String)}).
     */
    public static List<File> managedFolders(String worldName) {
        List<File> result = new ArrayList<>(2);
        if (!isSafeManagedName(worldName)) {
            return result;
        }
        for (File folder : candidates(worldName)) {
            if (folder.exists()) {
                result.add(canonical(folder));
            }
        }
        return result;
    }

    /**
     * The managed root a folder is strictly inside of, or null if it is outside all of them
     * (or is a root itself). Symbolic links are resolved first.
     */
    public static File rootOf(File folder) {
        return rootOf(folder, managedRoots());
    }

    /** Like {@link #rootOf(File)} with roots from {@link #managedRoots()}; safe to call off the main thread. */
    public static File rootOf(File folder, List<File> roots) {
        File canonicalFolder = canonical(folder);
        if (canonicalFolder == null) {
            return null;
        }
        for (File root : roots) {
            File canonicalRoot = canonical(root);
            if (canonicalRoot != null && canonicalFolder.toPath().startsWith(canonicalRoot.toPath())
                    && !canonicalFolder.equals(canonicalRoot)) {
                return canonicalRoot;
            }
        }
        return null;
    }

    private static File canonical(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
    }

    private static Path normalize(File file) {
        return file.toPath().toAbsolutePath().normalize();
    }

    /** Short description for /co-many about. */
    public static String describe() {
        return usesDimensionFolders()
                ? Bukkit.getWorlds().get(0).getName() + "/" + DIMENSIONS + "/" + WorldNames.ROOT
                : WorldNames.ROOT;
    }
}
