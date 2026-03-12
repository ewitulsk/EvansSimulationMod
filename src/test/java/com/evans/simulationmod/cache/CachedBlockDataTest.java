package com.evans.simulationmod.cache;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CachedBlockDataTest {

    @Test
    void airConstantIsAirCategory() {
        assertEquals(BlockCategory.AIR, CachedBlockData.AIR.getCategory());
        assertNull(CachedBlockData.AIR.getBlockId());
        assertEquals(-1, CachedBlockData.AIR.getGrowthStage());
        assertEquals(-1L, CachedBlockData.AIR.getPlantedAtTick());
    }

    @Test
    void equalityByValue() {
        CachedBlockData a = new CachedBlockData(BlockCategory.STONE);
        CachedBlockData b = new CachedBlockData(BlockCategory.STONE);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityByCategory() {
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);
        CachedBlockData soft = new CachedBlockData(BlockCategory.SOFT);
        assertNotEquals(stone, soft);
    }

    @Test
    void equalityWithBlockId() {
        ResourceLocation id = ResourceLocation.parse("minecraft:diamond_ore");
        CachedBlockData a = new CachedBlockData(BlockCategory.ORE, id);
        CachedBlockData b = new CachedBlockData(BlockCategory.ORE, id);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityByBlockId() {
        CachedBlockData a = new CachedBlockData(BlockCategory.ORE, ResourceLocation.parse("minecraft:diamond_ore"));
        CachedBlockData b = new CachedBlockData(BlockCategory.ORE, ResourceLocation.parse("minecraft:iron_ore"));
        assertNotEquals(a, b);
    }

    @Test
    void cropMetadata() {
        ResourceLocation wheat = ResourceLocation.parse("minecraft:wheat");
        CachedBlockData crop = new CachedBlockData(BlockCategory.CROP, wheat, 3, 1000L);
        assertEquals(BlockCategory.CROP, crop.getCategory());
        assertEquals(wheat, crop.getBlockId());
        assertEquals(3, crop.getGrowthStage());
        assertEquals(1000L, crop.getPlantedAtTick());
    }

    @Test
    void withGrowthStageCreatesNewInstance() {
        ResourceLocation wheat = ResourceLocation.parse("minecraft:wheat");
        CachedBlockData original = new CachedBlockData(BlockCategory.CROP, wheat, 0, 100L);
        CachedBlockData advanced = original.withGrowthStage(5);

        assertEquals(0, original.getGrowthStage());
        assertEquals(5, advanced.getGrowthStage());
        assertEquals(100L, advanced.getPlantedAtTick());
        assertEquals(wheat, advanced.getBlockId());
    }

    @Test
    void withPlantedAtTickCreatesNewInstance() {
        ResourceLocation wheat = ResourceLocation.parse("minecraft:wheat");
        CachedBlockData original = new CachedBlockData(BlockCategory.CROP, wheat, 3, -1L);
        CachedBlockData planted = original.withPlantedAtTick(500L);

        assertEquals(-1L, original.getPlantedAtTick());
        assertEquals(500L, planted.getPlantedAtTick());
        assertEquals(3, planted.getGrowthStage());
    }

    @Test
    void equalityIncludesCropMetadata() {
        ResourceLocation wheat = ResourceLocation.parse("minecraft:wheat");
        CachedBlockData a = new CachedBlockData(BlockCategory.CROP, wheat, 3, 100L);
        CachedBlockData b = new CachedBlockData(BlockCategory.CROP, wheat, 3, 100L);
        CachedBlockData c = new CachedBlockData(BlockCategory.CROP, wheat, 4, 100L);
        CachedBlockData d = new CachedBlockData(BlockCategory.CROP, wheat, 3, 200L);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }
}
