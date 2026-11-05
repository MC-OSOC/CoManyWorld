package org.cakedek.comanyworld;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.command.MessageTarget;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/** Starts a {@link TestServer} with the server's own three worlds and the plugin loaded. */
abstract class TestBase {

    @TempDir
    Path tmp;

    protected TestServer server;
    protected CoManyWorld plugin;
    protected ConsoleCommandSenderMock console;

    /** True to store worlds like Paper 26.x does (world/dimensions/minecraft/...). */
    protected abstract boolean dimensionFolders();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new TestServer(tmp.resolve("server").toFile(), dimensionFolders()));
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

    protected static List<String> drain(MessageTarget target) {
        List<String> messages = new ArrayList<>();
        String message;
        while ((message = target.nextMessage()) != null) {
            messages.add(ChatColor.stripColor(message));
        }
        return messages;
    }

    protected static String joined(List<String> messages) {
        return String.join("\n", messages);
    }

    /** Runs a console command and returns everything it printed. */
    protected String run(String commandLine) {
        server.dispatchCommand(console, commandLine);
        return joined(drain(console));
    }

    /**
     * Ticks the server until the console has printed {@code expected} (or 10 s pass).
     * Background work (backup, import, clear) runs on MockBukkit's thread pool and reports back
     * on the main thread, so we wait for the result instead of guessing when the pool is idle.
     */
    protected String awaitOutput(String expected) {
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

    /** &lt;server&gt;/&lt;path&gt; */
    protected File serverFile(String path) {
        return new File(server.getWorldContainer(), path);
    }

    protected List<String> configWorlds() {
        return plugin.getConfig().getStringList("worlds");
    }

    protected List<String> trash() {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "delete_worlds.yml"))
                .getStringList("delete_worlds");
    }

    protected void createAll(String name) {
        run("co-many create " + name + " -all");
        server.getScheduler().performTicks(5);
    }

    protected PlayerMock op() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        drain(player);
        return player;
    }
}
