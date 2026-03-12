package com.evans.simulationmod.cache;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CacheTestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("phantom")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("test_cache")
                        .executes(context -> runCacheTest(context.getSource()))
                )
                .then(Commands.literal("test_mapper")
                        .executes(context -> runMapperTest(context.getSource()))
                )
        );
    }

    private static int runCacheTest(CommandSourceStack source) {
        int passed = 0;
        int failed = 0;

        // Test 1: Set and get blocks
        CachedChunk chunk = new CachedChunk(new ChunkPos(0, 0));
        CachedBlockData stone = new CachedBlockData(BlockCategory.STONE);
        CachedBlockData ore = new CachedBlockData(BlockCategory.ORE);
        CachedBlockData log = new CachedBlockData(BlockCategory.LOG);

        chunk.setBlock(5, 64, 5, stone);
        chunk.setBlock(6, 64, 6, ore);
        chunk.setBlock(7, 64, 7, log);

        if (chunk.getBlock(5, 64, 5).equals(stone)
                && chunk.getBlock(6, 64, 6).equals(ore)
                && chunk.getBlock(7, 64, 7).equals(log)) {
            source.sendSuccess(() -> Component.literal("[PASS] Set/get blocks at known positions"), false);
            passed++;
        } else {
            source.sendFailure(Component.literal("[FAIL] Set/get blocks returned wrong values"));
            failed++;
        }

        // Test 2: Default is AIR
        if (chunk.getBlock(0, 0, 0).equals(CachedBlockData.AIR)) {
            source.sendSuccess(() -> Component.literal("[PASS] Unset blocks default to AIR"), false);
            passed++;
        } else {
            source.sendFailure(Component.literal("[FAIL] Unset blocks not AIR"));
            failed++;
        }

        // Test 3: Heightmap raises
        CachedChunk hChunk = new CachedChunk(new ChunkPos(0, 0));
        hChunk.setBlock(0, 10, 0, stone);
        if (hChunk.getHeightAt(0, 0) == 10) {
            source.sendSuccess(() -> Component.literal("[PASS] Heightmap raises on solid block placement"), false);
            passed++;
        } else {
            source.sendFailure(Component.literal("[FAIL] Heightmap did not raise: got " + hChunk.getHeightAt(0, 0)));
            failed++;
        }

        // Test 4: Heightmap lowers on surface break
        hChunk.setBlock(0, 5, 0, stone);
        hChunk.setBlock(0, 10, 0, CachedBlockData.AIR);
        if (hChunk.getHeightAt(0, 0) == 5) {
            source.sendSuccess(() -> Component.literal("[PASS] Heightmap lowers on surface block break"), false);
            passed++;
        } else {
            source.sendFailure(Component.literal("[FAIL] Heightmap did not lower: got " + hChunk.getHeightAt(0, 0)));
            failed++;
        }

        // Test 5: Palette compression - many block types
        CachedChunkSection section = new CachedChunkSection();
        boolean paletteOk = true;
        for (int i = 0; i < 10; i++) {
            BlockCategory cat = BlockCategory.values()[i];
            section.setBlock(i, 0, 0, new CachedBlockData(cat));
        }
        for (int i = 0; i < 10; i++) {
            BlockCategory cat = BlockCategory.values()[i];
            if (!section.getBlock(i, 0, 0).getCategory().equals(cat)) {
                paletteOk = false;
                break;
            }
        }
        if (paletteOk) {
            source.sendSuccess(() -> Component.literal("[PASS] Palette handles 10+ unique block types"), false);
            passed++;
        } else {
            source.sendFailure(Component.literal("[FAIL] Palette corruption with many block types"));
            failed++;
        }

        int p = passed;
        int f = failed;
        source.sendSuccess(() -> Component.literal(
                String.format("Cache tests complete: %d passed, %d failed", p, f)), false);
        return failed == 0 ? 1 : 0;
    }

    private static int runMapperTest(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int passed = 0;
        int failed = 0;

        // Test known block states against expected categories
        Object[][] tests = {
                {Blocks.AIR.defaultBlockState(), BlockCategory.AIR, "Air"},
                {Blocks.STONE.defaultBlockState(), BlockCategory.STONE, "Stone"},
                {Blocks.DEEPSLATE.defaultBlockState(), BlockCategory.STONE, "Deepslate"},
                {Blocks.DIRT.defaultBlockState(), BlockCategory.SOFT, "Dirt"},
                {Blocks.GRASS_BLOCK.defaultBlockState(), BlockCategory.SOFT, "Grass Block"},
                {Blocks.SAND.defaultBlockState(), BlockCategory.SOFT, "Sand"},
                {Blocks.GRAVEL.defaultBlockState(), BlockCategory.SOFT, "Gravel"},
                {Blocks.OAK_LOG.defaultBlockState(), BlockCategory.LOG, "Oak Log"},
                {Blocks.BIRCH_LOG.defaultBlockState(), BlockCategory.LOG, "Birch Log"},
                {Blocks.OAK_LEAVES.defaultBlockState(), BlockCategory.LEAVES, "Oak Leaves"},
                {Blocks.DIAMOND_ORE.defaultBlockState(), BlockCategory.ORE, "Diamond Ore"},
                {Blocks.IRON_ORE.defaultBlockState(), BlockCategory.ORE, "Iron Ore"},
                {Blocks.COAL_ORE.defaultBlockState(), BlockCategory.ORE, "Coal Ore"},
                {Blocks.WATER.defaultBlockState(), BlockCategory.LIQUID, "Water"},
                {Blocks.LAVA.defaultBlockState(), BlockCategory.LIQUID, "Lava"},
                {Blocks.OBSIDIAN.defaultBlockState(), BlockCategory.HARD, "Obsidian"},
                {Blocks.BEDROCK.defaultBlockState(), BlockCategory.HARD, "Bedrock"},
                {Blocks.OAK_PLANKS.defaultBlockState(), BlockCategory.BUILDABLE, "Oak Planks"},
                {Blocks.BRICKS.defaultBlockState(), BlockCategory.BUILDABLE, "Bricks"},
        };

        for (Object[] test : tests) {
            BlockState state = (BlockState) test[0];
            BlockCategory expected = (BlockCategory) test[1];
            String name = (String) test[2];

            CachedBlockData result = BlockCategoryMapper.categorize(state);
            if (result.getCategory() == expected) {
                final int p = ++passed;
                source.sendSuccess(() -> Component.literal(
                        String.format("[PASS] %s -> %s", name, expected)), false);
            } else {
                final int f = ++failed;
                source.sendFailure(Component.literal(
                        String.format("[FAIL] %s -> expected %s, got %s", name, expected, result.getCategory())));
            }
        }

        // Test crop metadata
        BlockState wheat = Blocks.WHEAT.defaultBlockState();
        CachedBlockData cropData = BlockCategoryMapper.categorize(wheat);
        if (cropData.getCategory() == BlockCategory.CROP && cropData.getGrowthStage() >= 0) {
            source.sendSuccess(() -> Component.literal(
                    String.format("[PASS] Wheat -> CROP with growth stage %d", cropData.getGrowthStage())), false);
            passed++;
        } else {
            source.sendFailure(Component.literal("[FAIL] Wheat categorization or metadata"));
            failed++;
        }

        // Test ore preserves block identity
        CachedBlockData diamondOre = BlockCategoryMapper.categorize(Blocks.DIAMOND_ORE.defaultBlockState());
        if (diamondOre.getBlockId() != null && diamondOre.getBlockId().getPath().contains("diamond")) {
            source.sendSuccess(() -> Component.literal(
                    String.format("[PASS] Diamond ore preserves identity: %s", diamondOre.getBlockId())), false);
            passed++;
        } else {
            source.sendFailure(Component.literal("[FAIL] Diamond ore identity not preserved"));
            failed++;
        }

        // Scan blocks around the player for a real-world test
        BlockPos playerPos = BlockPos.containing(source.getPosition());
        int scanned = 0;
        int[] categoryCounts = new int[BlockCategory.values().length];
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    CachedBlockData data = BlockCategoryMapper.categorize(state);
                    categoryCounts[data.getCategory().ordinal()]++;
                    scanned++;
                }
            }
        }

        int totalScanned = scanned;
        source.sendSuccess(() -> Component.literal(
                String.format("Scanned %d blocks around player:", totalScanned)), false);
        for (BlockCategory cat : BlockCategory.values()) {
            int count = categoryCounts[cat.ordinal()];
            if (count > 0) {
                source.sendSuccess(() -> Component.literal(
                        String.format("  %s: %d", cat.name(), count)), false);
            }
        }

        int p = passed;
        int f = failed;
        source.sendSuccess(() -> Component.literal(
                String.format("Mapper tests complete: %d passed, %d failed", p, f)), false);
        return failed == 0 ? 1 : 0;
    }
}
