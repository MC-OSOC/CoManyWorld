package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Naming rules for worlds managed by this plugin.
 * <pre>
 * many_world/&lt;name&gt;/world
 * many_world/&lt;name&gt;/world_nether
 * many_world/&lt;name&gt;/world_the_end
 * </pre>
 * Versions before 1.1.0 created "nether" and "the_end" folders, so those names are still recognised.
 */
public final class WorldNames {

    public static final String ROOT = "many_world/";
    public static final String OVERWORLD = "world";
    public static final String NETHER = "world_nether";
    public static final String THE_END = "world_the_end";
    private static final String LEGACY_NETHER = "nether";
    private static final String LEGACY_THE_END = "the_end";

    /**
     * Allowed characters for a world name typed by a user. Minecraft lower-cases world keys and only
     * allows [a-z0-9/._-] in them, and "/" or ".." would let the name escape the many_world folder.
     */
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,48}");

    private WorldNames() {
    }

    /** Lower-cases the name and returns it, or returns null if it contains characters that aren't allowed. */
    public static String sanitize(String input) {
        String name = input.toLowerCase(Locale.ROOT);
        return VALID_NAME.matcher(name).matches() ? name : null;
    }

    public static boolean isManaged(String worldName) {
        return worldName.toLowerCase(Locale.ROOT).startsWith(ROOT);
    }

    /** "many_world/foo/world_nether" -> "many_world/foo/" */
    public static String groupPrefix(String worldName) {
        int slash = worldName.lastIndexOf('/');
        return slash < 0 ? "" : worldName.substring(0, slash + 1);
    }

    public static World.Environment environmentOf(String worldName) {
        String lower = worldName.toLowerCase(Locale.ROOT);
        if (lower.endsWith("nether")) {
            return World.Environment.NETHER;
        }
        if (lower.endsWith("the_end")) {
            return World.Environment.THE_END;
        }
        return World.Environment.NORMAL;
    }

    public static World overworldOf(String prefix) {
        return Bukkit.getWorld(prefix + OVERWORLD);
    }

    public static World netherOf(String prefix) {
        World world = Bukkit.getWorld(prefix + NETHER);
        return world != null ? world : Bukkit.getWorld(prefix + LEGACY_NETHER);
    }

    public static World endOf(String prefix) {
        World world = Bukkit.getWorld(prefix + THE_END);
        return world != null ? world : Bukkit.getWorld(prefix + LEGACY_THE_END);
    }

    /**
     * Returns the folder of a managed world, or null if the path would point outside
     * the many_world folder (e.g. a hand-edited config containing "../").
     */
    public static File managedFolder(String worldName) {
        try {
            File root = new File(Bukkit.getWorldContainer(), ROOT).getCanonicalFile();
            File folder = new File(Bukkit.getWorldContainer(), worldName).getCanonicalFile();
            return folder.toPath().startsWith(root.toPath()) && !folder.equals(root) ? folder : null;
        } catch (IOException e) {
            return null;
        }
    }
}
