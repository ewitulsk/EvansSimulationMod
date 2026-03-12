package com.evans.simulationmod.simulation;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;

public class SimulationManager extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_KEY = "phantom_simulation";

    @Nullable
    private SimulationRegion region;

    public SimulationManager() {
        this.region = null;
    }

    public static SimulationManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SimulationManager::new, SimulationManager::load),
                DATA_KEY
        );
    }

    public static SimulationManager load(CompoundTag tag, HolderLookup.Provider provider) {
        SimulationManager manager = new SimulationManager();
        if (tag.contains("Region")) {
            manager.region = SimulationRegion.load(tag.getCompound("Region"));
            LOGGER.info("Loaded simulation region: center=({}, {}), radius={}, chunks={}",
                    manager.region.getCenter().x, manager.region.getCenter().z,
                    manager.region.getRadius(), manager.region.getChunkCount());
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        if (region != null) {
            tag.put("Region", region.save(new CompoundTag()));
        }
        return tag;
    }

    public void setRegion(SimulationRegion region) {
        this.region = region;
        setDirty();
    }

    @Nullable
    public SimulationRegion getRegion() {
        return region;
    }

    public boolean isInSimulationRegion(net.minecraft.world.level.ChunkPos pos) {
        return region != null && region.contains(pos);
    }
}
