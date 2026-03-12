package com.evans.simulationmod.cache;

import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkCacheManagerTest {

    @Test
    void emptyManagerHasNoChunks() {
        ChunkCacheManager manager = new ChunkCacheManager();
        assertEquals(0, manager.getCachedChunkCount());
        assertNull(manager.getChunk(new ChunkPos(0, 0)));
        assertFalse(manager.hasChunk(new ChunkPos(0, 0)));
    }

    @Test
    void manualCacheAndRetrieve() {
        ChunkCacheManager manager = new ChunkCacheManager();
        ChunkPos pos = new ChunkPos(3, 7);
        CachedChunk chunk = new CachedChunk(pos);
        chunk.setBlock(5, 64, 5, new CachedBlockData(BlockCategory.STONE));

        // Directly put into the cache via getAllChunks (testing the map access)
        manager.getAllChunks().put(pos, chunk);

        assertTrue(manager.hasChunk(pos));
        assertEquals(1, manager.getCachedChunkCount());
        assertNotNull(manager.getChunk(pos));
        assertEquals(BlockCategory.STONE, manager.getChunk(pos).getBlock(5, 64, 5).getCategory());
    }

    @Test
    void removeChunk() {
        ChunkCacheManager manager = new ChunkCacheManager();
        ChunkPos pos = new ChunkPos(1, 1);
        manager.getAllChunks().put(pos, new CachedChunk(pos));

        assertTrue(manager.hasChunk(pos));
        manager.removeChunk(pos);
        assertFalse(manager.hasChunk(pos));
        assertEquals(0, manager.getCachedChunkCount());
    }

    @Test
    void removeNonexistentChunkDoesNotThrow() {
        ChunkCacheManager manager = new ChunkCacheManager();
        manager.removeChunk(new ChunkPos(99, 99));
        // No exception
    }

    @Test
    void multipleChunks() {
        ChunkCacheManager manager = new ChunkCacheManager();
        ChunkPos pos1 = new ChunkPos(0, 0);
        ChunkPos pos2 = new ChunkPos(1, 0);
        ChunkPos pos3 = new ChunkPos(0, 1);

        manager.getAllChunks().put(pos1, new CachedChunk(pos1));
        manager.getAllChunks().put(pos2, new CachedChunk(pos2));
        manager.getAllChunks().put(pos3, new CachedChunk(pos3));

        assertEquals(3, manager.getCachedChunkCount());
        assertTrue(manager.hasChunk(pos1));
        assertTrue(manager.hasChunk(pos2));
        assertTrue(manager.hasChunk(pos3));
        assertFalse(manager.hasChunk(new ChunkPos(5, 5)));
    }

    @Test
    void getChunkReturnsNullForMissing() {
        ChunkCacheManager manager = new ChunkCacheManager();
        assertNull(manager.getChunk(new ChunkPos(10, 10)));
    }
}
