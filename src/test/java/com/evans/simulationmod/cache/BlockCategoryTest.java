package com.evans.simulationmod.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockCategoryTest {

    @Test
    void allCategoriesHaveUniqueIds() {
        byte[] seen = new byte[BlockCategory.values().length];
        for (BlockCategory cat : BlockCategory.values()) {
            for (int i = 0; i < seen.length; i++) {
                if (i < cat.ordinal() && BlockCategory.values()[i].getId() == cat.getId()) {
                    fail("Duplicate ID " + cat.getId() + " for " + cat + " and " + BlockCategory.values()[i]);
                }
            }
        }
    }

    @Test
    void fromIdRoundTrips() {
        for (BlockCategory cat : BlockCategory.values()) {
            assertEquals(cat, BlockCategory.fromId(cat.getId()));
        }
    }

    @Test
    void fromIdReturnsAirForUnknown() {
        assertEquals(BlockCategory.AIR, BlockCategory.fromId((byte) 99));
    }

    @Test
    void hasTenValues() {
        assertEquals(10, BlockCategory.values().length);
    }
}
