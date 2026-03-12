package com.evans.simulationmod.cache;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CachedChunkSectionTest {

    @Test
    void newSectionIsEmpty() {
        CachedChunkSection section = new CachedChunkSection();
        assertTrue(section.isEmpty());
    }

    @Test
    void defaultBlockIsAir() {
        CachedChunkSection section = new CachedChunkSection();
        assertEquals(CachedBlockData.AIR, section.getBlock(0, 0, 0));
        assertEquals(CachedBlockData.AIR, section.getBlock(15, 15, 15));
        assertEquals(CachedBlockData.AIR, section.getBlock(8, 4, 12));
    }

    @Test
    void setAndGetSingleBlock() {
        CachedChunkSection section = new CachedChunkSection();
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        section.setBlock(5, 10, 3, stone);

        assertEquals(stone, section.getBlock(5, 10, 3));
        assertEquals(CachedBlockData.AIR, section.getBlock(0, 0, 0));
    }

    @Test
    void setAndGetMultipleBlocks() {
        CachedChunkSection section = new CachedChunkSection();
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);
        CachedBlockData ore = new CachedBlockData(BlockCategory.ORE, ResourceLocation.parse("minecraft:diamond_ore"));
        CachedBlockData log = new CachedBlockData(BlockCategory.LOG);

        section.setBlock(0, 0, 0, stone);
        section.setBlock(1, 1, 1, ore);
        section.setBlock(2, 2, 2, log);

        assertEquals(stone, section.getBlock(0, 0, 0));
        assertEquals(ore, section.getBlock(1, 1, 1));
        assertEquals(log, section.getBlock(2, 2, 2));
    }

    @Test
    void overwriteBlock() {
        CachedChunkSection section = new CachedChunkSection();
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);
        CachedBlockData air = CachedBlockData.AIR;

        section.setBlock(5, 5, 5, stone);
        assertEquals(stone, section.getBlock(5, 5, 5));

        section.setBlock(5, 5, 5, air);
        assertEquals(air, section.getBlock(5, 5, 5));
    }

    @Test
    void notEmptyAfterSettingBlock() {
        CachedChunkSection section = new CachedChunkSection();
        section.setBlock(0, 0, 0, new CachedBlockData(BlockCategory.STONE));
        assertFalse(section.isEmpty());
    }

    @Test
    void paletteGrowsBeyondTwoEntries() {
        CachedChunkSection section = new CachedChunkSection();

        // Add many unique block types to force palette growth
        CachedBlockData[] types = new CachedBlockData[]{
                new CachedBlockData(BlockCategory.STONE),
                new CachedBlockData(BlockCategory.SOFT),
                new CachedBlockData(BlockCategory.HARD),
                new CachedBlockData(BlockCategory.ORE, ResourceLocation.parse("minecraft:diamond_ore")),
                new CachedBlockData(BlockCategory.ORE, ResourceLocation.parse("minecraft:iron_ore")),
                new CachedBlockData(BlockCategory.ORE, ResourceLocation.parse("minecraft:gold_ore")),
                new CachedBlockData(BlockCategory.LOG),
                new CachedBlockData(BlockCategory.LEAVES),
                new CachedBlockData(BlockCategory.LIQUID),
                new CachedBlockData(BlockCategory.BUILDABLE),
        };

        for (int i = 0; i < types.length; i++) {
            section.setBlock(i, 0, 0, types[i]);
        }

        // Read them all back
        for (int i = 0; i < types.length; i++) {
            assertEquals(types[i], section.getBlock(i, 0, 0),
                    "Block at index " + i + " should be " + types[i].getCategory());
        }
    }

    @Test
    void allCornersAccessible() {
        CachedChunkSection section = new CachedChunkSection();
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);

        int[][] corners = {
                {0, 0, 0}, {15, 0, 0}, {0, 15, 0}, {0, 0, 15},
                {15, 15, 0}, {15, 0, 15}, {0, 15, 15}, {15, 15, 15}
        };

        for (int[] corner : corners) {
            section.setBlock(corner[0], corner[1], corner[2], stone);
        }

        for (int[] corner : corners) {
            assertEquals(stone, section.getBlock(corner[0], corner[1], corner[2]),
                    "Corner (" + corner[0] + "," + corner[1] + "," + corner[2] + ") failed");
        }
    }

    @Test
    void rawDataAndPaletteAccessible() {
        CachedChunkSection section = new CachedChunkSection();
        section.setBlock(0, 0, 0, new CachedBlockData(BlockCategory.STONE));

        assertNotNull(section.getRawData());
        assertNotNull(section.getPalette());
        assertEquals(2, section.getPalette().size()); // AIR + STONE
        assertTrue(section.getBits() >= 1);
    }
}
