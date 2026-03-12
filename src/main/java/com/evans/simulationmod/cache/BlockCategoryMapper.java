package com.evans.simulationmod.cache;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

public class BlockCategoryMapper {

    public static CachedBlockData categorize(BlockState state) {
        if (state.isAir()) {
            return CachedBlockData.AIR;
        }

        if (!state.getFluidState().isEmpty()) {
            return new CachedBlockData(BlockCategory.LIQUID, getBlockId(state));
        }

        Block block = state.getBlock();
        ResourceLocation blockId = getBlockId(state);

        // Crops
        if (block instanceof CropBlock cropBlock) {
            int age = cropBlock.getAge(state);
            return new CachedBlockData(BlockCategory.CROP, blockId, age, -1L);
        }

        // Ores (check before stone since ores are stone-like)
        if (state.is(Tags.Blocks.ORES)) {
            return new CachedBlockData(BlockCategory.ORE, blockId);
        }

        // Logs
        if (state.is(BlockTags.LOGS)) {
            return new CachedBlockData(BlockCategory.LOG, blockId);
        }

        // Leaves
        if (state.is(BlockTags.LEAVES)) {
            return new CachedBlockData(BlockCategory.LEAVES, blockId);
        }

        // Hard blocks
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.BEDROCK
                || block == Blocks.REINFORCED_DEEPSLATE) {
            return new CachedBlockData(BlockCategory.HARD, blockId);
        }

        // Stone-type blocks
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
                || block == Blocks.STONE || block == Blocks.COBBLESTONE
                || block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE
                || block == Blocks.TUFF || block == Blocks.CALCITE
                || block == Blocks.DRIPSTONE_BLOCK || block == Blocks.SMOOTH_STONE) {
            return new CachedBlockData(BlockCategory.STONE, blockId);
        }

        // Soft blocks
        if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT || block == Blocks.PODZOL || block == Blocks.MYCELIUM
                || block == Blocks.MUD || block == Blocks.MUDDY_MANGROVE_ROOTS
                || state.is(BlockTags.SAND) || block == Blocks.GRAVEL
                || block == Blocks.CLAY || block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL
                || block == Blocks.FARMLAND || block == Blocks.DIRT_PATH) {
            return new CachedBlockData(BlockCategory.SOFT, blockId);
        }

        // Remaining solid blocks are BUILDABLE
        return new CachedBlockData(BlockCategory.BUILDABLE, blockId);
    }

    private static ResourceLocation getBlockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }
}
