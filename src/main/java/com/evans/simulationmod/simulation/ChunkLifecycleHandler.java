package com.evans.simulationmod.simulation;

import com.evans.simulationmod.cache.BlockCategoryMapper;
import com.evans.simulationmod.cache.CachedBlockData;
import com.evans.simulationmod.cache.ChunkCacheManager;
import com.evans.simulationmod.cache.PendingBlockChange;
import com.evans.simulationmod.cache.PendingChangeQueue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;

public class ChunkLifecycleHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        ChunkAccess chunkAccess = event.getChunk();
        ChunkPos pos = chunkAccess.getPos();

        if (!(chunkAccess instanceof LevelChunk levelChunk)) {
            LOGGER.info("Chunk unload ({}, {}): not a LevelChunk (type: {}), skipping",
                    pos.x, pos.z, chunkAccess.getClass().getSimpleName());
            return;
        }

        SimulationManager manager = SimulationManager.get(serverLevel);

        if (manager.getRegion() == null) {
            return;
        }

        if (!manager.isInSimulationRegion(pos)) {
            return;
        }

        manager.getChunkCacheManager().cacheChunk(pos, levelChunk);
        LOGGER.info("Cached chunk ({}, {}) on unload — total cached: {}",
                pos.x, pos.z, manager.getChunkCacheManager().getCachedChunkCount());
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        ChunkAccess chunkAccess = event.getChunk();
        if (!(chunkAccess instanceof LevelChunk levelChunk)) {
            return;
        }

        ChunkPos pos = levelChunk.getPos();
        SimulationManager manager = SimulationManager.get(serverLevel);

        if (!manager.isInSimulationRegion(pos)) {
            return;
        }

        ChunkCacheManager cacheManager = manager.getChunkCacheManager();
        PendingChangeQueue pendingQueue = manager.getPendingChangeQueue();

        // Reconcile pending changes
        if (pendingQueue.hasChanges(pos)) {
            List<PendingBlockChange> changes = pendingQueue.getChanges(pos);
            int applied = 0;
            int skipped = 0;

            for (PendingBlockChange change : changes) {
                var realState = serverLevel.getBlockState(change.getPos());
                CachedBlockData realCategory = BlockCategoryMapper.categorize(realState);

                if (realCategory.getCategory() == change.getExpectedBefore().getCategory()) {
                    serverLevel.setBlockAndUpdate(change.getPos(), change.getRealBlockState());
                    applied++;
                } else {
                    LOGGER.warn("Reconciliation conflict at {}: expected {} but found {}",
                            change.getPos(), change.getExpectedBefore().getCategory(), realCategory.getCategory());
                    skipped++;
                }
            }

            pendingQueue.clearChanges(pos);
            if (applied > 0 || skipped > 0) {
                LOGGER.info("Reconciled chunk ({}, {}): {} applied, {} conflicts skipped",
                        pos.x, pos.z, applied, skipped);
            }
        }

        // Remove chunk from cache — real world takes over
        if (cacheManager.hasChunk(pos)) {
            LOGGER.info("Removing chunk ({}, {}) from cache on load — real world takes over", pos.x, pos.z);
        }
        cacheManager.removeChunk(pos);
    }
}
