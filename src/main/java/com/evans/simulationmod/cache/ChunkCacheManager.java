package com.evans.simulationmod.cache;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ChunkCacheManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<ChunkPos, CachedChunk> cache = new HashMap<>();

    public void cacheChunk(ChunkPos pos, LevelChunk chunk) {
        CachedChunk cached = new CachedChunk(pos);
        LevelChunkSection[] sections = chunk.getSections();
        int minY = chunk.getMinBuildHeight();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir()) {
                continue;
            }

            int sectionY = minY + (sectionIndex * 16);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.isAir()) {
                            continue;
                        }
                        CachedBlockData data = BlockCategoryMapper.categorize(state);
                        cached.setBlock(x, sectionY + y, z, data);
                    }
                }
            }
        }

        cache.put(pos, cached);
        LOGGER.debug("Cached chunk ({}, {}) - {} sections scanned", pos.x, pos.z, sections.length);
    }

    @Nullable
    public CachedChunk getChunk(ChunkPos pos) {
        return cache.get(pos);
    }

    public void removeChunk(ChunkPos pos) {
        cache.remove(pos);
    }

    public boolean hasChunk(ChunkPos pos) {
        return cache.containsKey(pos);
    }

    public int getCachedChunkCount() {
        return cache.size();
    }

    public Map<ChunkPos, CachedChunk> getAllChunks() {
        return cache;
    }
}
