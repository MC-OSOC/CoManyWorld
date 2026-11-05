package org.cakedek.comanyworld;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Empty world with a small stone platform to stand on (lobby / mini-game worlds).
 * The platform is 3x3 blocks at y=63 and the world spawn is on top of it.
 */
public final class VoidGenerator extends ChunkGenerator {

    private static final int PLATFORM_Y = 63;
    private static final int PLATFORM_MIN = 7;
    private static final int PLATFORM_MAX = 9;

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        if (chunkX != 0 || chunkZ != 0) {
            return;
        }
        for (int x = PLATFORM_MIN; x <= PLATFORM_MAX; x++) {
            for (int z = PLATFORM_MIN; z <= PLATFORM_MAX; z++) {
                chunkData.setBlock(x, PLATFORM_Y, z, Material.STONE);
            }
        }
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 8.5, PLATFORM_Y + 1, 8.5);
    }
}
