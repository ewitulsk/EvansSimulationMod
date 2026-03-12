package com.evans.simulationmod.simulation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SimulationRegion {
    private final ChunkPos center;
    private final int radius;
    private final Set<ChunkPos> chunks;

    public SimulationRegion(ChunkPos center, int radius) {
        this.center = center;
        this.radius = radius;
        this.chunks = new HashSet<>();

        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
    }

    public boolean contains(ChunkPos pos) {
        return chunks.contains(pos);
    }

    public ChunkPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public Set<ChunkPos> getChunks() {
        return Collections.unmodifiableSet(chunks);
    }

    public int getChunkCount() {
        return chunks.size();
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putInt("CenterX", center.x);
        tag.putInt("CenterZ", center.z);
        tag.putInt("Radius", radius);
        return tag;
    }

    public static SimulationRegion load(CompoundTag tag) {
        int centerX = tag.getInt("CenterX");
        int centerZ = tag.getInt("CenterZ");
        int radius = tag.getInt("Radius");
        return new SimulationRegion(new ChunkPos(centerX, centerZ), radius);
    }
}
