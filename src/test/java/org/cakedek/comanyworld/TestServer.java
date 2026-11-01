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
 * worlds have real folders on disk (level.dat, region files, session.lock, uid.dat),
 * world names are case-insensitive, and createWorld returns an already loaded world.
 */
public class TestServer extends ServerMock {

    private final File container;

    public TestServer(File container) {
        this.container = container;
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
        DiskWorld world = new DiskWorld(creator, new File(container, creator.name()));
        addWorld(world);
        return world;
    }

    public static class DiskWorld extends WorldMock {
        private final File folder;
        public int saveCount = 0;

        DiskWorld(WorldCreator creator, File folder) {
            super(creator);
            this.folder = folder;
            try {
                Files.createDirectories(folder.toPath().resolve("region"));
                // A real server keeps level.dat if it exists (imported world) and writes these on load
                if (!new File(folder, "level.dat").exists()) {
                    Files.writeString(folder.toPath().resolve("level.dat"), "level of " + creator.name());
                }
                if (!new File(folder, "region/r.0.0.mca").exists()) {
                    Files.write(folder.toPath().resolve("region/r.0.0.mca"), new byte[64 * 1024]);
                }
                Files.writeString(folder.toPath().resolve("session.lock"), "lock");
                // Like a real server: an existing uid.dat is kept (and a copied one would make it a duplicate world)
                if (!new File(folder, "uid.dat").exists()) {
                    Files.writeString(folder.toPath().resolve("uid.dat"), getUID().toString());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
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
