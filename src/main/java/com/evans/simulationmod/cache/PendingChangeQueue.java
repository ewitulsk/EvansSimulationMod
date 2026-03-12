package com.evans.simulationmod.cache;

import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingChangeQueue {
    private final Map<ChunkPos, List<PendingBlockChange>> changes = new HashMap<>();

    public void enqueue(PendingBlockChange change) {
        ChunkPos chunkPos = new ChunkPos(change.getPos());
        changes.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(change);
    }

    public List<PendingBlockChange> getChanges(ChunkPos pos) {
        return changes.getOrDefault(pos, Collections.emptyList());
    }

    public void clearChanges(ChunkPos pos) {
        changes.remove(pos);
    }

    public boolean hasChanges(ChunkPos pos) {
        List<PendingBlockChange> list = changes.get(pos);
        return list != null && !list.isEmpty();
    }

    public int getTotalChangeCount() {
        return changes.values().stream().mapToInt(List::size).sum();
    }

    public Map<ChunkPos, List<PendingBlockChange>> getAllChanges() {
        return changes;
    }
}
