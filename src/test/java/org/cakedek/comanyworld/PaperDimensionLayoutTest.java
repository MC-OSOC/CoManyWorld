package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Paper 26.x keeps every world in world/dimensions/minecraft/&lt;name&gt; instead of &lt;server&gt;/&lt;name&gt;.
 * Before 1.1.0 the plugin only looked in &lt;server&gt;/many_world, so on Paper 26.x worlds were skipped
 * after a restart, imports loaded an empty world and clear deleted nothing.
 */
class PaperDimensionLayoutTest extends TestBase {

    @Override
    protected boolean dimensionFolders() {
        return true;
    }

    /** world/dimensions/minecraft/&lt;name&gt; */
    private File dimension(String worldName) {
        return serverFile("world/dimensions/minecraft/" + worldName);
    }

    private void loadWorldsOnStartup() throws Exception {
        Method load = CoManyWorld.class.getDeclaredMethod("loadWorldsOnStartup");
        load.setAccessible(true);
        load.invoke(plugin);
    }

    @Test
    void layoutIsDetected() {
        assertTrue(WorldFolders.usesDimensionFolders());
        assertEquals(dimension("many_world/a/world"), WorldFolders.newWorldFolder("many_world/a/world"));
        String about = run("co-many about");
        assertTrue(about.contains("World folders >>> world/dimensions/minecraft/many_world/"), about);
    }

    @Test
    void createPutsWorldsInTheDimensionsFolder() {
        createAll("test");
        for (String name : List.of("world", "world_nether", "world_the_end")) {
            World world = Bukkit.getWorld("many_world/test/" + name);
            assertNotNull(world, name);
            assertEquals(dimension("many_world/test/" + name), world.getWorldFolder());
        }
        assertFalse(serverFile("many_world").exists(), "nothing may be created in the old place");
        assertTrue(run("co-many create test -11").contains("already exists"));
    }

    @Test
    void startupLoadsWorldsFromBothPlaces() throws Exception {
        // Created on Paper 26.x
        Files.createDirectories(dimension("many_world/new/world/data/paper").toPath());
        Files.writeString(dimension("many_world/new/world/data/paper/level_overrides.dat").toPath(), "new");
        // Created before the upgrade: Paper moves it into the dimensions folder when it is loaded
        Files.createDirectories(serverFile("many_world/old/world").toPath());
        Files.writeString(serverFile("many_world/old/world/level.dat").toPath(), "old");
        plugin.getConfig().set("worlds", List.of("many_world/new/world", "many_world/old/world", "many_world/gone/world"));

        loadWorldsOnStartup();

        assertEquals("new", Files.readString(dimension("many_world/new/world/data/paper/level_overrides.dat").toPath()));
        assertNotNull(Bukkit.getWorld("many_world/new/world"));
        assertNotNull(Bukkit.getWorld("many_world/old/world"));
        assertEquals("old", Files.readString(dimension("many_world/old/world/level.dat").toPath()));
        assertNull(Bukkit.getWorld("many_world/gone/world"), "a missing world must not be regenerated");
        assertFalse(dimension("many_world/gone").exists());
    }

    @Test
    void worldsComeBackAfterPaperMovedThem() throws Exception {
        // The bug: after Paper moved an old world, the next start looked only in the old place.
        Files.createDirectories(serverFile("many_world/old/world").toPath());
        Files.writeString(serverFile("many_world/old/world/level.dat").toPath(), "old");
        plugin.getConfig().set("worlds", List.of("many_world/old/world"));
        loadWorldsOnStartup();
        assertFalse(serverFile("many_world/old/world").exists());
        assertTrue(Bukkit.unloadWorld(Bukkit.getWorld("many_world/old/world"), true));
        assertNull(Bukkit.getWorld("many_world/old/world"));

        loadWorldsOnStartup(); // next server start
        assertNotNull(Bukkit.getWorld("many_world/old/world"));
    }

    @Test
    void deleteAndClearRemoveTheDimensionFolders() {
        createAll("test");
        run("co-many delete many_world/test/");
        server.dispatchCommand(console, "co-many clear confirm");
        awaitOutput("Deleted world folder: many_world/test/world_the_end");
        assertFalse(dimension("many_world/test").exists(), "worlds and their empty group folder are gone");
        assertTrue(dimension("many_world").exists());
        assertTrue(dimension("the_nether/data/paper/level_overrides.dat").exists(), "the server's Nether is untouched");
        assertTrue(trash().isEmpty());
        // The name can be used again
        run("co-many create test -11");
        server.getScheduler().performTicks(3);
        assertNotNull(Bukkit.getWorld("many_world/test/world"));
    }

