package com.evans.simulationmod.cache;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;

public class CachedBlockData {
    public static final CachedBlockData AIR = new CachedBlockData(BlockCategory.AIR, null, -1, -1L);

    private final BlockCategory category;
    @Nullable
    private final ResourceLocation blockId;
    private final int growthStage;
    private final long plantedAtTick;

    public CachedBlockData(BlockCategory category, @Nullable ResourceLocation blockId, int growthStage, long plantedAtTick) {
        this.category = category;
        this.blockId = blockId;
        this.growthStage = growthStage;
        this.plantedAtTick = plantedAtTick;
    }

    public CachedBlockData(BlockCategory category) {
        this(category, null, -1, -1L);
    }

    public CachedBlockData(BlockCategory category, ResourceLocation blockId) {
        this(category, blockId, -1, -1L);
    }

    public BlockCategory getCategory() {
        return category;
    }

    @Nullable
    public ResourceLocation getBlockId() {
        return blockId;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public long getPlantedAtTick() {
        return plantedAtTick;
    }

    public CachedBlockData withGrowthStage(int growthStage) {
        return new CachedBlockData(category, blockId, growthStage, plantedAtTick);
    }

    public CachedBlockData withPlantedAtTick(long plantedAtTick) {
        return new CachedBlockData(category, blockId, growthStage, plantedAtTick);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CachedBlockData that = (CachedBlockData) o;
        return growthStage == that.growthStage
                && plantedAtTick == that.plantedAtTick
                && category == that.category
                && Objects.equals(blockId, that.blockId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, blockId, growthStage, plantedAtTick);
    }
}
