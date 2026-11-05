package org.cakedek.comanyworld;

import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

/**
 * How the overworld of a managed world is generated: a vanilla variant (flat, large biomes, amplified),
 * an empty void world, and/or a generator provided by another plugin.
 * The choice is kept in config.yml (section {@value #SECTION}) because the server needs it again
 * every time the world is loaded on startup.
 * <pre>
 * world-settings:
 *   many_world/lobby/world:
 *     type: void
 *   many_world/custom/world:
 *     generator: MyGenerator:myid
 * </pre>
 */
public final class WorldOptions {

    public enum Kind {
        NORMAL, FLAT, LARGE, AMPLIFIED, VOID;

        /** The create flag for this kind, e.g. "-flat". */
        public String flag() {
            return "-" + name().toLowerCase(Locale.ROOT);
        }
    }

    static final String SECTION = "world-settings";
    private static final String TYPE = "type";
    private static final String GENERATOR = "generator";

    private final Kind kind;
    /** "Plugin" or "Plugin:id" as accepted by {@link WorldCreator#generator(String)}, or null. */
    private final String generator;

    public WorldOptions(Kind kind, String generator) {
        this.kind = kind == null ? Kind.NORMAL : kind;
        this.generator = generator == null || generator.isBlank() ? null : generator;
    }

    public static WorldOptions defaults() {
        return new WorldOptions(Kind.NORMAL, null);
    }

    /** "-flat" -> FLAT, "-void" -> VOID ...; null if the argument isn't one of the world kind flags. */
    public static Kind kindOfFlag(String flag) {
        for (Kind kind : Kind.values()) {
            if (kind != Kind.NORMAL && kind.flag().equalsIgnoreCase(flag)) {
                return kind;
            }
        }
        return null;
    }

    public Kind kind() {
        return kind;
    }

    public String generator() {
        return generator;
    }

    public boolean isDefault() {
        return kind == Kind.NORMAL && generator == null;
    }

    /** Name of the plugin in {@link #generator()}, or null. */
    public String generatorPlugin() {
        return generator == null ? null : splitGenerator(generator)[0];
    }

    /** "Plugin:id" -> {"Plugin", "id"}; "Plugin" -> {"Plugin", null}. */
    static String[] splitGenerator(String generator) {
        int colon = generator.indexOf(':');
        return colon < 0
                ? new String[]{generator, null}
                : new String[]{generator.substring(0, colon), generator.substring(colon + 1)};
    }

    /** Applies these options to the creator of an overworld ({@code World.Environment.NORMAL}). */
    public void applyTo(WorldCreator creator) {
        switch (kind) {
            case FLAT:
                creator.type(WorldType.FLAT);
                break;
            case LARGE:
                creator.type(WorldType.LARGE_BIOMES);
                break;
            case AMPLIFIED:
                creator.type(WorldType.AMPLIFIED);
                break;
            case VOID:
                creator.generator(new VoidGenerator());
                creator.generateStructures(false);
                break;
            default:
                break;
        }
        if (generator != null) {
            creator.generator(generator);
        }
    }

    /** Short text for messages, e.g. "flat" or "generator MyPlugin:id". */
    public String describe() {
        if (generator != null) {
            return "generator " + generator;
        }
        return kind.name().toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- config.yml

    /** Reads the options saved for a world. Worlds without an entry (or with an unknown type) are normal. */
    public static WorldOptions load(FileConfiguration config, String worldName) {
        ConfigurationSection section = config.getConfigurationSection(SECTION);
        if (section == null) {
            return defaults();
        }
        ConfigurationSection world = section.getConfigurationSection(worldName);
        if (world == null) {
            return defaults();
        }
        Kind kind = Kind.NORMAL;
        String type = world.getString(TYPE);
        if (type != null) {
            Kind parsed = kindOfFlag("-" + type);
            if (parsed != null) {
                kind = parsed;
            }
        }
        return new WorldOptions(kind, world.getString(GENERATOR));
    }

    /** Saves the options for a world (in memory; the caller saves the config). Default options remove the entry. */
    public void save(FileConfiguration config, String worldName) {
        String path = SECTION + "." + worldName;
        if (isDefault()) {
            config.set(path, null);
            return;
        }
        config.set(path + "." + TYPE, kind == Kind.NORMAL ? null : kind.name().toLowerCase(Locale.ROOT));
        config.set(path + "." + GENERATOR, generator);
    }

    /** Forgets the options of a world (in memory; the caller saves the config). */
    public static void remove(FileConfiguration config, String worldName) {
        config.set(SECTION + "." + worldName, null);
    }
}
