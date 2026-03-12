package com.evans.simulationmod.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class PendingBlockChange {
    private final BlockPos pos;
    private final CachedBlockData expectedBefore;
    private final CachedBlockData after;
    private final BlockState realBlockState;
    private final long tick;

    public PendingBlockChange(BlockPos pos, CachedBlockData expectedBefore, CachedBlockData after,
                              BlockState realBlockState, long tick) {
        this.pos = pos;
        this.expectedBefore = expectedBefore;
        this.after = after;
        this.realBlockState = realBlockState;
        this.tick = tick;
    }

    public BlockPos getPos() {
        return pos;
    }

    public CachedBlockData getExpectedBefore() {
        return expectedBefore;
    }

    public CachedBlockData getAfter() {
        return after;
    }

    public BlockState getRealBlockState() {
        return realBlockState;
    }

    public long getTick() {
        return tick;
    }
}
