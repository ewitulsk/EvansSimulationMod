package com.evans.simulationmod.cache;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CachedChunkTest {

    @Test
    void newChunkDefaultsToAir() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        assertEquals(CachedBlockData.AIR, chunk.getBlock(0, 0, 0));
        assertEquals(CachedBlockData.AIR, chunk.getBlock(8, 64, 8));
        assertEquals(CachedBlockData.AIR, chunk.getBlock(15, 319, 15));
    }

    @Test
    void setAndGetAtVariousYLevels() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        // Bottom of world
        chunk.setBlock(0, -64, 0, stone);
        assertEquals(stone, chunk.getBlock(0, -64, 0));

        // Sea level
        chunk.setBlock(8, 63, 8, stone);
        assertEquals(stone, chunk.getBlock(8, 63, 8));

        // Top of world
        chunk.setBlock(15, 319, 15, stone);
        assertEquals(stone, chunk.getBlock(15, 319, 15));
    }

    @Test
    void outOfBoundsYReturnsAir() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        assertEquals(CachedBlockData.AIR, chunk.getBlock(0, -65, 0));
        assertEquals(CachedBlockData.AIR, chunk.getBlock(0, 320, 0));
    }

    @Test
    void outOfBoundsYSetIsIgnored() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        chunk.setBlock(0, -65, 0, new CachedBlockData(BlockCategory.STONE));
        chunk.setBlock(0, 320, 0, new CachedBlockData(BlockCategory.STONE));
        // Should not throw
    }

    @Test
    void heightmapStartsBelowMinY() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        assertEquals(-65, chunk.getHeightAt(0, 0));
    }

    @Test
    void heightmapRaisesOnSolidBlock() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        chunk.setBlock(5, 10, 5, stone);
        assertEquals(10, chunk.getHeightAt(5, 5));

        // Place higher block, heightmap should rise
        chunk.setBlock(5, 50, 5, stone);
        assertEquals(50, chunk.getHeightAt(5, 5));
    }

    @Test
    void heightmapDoesNotLowerOnNonSurfaceBreak() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        chunk.setBlock(3, 10, 3, stone);
        chunk.setBlock(3, 20, 3, stone);
        assertEquals(20, chunk.getHeightAt(3, 3));

        // Break the lower block — heightmap should stay at 20
        chunk.setBlock(3, 10, 3, CachedBlockData.AIR);
        assertEquals(20, chunk.getHeightAt(3, 3));
    }

    @Test
    void heightmapLowersOnSurfaceBreak() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        chunk.setBlock(7, 10, 7, stone);
        chunk.setBlock(7, 20, 7, stone);
        assertEquals(20, chunk.getHeightAt(7, 7));

        // Break the surface block — heightmap should drop to 10
        chunk.setBlock(7, 20, 7, CachedBlockData.AIR);
        assertEquals(10, chunk.getHeightAt(7, 7));
    }

    @Test
    void heightmapScansToBottomIfAllCleared() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        chunk.setBlock(0, 0, 0, stone);
        assertEquals(0, chunk.getHeightAt(0, 0));

        chunk.setBlock(0, 0, 0, CachedBlockData.AIR);
        assertEquals(-65, chunk.getHeightAt(0, 0));
    }

    @Test
    void heightmapIgnoresLiquid() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData liquid = new CachedBlockData(BlockCategory.LIQUID);

        chunk.setBlock(4, 62, 4, liquid);
        // Liquid should not count as surface
        assertEquals(-65, chunk.getHeightAt(4, 4));
    }

    @Test
    void heightmapTracksCorrectColumn() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        chunk.setBlock(0, 100, 0, stone);
        chunk.setBlock(15, 50, 15, stone);

        assertEquals(100, chunk.getHeightAt(0, 0));
        assertEquals(50, chunk.getHeightAt(15, 15));
        assertEquals(-65, chunk.getHeightAt(8, 8));
    }

    @Test
    void crossSectionBoundary() {
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        // Y=15 is in section 0 (relative to min Y), Y=16 is in section 1
        // Actual sections: Y=-64 to -49 = section 0, Y=-48 to -33 = section 1, etc.
        // Y=0 is in section (0 - (-64)) / 16 = 4
        // Y=15 is in section (15 + 64) / 16 = 4
        // Y=16 is in section (16 + 64) / 16 = 5
        chunk.setBlock(0, 15, 0, stone);
        chunk.setBlock(0, 16, 0, stone);

        assertEquals(stone, chunk.getBlock(0, 15, 0));
        assertEquals(stone, chunk.getBlock(0, 16, 0));
    }

    @Test
    void sectionCountIs24() {
        assertEquals(24, CachedChunk.SECTION_COUNT);
    }

    @Test
    void chunkPosAccessible() {
        ChunkPos pos = new ChunkPos(5, -3);
        CachedChunk chunk = new CachedChunk(pos);
        assertEquals(pos, chunk.getChunkPos());
    }
}
