package com.evans.simulationmod.simulation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class SimulationCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("start_simulation")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            int x = IntegerArgumentType.getInteger(context, "x");
                                            int z = IntegerArgumentType.getInteger(context, "z");
                                            int radius = IntegerArgumentType.getInteger(context, "radius");
                                            return startSimulation(context.getSource(), x, z, radius);
                                        })
                                )
                        )
                )
        );

        dispatcher.register(Commands.literal("phantom")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context.getSource()))
                )
        );
    }

    private static int startSimulation(CommandSourceStack source, int x, int z, int radius) {
        ServerLevel level = source.getServer().overworld();
        SimulationManager manager = SimulationManager.get(level);

        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        ChunkPos center = new ChunkPos(chunkX, chunkZ);
        SimulationRegion region = new SimulationRegion(center, radius);
        manager.setRegion(region);

        source.sendSuccess(() -> Component.literal(
                String.format("Simulation region created at block (%d, %d) [chunk (%d, %d)] with radius %d (%d chunks)",
                        x, z, chunkX, chunkZ, radius, region.getChunkCount())
        ), true);

        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        SimulationManager manager = SimulationManager.get(level);
        SimulationRegion region = manager.getRegion();

        if (region == null) {
            source.sendSuccess(() -> Component.literal("No active simulation region."), false);
        } else {
            int cached = manager.getChunkCacheManager().getCachedChunkCount();
            int pending = manager.getPendingChangeQueue().getTotalChangeCount();
            int loaded = 0;
            for (ChunkPos cp : region.getChunks()) {
                if (level.hasChunk(cp.x, cp.z)) {
                    loaded++;
                }
            }
            int finalLoaded = loaded;
            source.sendSuccess(() -> Component.literal(
                    String.format("Simulation region: center=(%d, %d), radius=%d, chunks=%d",
                            region.getCenter().x, region.getCenter().z,
                            region.getRadius(), region.getChunkCount())
            ), false);
            source.sendSuccess(() -> Component.literal(
                    String.format("  Currently loaded: %d | Cached (unloaded): %d | Pending changes: %d",
                            finalLoaded, cached, pending)
            ), false);
            if (finalLoaded > 0 && cached == 0) {
                source.sendSuccess(() -> Component.literal(
                        "  Note: Chunks near spawn stay loaded. Try a region farther from spawn."
                ), false);
            }
        }

        return 1;
    }
}
