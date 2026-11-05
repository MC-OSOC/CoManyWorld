package org.cakedek.comanyworld;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Locale;

/**
 * MockBukkit server that behaves a bit more like a real one for this plugin:
 * worlds have real folders on disk, world names are case-insensitive, and createWorld returns
 * an already loaded world.
 * <p>
 * Two layouts, like the real servers (checked on GitHub Actions, see .github/workflows/server-test.yml):
 * <ul>
 *   <li>Spigot / Paper 1.21: &lt;server&gt;/&lt;name&gt; with level.dat, region/, session.lock, uid.dat</li>
 *   <li>Paper 26.x ({@code dimensionFolders}): &lt;server&gt;/world/dimensions/minecraft/&lt;name&gt; with
 *       data/paper/level_overrides.dat, data/paper/metadata.dat (the world's id) and region/, but no level.dat.
 *       The main Nether and End are the_nether and the_end there. An old-layout folder &lt;server&gt;/&lt;name&gt;
 *       is moved into place when the world is loaded, like Paper's LegacyCraftBukkitWorldMigration.</li>
 * </ul>
 */
public class TestServer extends ServerMock {

    private final File container;
    private final boolean dimensionFolders;

    public TestServer(File container, boolean dimensionFolders) {
        this.container = container;
        this.dimensionFolders = dimensionFolders;
        container.mkdirs();
    }

    @Override
    public @NotNull File getWorldContainer() {
        return container;
    }

    @Override
    public World getWorld(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return getWorlds().stream().filter(w -> w.getName().toLowerCase(Locale.ROOT).equals(lower)).findFirst().orElse(null);
    }

    @Override
    public World createWorld(@NotNull WorldCreator creator) {
        World existing = getWorld(creator.name());
        if (existing != null) {
            return existing;
        }
        String name = creator.name();
        boolean dimension = dimensionFolders && !name.equals("world");
        File folder = new File(container, name);
        if (dimension) {
            String key = switch (name) {
                case "world_nether" -> "the_nether";
                case "world_the_end" -> "the_end";
                default -> name.toLowerCase(Locale.ROOT);
            };
            File legacy = folder;
            folder = new File(container, "world/dimensions/minecraft/" + key);
            if (legacy.isDirectory() && !folder.exists()) {
                try {
                    Files.createDirectories(folder.getParentFile().toPath());
                    Files.move(legacy.toPath(), folder.toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        DiskWorld world = new DiskWorld(creator, folder, dimension);
        addWorld(world);
        return world;
    }

    public static class DiskWorld extends WorldMock {
        private final File folder;
        public int saveCount = 0;

        DiskWorld(WorldCreator creator, File folder, boolean dimension) {
            super(creator);
            this.folder = folder;
            try {
                Files.createDirectories(folder.toPath().resolve("region"));
                if (!new File(folder, "region/r.0.0.mca").exists()) {
                    Files.write(folder.toPath().resolve("region/r.0.0.mca"), new byte[64 * 1024]);
                }
                // A real server keeps the files that exist (imported world) and writes these on load.
                // An existing world id is kept too, so a copied one would make the world a duplicate.
                if (dimension) {
                    Files.createDirectories(folder.toPath().resolve("data/paper"));
                    writeIfMissing(new File(folder, "data/paper/level_overrides.dat"), "level of " + creator.name());
                    writeIfMissing(new File(folder, "data/paper/metadata.dat"), getUID().toString());
                } else {
                    writeIfMissing(new File(folder, "level.dat"), "level of " + creator.name());
                    Files.writeString(folder.toPath().resolve("session.lock"), "lock");
                    writeIfMissing(new File(folder, "uid.dat"), getUID().toString());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static void writeIfMissing(File file, String content) throws IOException {
            if (!file.exists()) {
                Files.writeString(file.toPath(), content);
            }
        }

        @Override
        public @NotNull File getWorldFolder() {
            return folder;
        }

        @Override
        public void save() {
            saveCount++;
        }
    }
}
