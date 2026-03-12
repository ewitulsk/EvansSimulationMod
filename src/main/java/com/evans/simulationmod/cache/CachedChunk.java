package com.evans.simulationmod.cache;

import net.minecraft.world.level.ChunkPos;

public class CachedChunk {
    public static final int MIN_Y = -64;
    public static final int MAX_Y = 319;
    public static final int SECTION_COUNT = (MAX_Y - MIN_Y + 1) / 16; // 24
    private static final int COLUMNS = 16 * 16; // 256

    private final ChunkPos chunkPos;
    private final CachedChunkSection[] sections;
    private final int[] heightmap;

    public CachedChunk(ChunkPos chunkPos) {
        this.chunkPos = chunkPos;
        this.sections = new CachedChunkSection[SECTION_COUNT];
        this.heightmap = new int[COLUMNS];

        for (int i = 0; i < SECTION_COUNT; i++) {
            sections[i] = new CachedChunkSection();
        }
        for (int i = 0; i < COLUMNS; i++) {
            heightmap[i] = MIN_Y - 1;
        }
    }

    public CachedChunk(ChunkPos chunkPos, CachedChunkSection[] sections, int[] heightmap) {
        this.chunkPos = chunkPos;
        this.sections = sections;
        this.heightmap = heightmap;
    }

    public CachedBlockData getBlock(int x, int y, int z) {
        int sectionIndex = getSectionIndex(y);
        if (sectionIndex < 0 || sectionIndex >= SECTION_COUNT) {
            return CachedBlockData.AIR;
        }
        return sections[sectionIndex].getBlock(x & 15, y & 15, z & 15);
    }

    public void setBlock(int x, int y, int z, CachedBlockData data) {
        int sectionIndex = getSectionIndex(y);
        if (sectionIndex < 0 || sectionIndex >= SECTION_COUNT) {
            return;
        }
        sections[sectionIndex].setBlock(x & 15, y & 15, z & 15, data);
        updateHeightmap(x & 15, y, z & 15, data);
    }

    private void updateHeightmap(int localX, int y, int localZ, CachedBlockData data) {
        int columnIndex = localZ * 16 + localX;
        int currentHeight = heightmap[columnIndex];

        if (data.getCategory() != BlockCategory.AIR && data.getCategory() != BlockCategory.LIQUID) {
            if (y > currentHeight) {
                heightmap[columnIndex] = y;
            }
        } else if (y == currentHeight) {
            // Scan downward to find new surface
            for (int scanY = y - 1; scanY >= MIN_Y; scanY--) {
                CachedBlockData below = getBlock(localX, scanY, localZ);
                if (below.getCategory() != BlockCategory.AIR && below.getCategory() != BlockCategory.LIQUID) {
                    heightmap[columnIndex] = scanY;
                    return;
                }
            }
            heightmap[columnIndex] = MIN_Y - 1;
        }
    }

    public int getHeightAt(int localX, int localZ) {
        return heightmap[localZ * 16 + localX];
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public CachedChunkSection[] getSections() {
        return sections;
    }

    public int[] getHeightmap() {
        return heightmap;
    }

    private static int getSectionIndex(int y) {
        return (y - MIN_Y) >> 4;
    }
}
