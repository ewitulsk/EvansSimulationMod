package com.evans.simulationmod.cache;

import net.minecraft.util.SimpleBitStorage;

import java.util.ArrayList;
import java.util.List;

public class CachedChunkSection {
    private static final int SIZE = 16;
    private static final int TOTAL_BLOCKS = SIZE * SIZE * SIZE; // 4096

    private List<CachedBlockData> palette;
    private SimpleBitStorage storage;

    public CachedChunkSection() {
        this.palette = new ArrayList<>();
        this.palette.add(CachedBlockData.AIR);
        this.storage = new SimpleBitStorage(1, TOTAL_BLOCKS);
    }

    public CachedChunkSection(List<CachedBlockData> palette, long[] data, int bits) {
        this.palette = new ArrayList<>(palette);
        this.storage = new SimpleBitStorage(bits, TOTAL_BLOCKS, data);
    }

    public CachedBlockData getBlock(int x, int y, int z) {
        int index = getIndex(x, y, z);
        int paletteIndex = storage.get(index);
        return palette.get(paletteIndex);
    }

    public void setBlock(int x, int y, int z, CachedBlockData data) {
        int paletteIndex = getPaletteIndex(data);
        int index = getIndex(x, y, z);
        storage.set(index, paletteIndex);
    }

    private int getPaletteIndex(CachedBlockData data) {
        int idx = palette.indexOf(data);
        if (idx != -1) {
            return idx;
        }

        palette.add(data);
        int newSize = palette.size();
        int requiredBits = ceilLog2(newSize);

        if (requiredBits > storage.getBits()) {
            SimpleBitStorage newStorage = new SimpleBitStorage(requiredBits, TOTAL_BLOCKS);
            for (int i = 0; i < TOTAL_BLOCKS; i++) {
                newStorage.set(i, storage.get(i));
            }
            storage = newStorage;
        }

        return newSize - 1;
    }

    public boolean isEmpty() {
        return palette.size() == 1 && palette.get(0).equals(CachedBlockData.AIR);
    }

    public List<CachedBlockData> getPalette() {
        return palette;
    }

    public long[] getRawData() {
        return storage.getRaw();
    }

    public int getBits() {
        return storage.getBits();
    }

    private static int getIndex(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    private static int ceilLog2(int value) {
        if (value <= 1) return 1;
        return Integer.SIZE - Integer.numberOfLeadingZeros(value - 1);
    }
}
