package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.command.MessageTarget;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class CoManyWorldTest {

    @TempDir
    Path tmp;

    private TestServer server;
    private CoManyWorld plugin;
    private ConsoleCommandSenderMock console;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new TestServer(tmp.resolve("server").toFile()));
        // The server's own worlds, loaded before plugins like on a real server
        server.createWorld(new WorldCreator("world"));
        server.createWorld(new WorldCreator("world_nether").environment(World.Environment.NETHER));
        server.createWorld(new WorldCreator("world_the_end").environment(World.Environment.THE_END));
        plugin = MockBukkit.load(CoManyWorld.class);
        console = (ConsoleCommandSenderMock) server.getConsoleSender();
        drain(console);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ---------------------------------------------------------------- helpers

    private static List<String> drain(MessageTarget target) {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = target.nextMessage()) != null) {
            messages.add(ChatColor.stripColor(message));
        }
        return messages;
    }

    private static String joined(List<String> messages) {
        return String.join("\n", messages);
    }

    /** Runs a console command and returns everything it printed. */
    private String run(String commandLine) {
        server.dispatchCommand(console, commandLine);
        return joined(drain(console));
    }

    /**
     * Ticks the server until the console has printed {@code expected} (or 10 s pass).
     * Background work (backup, import, clear) runs on MockBukkit's thread pool and reports back
     * on the main thread, so we wait for the result instead of guessing when the pool is idle.
     */
    private String awaitOutput(String expected) {
        StringBuilder all = new StringBuilder();
        long deadline = System.currentTimeMillis() + 10_000;
        while (true) {
            server.getScheduler().performTicks(1);
            for (String message : drain(console)) {
                all.append(message).append('\n');
            }
            if (all.toString().contains(expected)) {
                return all.toString();
            }
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for \"" + expected + "\". Output was:\n" + all);
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                fail(e);
            }
        }
    }

    private File folder(String worldName) {
        return new File(server.getWorldContainer(), worldName);
    }

    private List<String> configWorlds() {
        return plugin.getConfig().getStringList("worlds");
    }

    private List<String> trash() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "delete_worlds.yml"))
                .getStringList("delete_worlds");
    }

    private void createAll(String name) {
        run("co-many create " + name + " -all");
        server.getScheduler().performTicks(5);
    }

    private PlayerMock op() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        drain(player);
        return player;
    }

    // ---------------------------------------------------------------- plugin.yml / startup

    @Test
    void pluginDescriptorIsValid() {
        assertTrue(plugin.isEnabled());
        assertEquals("1.21", plugin.getDescription().getAPIVersion());
        assertFalse(plugin.getDescription().getVersion().contains("${"), "version must be filtered by Maven");
        assertInstanceOf(CommandHandler.class, plugin.getCommand("co-many").getExecutor());
        assertNotNull(plugin.getCommand("co-many-clear").getExecutor());
        assertTrue(Platform.isPaper(), "MockBukkit implements the Paper API, so teleportAsync should be found");
        // Version 1.0.5 registered the WorldManager listener three times
        long portalListeners = Arrays.stream(PlayerPortalEvent.getHandlerList().getRegisteredListeners())
                .filter(l -> l.getPlugin() == plugin).count();
        assertEquals(1, portalListeners);
    }

    @Test
    void startupLoadsExistingWorldsAndSkipsMissingOnes() throws Exception {
        Files.createDirectories(folder("many_world/keep/world").toPath());
        Files.writeString(folder("many_world/keep/world/level.dat").toPath(), "x");
        Files.createDirectories(folder("many_world/old/nether").toPath());
        plugin.getConfig().set("worlds", List.of("many_world/keep/world", "many_world/gone/world", "many_world/old/nether", "../escape"));

        Method load = CoManyWorld.class.getDeclaredMethod("loadWorldsOnStartup");
        load.setAccessible(true);
        load.invoke(plugin);

        assertNotNull(Bukkit.getWorld("many_world/keep/world"));
        assertEquals(World.Environment.NETHER, Bukkit.getWorld("many_world/old/nether").getEnvironment());
        assertNull(Bukkit.getWorld("many_world/gone/world"), "a missing world must not be regenerated");
        assertFalse(folder("many_world/gone").exists());
        assertNull(Bukkit.getWorld("../escape"));
    }

    // ---------------------------------------------------------------- create

    @Test
    void createAllMakesThreeLinkedWorlds() {
        String out = run("co-many create Test -all -s12345");
        assertNull(Bukkit.getWorld("many_world/test/world"), "worlds are created on the next ticks, not inside the command");
        server.getScheduler().performTicks(5);
        out += joined(drain(console));

        World overworld = Bukkit.getWorld("many_world/test/world");
        World nether = Bukkit.getWorld("many_world/test/world_nether");
        World end = Bukkit.getWorld("many_world/test/world_the_end");
        assertNotNull(overworld, out);
        assertNotNull(nether, out);
        assertNotNull(end, out);
        assertEquals(World.Environment.NORMAL, overworld.getEnvironment());
        assertEquals(World.Environment.NETHER, nether.getEnvironment());
        assertEquals(World.Environment.THE_END, end.getEnvironment());
        assertEquals(12345L, overworld.getSeed());
        assertEquals(List.of("many_world/test/world", "many_world/test/world_nether", "many_world/test/world_the_end"), configWorlds());
        assertTrue(out.contains("created"), out);
    }

    @Test
    void createSingleNether() {
        run("co-many create hell -12");
        server.getScheduler().performTicks(3);
        World nether = Bukkit.getWorld("many_world/hell/world_nether");
        assertNotNull(nether);
        assertEquals(World.Environment.NETHER, nether.getEnvironment());
        assertEquals(List.of("many_world/hell/world_nether"), configWorlds());
    }

    @Test
    void createRejectsBadInput() {
        assertTrue(run("co-many create ../evil -11").contains("Invalid world name"));
        assertTrue(run("co-many create a/b -11").contains("Invalid world name"));
        assertTrue(run("co-many create world -11").contains("reserved"));
        assertTrue(run("co-many create ok -99").contains("Invalid world type"));
        assertTrue(run("co-many create ok -sabc").contains("Invalid seed"));
        server.getScheduler().performTicks(5);
        assertEquals(3, Bukkit.getWorlds().size(), "no world may have been created");
        assertFalse(folder("many_world/ok").exists());
        assertFalse(new File(server.getWorldContainer().getParentFile(), "evil").exists());
    }

    @Test
    void createTwiceInTheSameTickOnlyCreatesOnce() {
        run("co-many create dup -11");
        String second = run("co-many create dup -all");
        assertTrue(second.contains("already exists"), second);
        server.getScheduler().performTicks(5);
        assertEquals(List.of("many_world/dup/world"), configWorlds());
        assertTrue(run("co-many create DUP -11").contains("already exists"));
    }

    // ---------------------------------------------------------------- tp

    @Test
    void tpTeleportsPlayer() {
        createAll("test");
        PlayerMock player = op();
        player.performCommand("co-many tp many_world/test/world_nether");
        server.getScheduler().performTicks(1);
        assertEquals("many_world/test/world_nether", player.getWorld().getName());
        assertTrue(joined(drain(player)).contains("Teleported"));

        player.performCommand("co-many tp nope");
        assertTrue(joined(drain(player)).contains("does not exist"));
        assertTrue(run("co-many tp many_world/test/world").contains("only be used by players"));
    }

    // ---------------------------------------------------------------- portals

    private PlayerPortalEvent portal(PlayerMock player, Location from, TeleportCause cause) {
        player.teleport(from);
        Location vanillaTo = new Location(Bukkit.getWorld("world_nether"), 1, 2, 3);
        PlayerPortalEvent event = new PlayerPortalEvent(player, from, vanillaTo, cause);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    void netherPortalGoesToTheGroupsNetherAndBack() {
        createAll("test");
        PlayerMock player = op();
        World overworld = Bukkit.getWorld("many_world/test/world");
        World nether = Bukkit.getWorld("many_world/test/world_nether");

        PlayerPortalEvent toNether = portal(player, new Location(overworld, 80, 300, -160), TeleportCause.NETHER_PORTAL);
        assertFalse(toNether.isCancelled());
        assertEquals(nether, toNether.getTo().getWorld());
        assertEquals(10.0, toNether.getTo().getX(), 1e-9);
        assertEquals(-20.0, toNether.getTo().getZ(), 1e-9);
        assertTrue(toNether.getTo().getY() <= nether.getMinHeight() + 120, "must not be above the nether roof: " + toNether.getTo().getY());

        PlayerPortalEvent back = portal(player, new Location(nether, 10, 64, -20), TeleportCause.NETHER_PORTAL);
        assertEquals(overworld, back.getTo().getWorld());
        assertEquals(80.0, back.getTo().getX(), 1e-9);
        assertEquals(-160.0, back.getTo().getZ(), 1e-9);
    }

    @Test
    void endPortalLandsOnObsidianPlatform() {
        createAll("test");
        PlayerMock player = op();
        World end = Bukkit.getWorld("many_world/test/world_the_end");
        PlayerPortalEvent event = portal(player, new Location(Bukkit.getWorld("many_world/test/world"), 5, 30, 5), TeleportCause.END_PORTAL);
        assertEquals(end, event.getTo().getWorld());
        assertEquals(100.5, event.getTo().getX(), 1e-9);
        assertEquals(49.0, event.getTo().getY(), 1e-9);
        assertEquals(0.5, event.getTo().getZ(), 1e-9);
        assertEquals(Material.OBSIDIAN, end.getBlockAt(100, 48, 0).getType());
        assertEquals(Material.OBSIDIAN, end.getBlockAt(98, 48, -2).getType());
        assertEquals(Material.AIR, end.getBlockAt(100, 49, 0).getType());

        PlayerPortalEvent exit = portal(player, new Location(end, 0, 64, 0), TeleportCause.END_PORTAL);
        assertEquals(Bukkit.getWorld("many_world/test/world"), exit.getTo().getWorld());
    }

    @Test
    void portalsInServerWorldsAndEndGatewaysAreLeftAlone() {
        createAll("test");
        PlayerMock player = op();
        PlayerPortalEvent main = portal(player, new Location(Bukkit.getWorld("world"), 80, 64, 80), TeleportCause.NETHER_PORTAL);
        assertEquals(new Location(Bukkit.getWorld("world_nether"), 1, 2, 3), main.getTo());

        PlayerPortalEvent gateway = portal(player, new Location(Bukkit.getWorld("many_world/test/world_the_end"), 0, 64, 0), TeleportCause.END_GATEWAY);
        assertEquals(new Location(Bukkit.getWorld("world_nether"), 1, 2, 3), gateway.getTo());
    }

    @Test
    void singleWorldPortalsAreBlockedOrUseMainWorlds() {
        run("co-many create solo -11");
        server.getScheduler().performTicks(3);
        PlayerMock player = op();
        World solo = Bukkit.getWorld("many_world/solo/world");

        PlayerPortalEvent blocked = portal(player, new Location(solo, 16, 64, 16), TeleportCause.NETHER_PORTAL);
        assertTrue(blocked.isCancelled());
        assertTrue(joined(drain(player)).contains("disabled"));

        plugin.getConfig().set("prevent-single-world-portals", false);
        PlayerPortalEvent allowed = portal(player, new Location(solo, 16, 64, 16), TeleportCause.NETHER_PORTAL);
        assertFalse(allowed.isCancelled());
        assertEquals(Bukkit.getWorld("world_nether"), allowed.getTo().getWorld());
        assertEquals(2.0, allowed.getTo().getX(), 1e-9);
    }

    @Test
    void legacyNetherAndEndFolderNamesStillWork() {
        server.createWorld(new WorldCreator("many_world/old/world"));
        server.createWorld(new WorldCreator("many_world/old/nether").environment(World.Environment.NETHER));
        server.createWorld(new WorldCreator("many_world/old/the_end").environment(World.Environment.THE_END));
        PlayerMock player = op();
        PlayerPortalEvent event = portal(player, new Location(Bukkit.getWorld("many_world/old/world"), 8, 64, 8), TeleportCause.NETHER_PORTAL);
        assertFalse(event.isCancelled());
        assertEquals("many_world/old/nether", event.getTo().getWorld().getName());
    }

    // ---------------------------------------------------------------- respawn

    @Test
    void respawnGoesToTheGroupsOverworld() {
        createAll("test");
        PlayerMock player = op();
        World overworld = Bukkit.getWorld("many_world/test/world");
        Location vanilla = Bukkit.getWorld("world").getSpawnLocation();

        player.teleport(new Location(Bukkit.getWorld("many_world/test/world_nether"), 0, 64, 0));
        PlayerRespawnEvent death = new PlayerRespawnEvent(player, vanilla, false, false, false, PlayerRespawnEvent.RespawnReason.DEATH);
        server.getPluginManager().callEvent(death);
        assertEquals(overworld, death.getRespawnLocation().getWorld());

        player.teleport(new Location(Bukkit.getWorld("many_world/test/world_the_end"), 0, 64, 0));
        PlayerRespawnEvent leftEnd = new PlayerRespawnEvent(player, vanilla, false, false, false, PlayerRespawnEvent.RespawnReason.END_PORTAL);
        server.getPluginManager().callEvent(leftEnd);
        assertEquals(overworld, leftEnd.getRespawnLocation().getWorld(), "leaving a custom End must not keep the player in the End");

        Location bed = new Location(Bukkit.getWorld("world"), 50, 70, 50);
        PlayerRespawnEvent bedSpawn = new PlayerRespawnEvent(player, bed, true, false, false, PlayerRespawnEvent.RespawnReason.DEATH);
        server.getPluginManager().callEvent(bedSpawn);
        assertEquals(bed, bedSpawn.getRespawnLocation());
    }

    // ---------------------------------------------------------------- delete / clear

    @Test
    void deleteRefusesServerWorlds() {
        for (String target : List.of("world", "world_nether", "many_world/", "many_world/../world", "../world")) {
            String out = run("co-many delete " + target);
            assertTrue(out.contains("can be deleted"), target + " -> " + out);
        }
        assertNotNull(Bukkit.getWorld("world"));
        assertTrue(trash().isEmpty());
        // Even if an old version put the main world in the trash, clear must not delete it
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("delete_worlds", List.of("world", "../outside"));
        try {
            yml.save(new File(plugin.getDataFolder(), "delete_worlds.yml"));
        } catch (IOException e) {
            fail(e);
        }
        String out = run("co-many clear confirm");
        assertTrue(out.contains("Skipped world"), out);
        assertTrue(out.contains("Skipped ../outside"), out);
        server.getScheduler().performTicks(5);
        assertTrue(folder("world/level.dat").exists());
        assertEquals(List.of("world", "../outside"), trash());
    }

    @Test
    void deleteMovesPlayersOutAndClearOnlyRemovesThatWorld() {
        createAll("test");
        PlayerMock player = op();
        player.teleport(new Location(Bukkit.getWorld("many_world/test/world_nether"), 0, 64, 0));

        String out = run("co-many delete many_world/test/world_nether");
        assertTrue(out.contains("marked for deletion"), out);
        assertNull(Bukkit.getWorld("many_world/test/world_nether"));
        assertEquals("world", player.getWorld().getName(), "player must be moved out before unloading");
        assertEquals(List.of("many_world/test/world_nether"), trash());
        assertEquals(List.of("many_world/test/world", "many_world/test/world_the_end"), configWorlds());
        assertTrue(folder("many_world/test/world_nether").exists(), "delete only moves to the trash");

        out = run("co-many clear");
        server.getScheduler().performTicks(5);
        assertTrue(out.contains("confirm"), out);
        assertTrue(folder("many_world/test/world_nether").exists(), "clear without confirm must not delete");

        run("co-many clear confirm");
        awaitOutput("Deleted world folder: many_world/test/world_nether");
        assertFalse(folder("many_world/test/world_nether").exists());
        assertTrue(folder("many_world/test/world/level.dat").exists(), "sibling worlds must survive");
        assertTrue(folder("many_world/test/world_the_end/level.dat").exists());
        assertTrue(trash().isEmpty());
    }

    @Test
    void deleteWholeGroupAndClearWithAliasCommand() {
        createAll("test");
        String out = run("co-many delete many_world/test/");
        assertTrue(out.contains("many_world/test/world_the_end"), out);
        assertEquals(3, trash().size());
        assertTrue(configWorlds().isEmpty());

        server.dispatchCommand(console, "co-many-clear confirm");
        awaitOutput("Deleted world folder: many_world/test/world_the_end");
        assertFalse(folder("many_world/test").exists(), "empty group folder is removed too");
        assertTrue(folder("many_world").exists());
    }

    @Test
    void deleteAndClearNeedTheirPermissions() {
        createAll("test");
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "co.many.worlds.admin", true);
        player.performCommand("co-many delete many_world/test/world");
        assertTrue(joined(drain(player)).contains("permission"));
        player.performCommand("co-many clear confirm");
        assertTrue(joined(drain(player)).contains("permission"));
        assertNotNull(Bukkit.getWorld("many_world/test/world"));
    }

    // ---------------------------------------------------------------- backup

    @Test
    void backupCreatesImportableZip() throws Exception {
        createAll("test");
        TestServer.DiskWorld world = (TestServer.DiskWorld) Bukkit.getWorld("many_world/test/world");
        String out = run("co-many backup many_world/test/world");
        assertFalse(world.isAutoSave(), "autosave is paused while zipping");
        assertTrue(world.saveCount > 0, "world must be saved before the backup");
        assertTrue(run("co-many backup many_world/test/world").contains("already running"));
        out += awaitOutput("backed up successfully");
        assertTrue(world.isAutoSave(), "autosave restored");

        File[] zips = new File(plugin.getDataFolder(), "many_world_backup").listFiles((d, n) -> n.endsWith(".zip"));
        assertNotNull(zips);
        assertEquals(1, zips.length);
        assertTrue(zips[0].getName().endsWith("-many_world_test_world.zip"), zips[0].getName());
        try (ZipFile zip = new ZipFile(zips[0])) {
            Set<String> names = new HashSet<>();
            zip.stream().forEach(e -> names.add(e.getName()));
            assertTrue(names.contains("world/level.dat"), names.toString());
            assertTrue(names.contains("world/region/r.0.0.mca"), names.toString());
            assertFalse(names.contains("world/session.lock"), names.toString());
        }

        // Round trip: the backup can be imported again
        Files.copy(zips[0].toPath(), new File(plugin.getDataFolder(), "import/restored.zip").toPath());
        out = run("co-many import restored.zip");
        out += awaitOutput("successfully imported");
        assertNotNull(Bukkit.getWorld("many_world/restored/world"), out);
        assertEquals("level of many_world/test/world",
                Files.readString(folder("many_world/restored/world/level.dat").toPath()));
    }

    // ---------------------------------------------------------------- import

    @Test
    void importFolderDoesNotCopyUidDat() throws Exception {
        Path source = plugin.getDataFolder().toPath().resolve("import/MyMap/saves/inner");
        Files.createDirectories(source.resolve("region"));
        Files.writeString(source.resolve("level.dat"), "my map");
        Files.write(source.resolve("region/r.1.1.mca"), new byte[1000]);
        Files.writeString(source.resolve("uid.dat"), "same-uid-as-original");
        Files.writeString(source.resolve("session.lock"), "old-lock");

        String out = run("co-many import MyMap");
        assertTrue(out.contains("Importing"), out);
        assertNull(Bukkit.getWorld("many_world/mymap/world"), "copying happens in the background");
        out = awaitOutput("successfully imported");
        assertNotNull(Bukkit.getWorld("many_world/mymap/world"), out);

        Path target = folder("many_world/mymap/world").toPath();
        assertEquals("my map", Files.readString(target.resolve("level.dat")));
        assertTrue(Files.exists(target.resolve("region/r.1.1.mca")));
        // The server writes a new uid.dat only if there is none, so a copied one would still be here.
        assertNotEquals("same-uid-as-original", Files.readString(target.resolve("uid.dat")), "uid.dat must not be copied");
        assertTrue(configWorlds().contains("many_world/mymap/world"));
        assertTrue(run("co-many import MyMap").contains("already exists"));
    }

    @Test
    void importRejectsTraversalAndZipSlip() throws Exception {
        assertTrue(run("co-many import ../../world").contains("inside"));
        assertTrue(run("co-many import missing").contains("not found"));

        File evilZip = new File(plugin.getDataFolder(), "import/evil.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(evilZip.toPath()))) {
            zos.putNextEntry(new ZipEntry("w/level.dat"));
            zos.write("x".getBytes());
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("../../../../escaped.txt"));
            zos.write("pwned".getBytes());
            zos.closeEntry();
        }
        run("co-many import evil.zip");
        awaitOutput("Failed to import");
        assertNull(Bukkit.getWorld("many_world/evil/world"));
        assertFalse(folder("many_world/evil").exists(), "partial import must be cleaned up");
        try (var files = Files.walk(tmp)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().equals("escaped.txt")), "zip slip entry was written");
        }
    }

    @Test
    void importZipWithoutLevelDatFails() throws Exception {
        File zip = new File(plugin.getDataFolder(), "import/notaworld.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("hi".getBytes());
            zos.closeEntry();
        }
        run("co-many import notaworld.zip");
        awaitOutput("no level.dat");
        assertFalse(folder("many_world/notaworld").exists());
    }

    // ---------------------------------------------------------------- list / tab complete

    @Test
    void listAndTabComplete() throws IOException {
        createAll("test");
        Files.createDirectories(plugin.getDataFolder().toPath().resolve("import/somemap"));
        String list = run("co-many list");
        assertTrue(list.contains("many_world/test/world_nether [Nether]"), list);

        CommandCompleter completer = new CommandCompleter();
        Command command = plugin.getCommand("co-many");
        assertEquals(List.of("create", "clear"), completer.onTabComplete(console, command, "co-many", new String[]{"c"}));
        List<String> delete = completer.onTabComplete(console, command, "co-many", new String[]{"delete", "many"});
        assertTrue(delete.contains("many_world/test/"), delete.toString());
        assertTrue(delete.contains("many_world/test/world_the_end"), delete.toString());
        assertFalse(delete.contains("world"), "server worlds are not offered for deletion");
        assertEquals(List.of("somemap"), completer.onTabComplete(console, command, "co-many", new String[]{"import", ""}));
        assertEquals(List.of("confirm"), completer.onTabComplete(console, command, "co-many", new String[]{"clear", ""}));
        assertEquals(List.of("-all"), completer.onTabComplete(console, command, "co-many", new String[]{"create", "x", "-a"}));
        assertEquals(List.of(), completer.onTabComplete(console, command, "co-many", new String[]{"about", ""}));
    }

    @Test
    void aboutShowsVersionAndPlatform() {
        String out = run("co-many about");
        assertTrue(out.contains(plugin.getDescription().getVersion()), out);
        assertTrue(run("co-many").contains("/co-many create"));
    }
}
