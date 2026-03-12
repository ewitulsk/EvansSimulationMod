package com.evans.simulationmod.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PendingChangeQueueTest {

    private PendingBlockChange makeChange(BlockPos pos, BlockCategory before, BlockCategory after, long tick) {
        // Pass null for BlockState — we're only testing queue logic, not reconciliation
        return new PendingBlockChange(
                pos,
                new CachedBlockData(before),
                new CachedBlockData(after),
                null,
                tick
        );
    }

    @Test
    void emptyQueueHasNoChanges() {
        PendingChangeQueue queue = new PendingChangeQueue();
        ChunkPos pos = new ChunkPos(0, 0);

        assertFalse(queue.hasChanges(pos));
        assertTrue(queue.getChanges(pos).isEmpty());
        assertEquals(0, queue.getTotalChangeCount());
    }

    @Test
    void enqueueAndRetrieve() {
        PendingChangeQueue queue = new PendingChangeQueue();
        BlockPos blockPos = new BlockPos(5, 64, 5);
        PendingBlockChange change = makeChange(blockPos, BlockCategory.STONE, BlockCategory.AIR, 100L);

        queue.enqueue(change);

        ChunkPos chunkPos = new ChunkPos(blockPos);
        assertTrue(queue.hasChanges(chunkPos));
        assertEquals(1, queue.getChanges(chunkPos).size());
        assertEquals(1, queue.getTotalChangeCount());
    }

    @Test
    void multipleChangesInSameChunk() {
        PendingChangeQueue queue = new PendingChangeQueue();
        BlockPos pos1 = new BlockPos(1, 10, 1);
        BlockPos pos2 = new BlockPos(2, 20, 2);
        BlockPos pos3 = new BlockPos(3, 30, 3);

        queue.enqueue(makeChange(pos1, BlockCategory.STONE, BlockCategory.AIR, 1L));
        queue.enqueue(makeChange(pos2, BlockCategory.ORE, BlockCategory.AIR, 2L));
        queue.enqueue(makeChange(pos3, BlockCategory.SOFT, BlockCategory.AIR, 3L));

        ChunkPos chunkPos = new ChunkPos(pos1);
        assertEquals(3, queue.getChanges(chunkPos).size());
        assertEquals(3, queue.getTotalChangeCount());
    }

    @Test
    void changesInDifferentChunks() {
        PendingChangeQueue queue = new PendingChangeQueue();
        // Chunk (0,0)
        BlockPos posA = new BlockPos(5, 64, 5);
        // Chunk (1,0) — 16 blocks over in X
        BlockPos posB = new BlockPos(21, 64, 5);

        queue.enqueue(makeChange(posA, BlockCategory.STONE, BlockCategory.AIR, 1L));
        queue.enqueue(makeChange(posB, BlockCategory.STONE, BlockCategory.AIR, 2L));

        assertEquals(1, queue.getChanges(new ChunkPos(posA)).size());
        assertEquals(1, queue.getChanges(new ChunkPos(posB)).size());
        assertEquals(2, queue.getTotalChangeCount());
    }

    @Test
    void clearChangesRemovesOnlyTargetChunk() {
        PendingChangeQueue queue = new PendingChangeQueue();
        BlockPos posA = new BlockPos(5, 64, 5);
        BlockPos posB = new BlockPos(21, 64, 5);

        queue.enqueue(makeChange(posA, BlockCategory.STONE, BlockCategory.AIR, 1L));
        queue.enqueue(makeChange(posB, BlockCategory.STONE, BlockCategory.AIR, 2L));

        ChunkPos chunkA = new ChunkPos(posA);
        queue.clearChanges(chunkA);

        assertFalse(queue.hasChanges(chunkA));
        assertTrue(queue.hasChanges(new ChunkPos(posB)));
        assertEquals(1, queue.getTotalChangeCount());
    }

    @Test
    void clearEmptyChunkDoesNotThrow() {
        PendingChangeQueue queue = new PendingChangeQueue();
        queue.clearChanges(new ChunkPos(99, 99));
        // No exception
    }

    @Test
    void getChangesReturnsEmptyListForUnknownChunk() {
        PendingChangeQueue queue = new PendingChangeQueue();
        List<PendingBlockChange> changes = queue.getChanges(new ChunkPos(42, 42));
        assertNotNull(changes);
        assertTrue(changes.isEmpty());
    }

    @Test
    void changesPreserveOrder() {
        PendingChangeQueue queue = new PendingChangeQueue();
        BlockPos pos1 = new BlockPos(1, 10, 1);
        BlockPos pos2 = new BlockPos(2, 20, 2);
        BlockPos pos3 = new BlockPos(3, 30, 3);

        queue.enqueue(makeChange(pos1, BlockCategory.STONE, BlockCategory.AIR, 10L));
        queue.enqueue(makeChange(pos2, BlockCategory.ORE, BlockCategory.AIR, 20L));
        queue.enqueue(makeChange(pos3, BlockCategory.SOFT, BlockCategory.AIR, 30L));

        List<PendingBlockChange> changes = queue.getChanges(new ChunkPos(pos1));
        assertEquals(pos1, changes.get(0).getPos());
        assertEquals(pos2, changes.get(1).getPos());
        assertEquals(pos3, changes.get(2).getPos());
        assertEquals(10L, changes.get(0).getTick());
        assertEquals(20L, changes.get(1).getTick());
        assertEquals(30L, changes.get(2).getTick());
    }

    @Test
    void pendingBlockChangeFieldAccess() {
        BlockPos pos = new BlockPos(8, 100, 8);
        CachedBlockData before = new CachedBlockData(BlockCategory.STONE);
        CachedBlockData after = new CachedBlockData(BlockCategory.AIR);

        PendingBlockChange change = new PendingBlockChange(pos, before, after, null, 42L);

        assertEquals(pos, change.getPos());
        assertEquals(before, change.getExpectedBefore());
        assertEquals(after, change.getAfter());
        assertNull(change.getRealBlockState());
        assertEquals(42L, change.getTick());
    }
}
