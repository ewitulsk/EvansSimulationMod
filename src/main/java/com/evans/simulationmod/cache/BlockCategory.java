package com.evans.simulationmod.cache;

public enum BlockCategory {
    AIR((byte) 0),
    LIQUID((byte) 1),
    SOFT((byte) 2),
    STONE((byte) 3),
    HARD((byte) 4),
    ORE((byte) 5),
    CROP((byte) 6),
    LOG((byte) 7),
    LEAVES((byte) 8),
    BUILDABLE((byte) 9);

    private final byte id;

    BlockCategory(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static BlockCategory fromId(byte id) {
        for (BlockCategory category : values()) {
            if (category.id == id) {
                return category;
            }
        }
        return AIR;
    }
}
