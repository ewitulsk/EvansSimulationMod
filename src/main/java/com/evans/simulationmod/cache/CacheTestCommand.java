package com.evans.simulationmod.cache;

import com.evans.simulationmod.simulation.SimulationManager;
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
                .then(Commands.literal("test_lifecycle")
                        .executes(context -> runLifecycleTest(context.getSource()))
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

    private static int runLifecycleTest(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        SimulationManager manager = SimulationManager.get(level);
        int passed = 0;
        int failed = 0;

        // Test 1: Check simulation region exists
        if (manager.getRegion() == null) {
            source.sendFailure(Component.literal(
                    "[SKIP] No simulation region active. Run /start_simulation first."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                String.format("[INFO] Region: center=(%d,%d), radius=%d, chunks=%d",
                        manager.getRegion().getCenter().x, manager.getRegion().getCenter().z,
                        manager.getRegion().getRadius(), manager.getRegion().getChunkCount())), false);

        // Test 2: ChunkCacheManager state
        ChunkCacheManager cacheManager = manager.getChunkCacheManager();
        int cachedCount = cacheManager.getCachedChunkCount();
        source.sendSuccess(() -> Component.literal(
                String.format("[INFO] Cached chunks: %d", cachedCount)), false);

        // Test 3: Verify cached chunks are in the simulation region
        boolean allInRegion = true;
        for (ChunkPos pos : cacheManager.getAllChunks().keySet()) {
            if (!manager.isInSimulationRegion(pos)) {
                source.sendFailure(Component.literal(
                        String.format("[FAIL] Cached chunk (%d,%d) is NOT in simulation region", pos.x, pos.z)));
                allInRegion = false;
                failed++;
                break;
            }
        }
        if (allInRegion && cachedCount > 0) {
            source.sendSuccess(() -> Component.literal(
                    "[PASS] All cached chunks are within the simulation region"), false);
            passed++;
        } else if (cachedCount == 0) {
            source.sendSuccess(() -> Component.literal(
                    "[INFO] No cached chunks yet — walk away from the region to trigger unloads"), false);
        }

        // Test 4: Verify isSimulated consistency
        boolean simulatedOk = true;
        for (ChunkPos pos : cacheManager.getAllChunks().keySet()) {
            if (!manager.isSimulated(pos)) {
                source.sendFailure(Component.literal(
                        String.format("[FAIL] isSimulated false for cached chunk (%d,%d)", pos.x, pos.z)));
                simulatedOk = false;
                failed++;
                break;
            }
        }
        if (simulatedOk && cachedCount > 0) {
            source.sendSuccess(() -> Component.literal(
                    "[PASS] isSimulated returns true for all cached chunks"), false);
            passed++;
        }

        // Test 5: Spot-check a cached chunk's contents
        if (cachedCount > 0) {
            var firstEntry = cacheManager.getAllChunks().entrySet().iterator().next();
            ChunkPos samplePos = firstEntry.getKey();
            CachedChunk sampleChunk = firstEntry.getValue();

            // Count non-air blocks
            int nonAir = 0;
            int[] catCounts = new int[BlockCategory.values().length];
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int height = sampleChunk.getHeightAt(x, z);
                    if (height >= CachedChunk.MIN_Y) {
                        // Sample blocks at and around the surface
                        for (int y = Math.max(CachedChunk.MIN_Y, height - 5); y <= height; y++) {
                            CachedBlockData data = sampleChunk.getBlock(x, y, z);
                            if (data.getCategory() != BlockCategory.AIR) {
                                nonAir++;
                                catCounts[data.getCategory().ordinal()]++;
                            }
                        }
                    }
                }
            }

            if (nonAir > 0) {
                int finalNonAir = nonAir;
                source.sendSuccess(() -> Component.literal(
                        String.format("[PASS] Sample chunk (%d,%d) has %d non-air blocks near surface",
                                samplePos.x, samplePos.z, finalNonAir)), false);
                passed++;

                // Show category breakdown
                for (BlockCategory cat : BlockCategory.values()) {
                    int count = catCounts[cat.ordinal()];
                    if (count > 0) {
                        source.sendSuccess(() -> Component.literal(
                                String.format("  %s: %d", cat.name(), count)), false);
                    }
                }
            } else {
                source.sendFailure(Component.literal(
                        String.format("[FAIL] Sample chunk (%d,%d) has no non-air blocks — scan may have failed",
                                samplePos.x, samplePos.z)));
                failed++;
            }
        }

        // Test 6: PendingChangeQueue state
        PendingChangeQueue pendingQueue = manager.getPendingChangeQueue();
        int pendingCount = pendingQueue.getTotalChangeCount();
        source.sendSuccess(() -> Component.literal(
                String.format("[INFO] Pending changes: %d", pendingCount)), false);

        // Test 7: Manually enqueue and verify a pending change
        if (cachedCount > 0) {
            var testEntry = cacheManager.getAllChunks().entrySet().iterator().next();
            ChunkPos testChunkPos = testEntry.getKey();
            CachedChunk testChunk = testEntry.getValue();

            // Find a stone block in the cached chunk to test with
            BlockPos testBlockPos = null;
            for (int x = 0; x < 16 && testBlockPos == null; x++) {
                for (int z = 0; z < 16 && testBlockPos == null; z++) {
                    int height = testChunk.getHeightAt(x, z);
                    if (height >= CachedChunk.MIN_Y) {
                        CachedBlockData data = testChunk.getBlock(x, height, z);
                        if (data.getCategory() == BlockCategory.STONE || data.getCategory() == BlockCategory.SOFT) {
                            testBlockPos = new BlockPos(
                                    testChunkPos.getMinBlockX() + x, height,
                                    testChunkPos.getMinBlockZ() + z);
                        }
                    }
                }
            }

            if (testBlockPos != null) {
                CachedBlockData before = testChunk.getBlock(
                        testBlockPos.getX() & 15, testBlockPos.getY(), testBlockPos.getZ() & 15);
                CachedBlockData after = CachedBlockData.AIR;
                PendingBlockChange change = new PendingBlockChange(
                        testBlockPos, before, after, Blocks.AIR.defaultBlockState(), 0L);

                int beforeCount = pendingQueue.getTotalChangeCount();
                pendingQueue.enqueue(change);
                int afterCount = pendingQueue.getTotalChangeCount();

                if (afterCount == beforeCount + 1 && pendingQueue.hasChanges(testChunkPos)) {
                    source.sendSuccess(() -> Component.literal(
                            "[PASS] PendingChangeQueue enqueue works correctly"), false);
                    passed++;
                } else {
                    source.sendFailure(Component.literal("[FAIL] PendingChangeQueue enqueue failed"));
                    failed++;
                }

                // Clean up — remove the test change
                pendingQueue.clearChanges(testChunkPos);
                if (!pendingQueue.hasChanges(testChunkPos)) {
                    source.sendSuccess(() -> Component.literal(
                            "[PASS] PendingChangeQueue clearChanges works correctly"), false);
                    passed++;
                } else {
                    source.sendFailure(Component.literal("[FAIL] PendingChangeQueue clearChanges failed"));
                    failed++;
                }
            } else {
                source.sendSuccess(() -> Component.literal(
                        "[INFO] No suitable block found for pending change test"), false);
            }
        }

        int fp = passed;
        int ff = failed;
        source.sendSuccess(() -> Component.literal(
                String.format("Lifecycle tests complete: %d passed, %d failed", fp, ff)), false);
        return failed == 0 ? 1 : 0;
    }
}
