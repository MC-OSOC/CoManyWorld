package org.cakedek.comanyworld;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Small compatibility layer so one jar works on both Spigot and Paper.
 * The plugin is compiled against the Spigot API; Paper-only methods are looked up at runtime.
 */
public final class Platform {

    /** Paper's Entity#teleportAsync(Location), or null on Spigot. */
    private static final Method TELEPORT_ASYNC = findMethod(Entity.class, "teleportAsync", Location.class);

    private Platform() {
    }

    public static boolean isPaper() {
        return TELEPORT_ASYNC != null;
    }

    /** Server software name and version, e.g. "Paper 1.21.11-...". */
    public static String describe() {
        return Bukkit.getName() + " " + Bukkit.getVersion();
    }

    /**
     * Teleports an entity. On Paper the destination chunks are loaded asynchronously,
     * so teleporting into a world that isn't loaded around its spawn doesn't freeze the server.
     * On Spigot this is a normal synchronous teleport.
     * The returned future completes on the main thread.
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        if (TELEPORT_ASYNC != null) {
            try {
                return (CompletableFuture<Boolean>) TELEPORT_ASYNC.invoke(entity, location);
            } catch (ReflectiveOperationException | ClassCastException ignored) {
                // Fall back to the normal teleport below.
            }
        }
        return CompletableFuture.completedFuture(entity.teleport(location));
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