    @Test
    void clearDeletesFilesLeftInBothPlaces() throws Exception {
        createAll("test");
        run("co-many delete many_world/test/world");
        Files.createDirectories(serverFile("many_world/test/world/region").toPath()); // a left-over old copy
        server.dispatchCommand(console, "co-many clear confirm");
        awaitOutput("Deleted world folder: many_world/test/world");
        assertFalse(dimension("many_world/test/world").exists());
        assertFalse(serverFile("many_world/test/world").exists());
        assertTrue(dimension("many_world/test/world_nether").exists(), "sibling worlds must survive");
    }

    @Test
    void clearNeverDeletesServerWorldsOrLoadedWorlds() throws Exception {
        createAll("test");
        YamlConfiguration yml = new YamlConfiguration();
        // "many_world/test" is the group of loaded worlds, "world_nether" lives in world/dimensions/minecraft/the_nether
        yml.set("delete_worlds", List.of("many_world/test", "world_nether", "many_world/../the_nether", "many_world/"));
        yml.save(new File(plugin.getDataFolder(), "delete_worlds.yml"));

        String out = run("co-many clear confirm");
        assertTrue(out.contains("Skipped many_world/test: the loaded world many_world/test/"), out);
        assertTrue(out.contains("Skipped world_nether"), out);
        assertTrue(out.contains("Skipped many_world/../the_nether"), out);
        assertTrue(out.contains("Skipped many_world/:"), out);
        server.getScheduler().performTicks(5);
        assertTrue(dimension("many_world/test/world/data/paper/level_overrides.dat").exists());
        assertTrue(dimension("the_nether/data/paper/level_overrides.dat").exists());
        assertEquals(4, trash().size(), "skipped entries stay in the trash");
    }

    @Test
    void backupOfAPaperWorldCanBeImportedAgain() throws Exception {
        createAll("test");
        run("co-many backup many_world/test/world");
        awaitOutput("backed up successfully");
        File[] zips = new File(plugin.getDataFolder(), "many_world_backup").listFiles((d, n) -> n.endsWith(".zip"));
        assertNotNull(zips);
        assertEquals(1, zips.length);
        try (ZipFile zip = new ZipFile(zips[0])) {
            Set<String> names = new HashSet<>();
            zip.stream().forEach(e -> names.add(e.getName()));
            assertTrue(names.contains("world/data/paper/level_overrides.dat"), names.toString());
            assertFalse(names.contains("world/level.dat"), names.toString());
        }

        Files.copy(zips[0].toPath(), new File(plugin.getDataFolder(), "import/restored.zip").toPath());
        run("co-many import restored.zip");
        String out = awaitOutput("successfully imported");
        World restored = Bukkit.getWorld("many_world/restored/world");
        assertNotNull(restored, out);
        Path folder = dimension("many_world/restored/world").toPath();
        assertEquals(folder.toFile(), restored.getWorldFolder());
        assertEquals("level of many_world/test/world", Files.readString(folder.resolve("data/paper/level_overrides.dat")));
        assertNotEquals(Files.readString(dimension("many_world/test/world/data/paper/metadata.dat").toPath()),
                Files.readString(folder.resolve("data/paper/metadata.dat")), "the world id must not be copied");
        assertFalse(serverFile("many_world/restored").exists());
        assertTrue(configWorlds().contains("many_world/restored/world"));
    }

    @Test
    void importOfALevelDatWorldIsMovedIntoPlaceByTheServer() throws Exception {
        Path source = plugin.getDataFolder().toPath().resolve("import/OldMap");
        Files.createDirectories(source.resolve("region"));
        Files.writeString(source.resolve("level.dat"), "my old map");
        Files.writeString(source.resolve("uid.dat"), "same-uid-as-original");

        run("co-many import OldMap");
        String out = awaitOutput("successfully imported");
        assertNotNull(Bukkit.getWorld("many_world/oldmap/world"), out);
        Path folder = dimension("many_world/oldmap/world").toPath();
        assertEquals("my old map", Files.readString(folder.resolve("level.dat")));
        assertFalse(Files.exists(folder.resolve("uid.dat")), "uid.dat must not be copied");
        assertFalse(serverFile("many_world/oldmap/world").exists());
        assertTrue(run("co-many import OldMap").contains("already exists"));
    }
}
