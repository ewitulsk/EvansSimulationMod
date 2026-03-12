# Phantom Engine — Development TODO

## Phase 1: Simulation Region Definition & Persistence

### PE-1: Create SimulationRegion data class
**Type:** Task
**Priority:** Highest
**Description:**
Create `SimulationRegion` data class holding a center `ChunkPos`, integer radius, and `Set<ChunkPos>` of all chunk positions within the boundary.

**Acceptance Criteria:**
- [ ] Class stores center `ChunkPos` and integer radius
- [ ] Constructor calculates all `ChunkPos` within the circular/square radius and stores them in a `Set<ChunkPos>`
- [ ] `contains(ChunkPos)` method returns correct results for chunks inside and outside the boundary
- [ ] Immutable after construction (or controlled mutation via clear/rebuild)

**Package:** `com.evans.simulationmod.simulation`

---

### PE-2: Create SimulationManager as SavedData singleton
**Type:** Task
**Priority:** Highest
**Depends on:** PE-1
**Description:**
Create `SimulationManager` extending `SavedData`, attached to the overworld `ServerLevel`. Holds the `SimulationRegion`. Manages lifecycle: create region, persist, load on server start.

**Acceptance Criteria:**
- [ ] Extends `SavedData` and attaches to the overworld `ServerLevel`
- [ ] Holds a nullable `SimulationRegion`
- [ ] `save(CompoundTag)` serializes region center and radius to NBT
- [ ] `load(CompoundTag)` deserializes and reconstructs the `SimulationRegion` including the `ChunkPos` set
- [ ] Accessible via a static `get(ServerLevel)` helper
- [ ] Calls `setDirty()` when region is created or modified

---

### PE-3: Implement /start_simulation command
**Type:** Task
**Priority:** Highest
**Depends on:** PE-2
**Description:**
Register a `/start_simulation <x> <z> <radius>` server command that creates a `SimulationRegion`, stores it in `SimulationManager`, and marks `SavedData` dirty.

**Acceptance Criteria:**
- [ ] Command registered on the NeoForge event bus via `RegisterCommandsEvent`
- [ ] Accepts three integer arguments: x, z, radius
- [ ] Creates a `SimulationRegion` and stores it in `SimulationManager`
- [ ] Sends feedback message to the command sender with region info (chunk count, center, radius)
- [ ] Region persists across server restart (shut down, restart, verify region still exists)

---

### PE-4: Manual verification — region persistence round-trip
**Type:** Test / QA
**Priority:** High
**Depends on:** PE-3
**Description:**
Verify end-to-end that running `/start_simulation`, shutting down the server, and restarting preserves the region.

**Acceptance Criteria:**
- [ ] Run `/start_simulation 0 0 5`, confirm feedback message
- [ ] Stop server, restart server
- [ ] Run `/phantom status` (or check logs) — region is intact with correct center, radius, and chunk count
- [ ] `contains()` returns true for chunks inside and false for chunks outside

---

## Phase 2: Block Cache Data Structure

### PE-5: Define BlockCategory enum and CachedBlockData record
**Type:** Task
**Priority:** Highest
**Description:**
Create `BlockCategory` enum with values: `AIR, LIQUID, SOFT, STONE, HARD, ORE, CROP, LOG, LEAVES, BUILDABLE`. Create `CachedBlockData` record/class holding the category byte, an optional `ResourceLocation` for block identity, and optional crop metadata (growth stage int, planted-at-tick long).

**Acceptance Criteria:**
- [ ] `BlockCategory` enum with 10 values, each mapped to a byte
- [ ] `CachedBlockData` stores category + optional `ResourceLocation` + optional crop growth stage + optional planted-at-tick
- [ ] Equality and hashCode work correctly (needed for palette deduplication)
- [ ] A static `AIR` constant for the default empty block

**Package:** `com.evans.simulationmod.cache`

---

### PE-6: Implement CachedChunkSection with palette compression
**Type:** Task
**Priority:** Highest
**Depends on:** PE-5
**Description:**
Create `CachedChunkSection` storing a 16x16x16 palette-compressed array of `CachedBlockData`. Use a palette list + `BitStorage` (packed long array) indexing into the palette, mirroring vanilla's approach.

**Acceptance Criteria:**
- [ ] Stores 4096 block entries (16^3) using palette compression
- [ ] `getBlock(int x, int y, int z)` returns the correct `CachedBlockData`
- [ ] `setBlock(int x, int y, int z, CachedBlockData)` updates the entry, adding to palette if new
- [ ] Palette grows as needed when new unique block data is added
- [ ] A section of all-air uses minimal memory (single palette entry)

---

### PE-7: Implement CachedChunk with heightmap
**Type:** Task
**Priority:** Highest
**Depends on:** PE-6
**Description:**
Create `CachedChunk` wrapping an array of `CachedChunkSection` spanning full world height (24 sections for Y -64 to 319). Stores a heightmap (256-entry int array — max solid Y per column) and the chunk position.

**Acceptance Criteria:**
- [ ] Covers Y range -64 to 319 (24 sections)
- [ ] `getBlock(int x, int y, int z)` delegates to the correct section
- [ ] `setBlock(int x, int y, int z, CachedBlockData)` delegates to the correct section and updates the heightmap
- [ ] Heightmap correctly tracks the highest non-AIR, non-LIQUID block per column
- [ ] Setting a block above current surface raises the heightmap
- [ ] Breaking the surface block (setting to AIR) scans downward to find the new surface

---

### PE-8: Implement BlockCategoryMapper
**Type:** Task
**Priority:** Highest
**Depends on:** PE-5
**Description:**
Create `BlockCategoryMapper` with a static `categorize(BlockState) -> CachedBlockData` method. Use block tags (`#minecraft:ores`, `#minecraft:logs`, `#minecraft:crops`, etc.) and `BlockState` properties rather than hardcoding specific blocks.

**Acceptance Criteria:**
- [ ] Maps ores (tagged `#minecraft:ores` or similar) → `ORE` with `ResourceLocation` preserved
- [ ] Maps logs (`#minecraft:logs`) → `LOG`
- [ ] Maps leaves (`#minecraft:leaves`) → `LEAVES`
- [ ] Maps crops (`#minecraft:crops` or `CropBlock` instance check) → `CROP` with growth stage extracted
- [ ] Maps liquids (`blockState.liquid()` or `FluidState`) → `LIQUID`
- [ ] Maps air → `AIR`
- [ ] Maps stone/deepslate/netherrack → `STONE`
- [ ] Maps dirt/sand/gravel/grass → `SOFT`
- [ ] Maps obsidian/bedrock → `HARD`
- [ ] Remaining solid blocks → `BUILDABLE`

---

### PE-9: Unit-style verification of cache data structures
**Type:** Test / QA
**Priority:** High
**Depends on:** PE-7, PE-8
**Description:**
Verify cache correctness through in-game or test-harness validation.

**Acceptance Criteria:**
- [ ] Create a `CachedChunk`, write blocks at known positions, read them back — values match
- [ ] Heightmap updates correctly when setting and clearing blocks
- [ ] `BlockCategoryMapper` correctly categorizes: stone, diamond ore, water, wheat, oak log, air

---

## Phase 3: Chunk Lifecycle Hooks

### PE-10: Create ChunkCacheManager
**Type:** Task
**Priority:** Highest
**Depends on:** PE-7
**Description:**
Create `ChunkCacheManager` owned by `SimulationManager`. Holds a `HashMap<ChunkPos, CachedChunk>`. Exposes `cacheChunk(ChunkPos, LevelChunk)` (scans and stores) and `getChunk(ChunkPos) -> CachedChunk?`.

**Acceptance Criteria:**
- [ ] `cacheChunk` iterates every block in the `LevelChunk`, categorizes via `BlockCategoryMapper`, and populates a `CachedChunk`
- [ ] Skips entirely-air sections for performance
- [ ] `getChunk` returns the cached chunk or null
- [ ] `removeChunk(ChunkPos)` removes a chunk from the cache

---

### PE-11: Implement chunk unload hook — cache population
**Type:** Task
**Priority:** Highest
**Depends on:** PE-10, PE-2
**Description:**
Subscribe to `ChunkEvent.Unload` on the NeoForge event bus. When a chunk in the simulation region unloads, scan it and populate the cache via `ChunkCacheManager`.

**Acceptance Criteria:**
- [ ] Event handler checks `simulationRegion.contains(chunkPos)` before caching
- [ ] Only caches chunks from the overworld `ServerLevel` (not client-side)
- [ ] Full chunk scan populates the `CachedChunk` correctly
- [ ] Cache contains the chunk after unload event fires

---

### PE-12: Create PendingChangeQueue
**Type:** Task
**Priority:** Highest
**Description:**
Create `PendingChangeQueue` holding a `HashMap<ChunkPos, List<PendingBlockChange>>`. `PendingBlockChange` stores: `BlockPos`, expected-before `CachedBlockData`, after `CachedBlockData`, the real `BlockState` to write on reconciliation, and a tick timestamp.

**Acceptance Criteria:**
- [ ] `enqueue(PendingBlockChange)` adds to the correct chunk's list
- [ ] `getChanges(ChunkPos) -> List<PendingBlockChange>` returns pending changes or empty list
- [ ] `clearChanges(ChunkPos)` removes all pending changes for a chunk
- [ ] `PendingBlockChange` record/class has all required fields

---

### PE-13: Implement chunk load hook — reconciliation
**Type:** Task
**Priority:** Highest
**Depends on:** PE-11, PE-12
**Description:**
Subscribe to `ChunkEvent.Load`. When a chunk in the simulation region loads, replay pending changes against the real world, then remove the chunk from the cache.

**Acceptance Criteria:**
- [ ] For each pending change: read real block, compare category to expected "before" state
- [ ] If match → apply change via `level.setBlock()`
- [ ] If mismatch → skip and log a conflict warning
- [ ] After processing, clear pending changes for that chunk
- [ ] Remove `CachedChunk` from cache (real world takes over)
- [ ] `SimulationManager.isSimulated(ChunkPos)` returns true only if chunk is in region AND in cache (unloaded)

---

### PE-14: Verification — chunk lifecycle round-trip
**Type:** Test / QA
**Priority:** High
**Depends on:** PE-13
**Description:**
End-to-end verification of the cache-on-unload → reconcile-on-load cycle.

**Acceptance Criteria:**
- [ ] Enter simulation region, place distinctive blocks, walk away (chunks unload) — cache contains placed blocks with correct categories
- [ ] Manually inject a pending change (stone → air) — walk back (chunk loads) — verify block changed in real world
- [ ] Test conflict: inject pending change, externally modify that block to something else, load chunk — verify pending change is skipped and conflict is logged

---

## Phase 4: Cache Serialization

### PE-15: Serialize CachedChunk to NBT
**Type:** Task
**Priority:** Highest
**Depends on:** PE-6, PE-7
**Description:**
Implement NBT serialization for `CachedChunkSection` and `CachedChunk`. Write each section's palette as a list of `CachedBlockData` entries and the packed long array as a long array NBT tag. Skip entirely-air sections.

**Acceptance Criteria:**
- [ ] `CachedBlockData` serializes to NBT: category byte + optional resource location string + optional crop metadata
- [ ] `CachedChunkSection` serializes palette list + packed long array
- [ ] `CachedChunk` serializes all non-empty sections + heightmap + chunk position
- [ ] Entirely-air sections are omitted from serialization
- [ ] Deserialization reconstructs identical `CachedChunk`

---

### PE-16: Serialize PendingChangeQueue to NBT
**Type:** Task
**Priority:** Highest
**Depends on:** PE-12
**Description:**
Serialize pending block changes as part of `SimulationManager`'s `SavedData`. Use `NbtUtils.writeBlockState()` for the real `BlockState` and category bytes for `CachedBlockData`.

**Acceptance Criteria:**
- [ ] Each `PendingBlockChange` serializes: position, before/after cache data, real `BlockState`, tick timestamp
- [ ] Full queue serializes as a map of chunk positions → list of changes
- [ ] Deserialization reconstructs the queue identically

---

### PE-17: Integrate cache serialization into SimulationManager SavedData
**Type:** Task
**Priority:** Highest
**Depends on:** PE-15, PE-16, PE-2
**Description:**
Wire `ChunkCacheManager` and `PendingChangeQueue` serialization into `SimulationManager.save()` / `load()`. Implement dirty-flag throttling (batch to once per tick).

**Acceptance Criteria:**
- [ ] `SimulationManager.save()` writes region + cache + pending queue
- [ ] `SimulationManager.load()` restores all three
- [ ] `setDirty()` is called at most once per tick (throttled via a tick-end flag)
- [ ] Populate cache with known data, save, restart — cache contents identical
- [ ] Queue pending changes, restart — still pending, applied on next chunk load
- [ ] Empty region serializes and deserializes cleanly (no NPEs)

---

### PE-18: Verification — serialization round-trip
**Type:** Test / QA
**Priority:** High
**Depends on:** PE-17
**Description:**
Verify persistence of cache and pending changes across server restarts.

**Acceptance Criteria:**
- [ ] Populate cache, stop server, restart — cache data intact
- [ ] Enqueue pending changes, restart — changes still queued and apply on chunk load
- [ ] Empty state (no region, no cache, no pending) deserializes without errors

---

## Phase 5: Simulation Block API

### PE-19: Implement SimulationWorldView
**Type:** Task
**Priority:** Highest
**Depends on:** PE-10, PE-12
**Description:**
Create `SimulationWorldView` — the public API for entity simulation code to read and modify cached world state. Wraps `ChunkCacheManager` and `PendingChangeQueue`.

**Methods:**
- `getBlock(BlockPos) -> CachedBlockData?` — returns cached block, or null if chunk isn't simulated
- `setBlock(BlockPos, CachedBlockData, BlockState)` — updates cache + enqueues pending change
- `getHeightAt(int x, int z) -> Integer?` — surface Y, null if not cached
- `findBlocks(ChunkPos, BlockCategory) -> List<BlockPos>` — scan cached chunk for all blocks of a category
- `findBlocksInRadius(BlockPos center, int radius, BlockCategory) -> List<BlockPos>` — multi-chunk search
- `isSimulated(BlockPos) -> boolean` — whether position is in an unloaded, cached chunk

**Acceptance Criteria:**
- [ ] `setBlock` updates the cache AND enqueues a `PendingBlockChange` with correct before/after states
- [ ] `findBlocks` returns correct positions after cache population
- [ ] `getBlock` returns null for positions in loaded chunks (not in cache)
- [ ] `setBlock` updates the heightmap correctly
- [ ] `findBlocksInRadius` searches across multiple cached chunks
- [ ] Entity simulation code never needs to touch `ChunkCacheManager` or `PendingChangeQueue` directly

---

### PE-20: Verification — SimulationWorldView API
**Type:** Test / QA
**Priority:** High
**Depends on:** PE-19
**Description:**
Validate the world view API against known cache states.

**Acceptance Criteria:**
- [ ] Set blocks via API, verify cache updated and pending change enqueued
- [ ] `findBlocks` returns correct positions after manual cache population
- [ ] `getBlock` returns null for loaded-chunk positions
- [ ] Heightmap updates correctly via `setBlock` (place above surface → raises, break surface → lowers)

---

## Phase 6: Simulation Tick Loop

### PE-21: Implement SimulationTickHandler with time budgeting
**Type:** Task
**Priority:** Highest
**Depends on:** PE-19
**Description:**
Create `SimulationTickHandler` subscribing to `ServerTickEvent.Post`. Maintains a persisted tick counter. Processes time-based simulation each tick with a configurable time budget (default 5ms) to protect TPS.

**Acceptance Criteria:**
- [ ] Subscribes to `ServerTickEvent.Post`
- [ ] Increments and persists a simulation tick counter in `SavedData`
- [ ] Enforces a per-tick time budget (stops processing when budget exceeded, defers to next tick)
- [ ] Provides a hook point for crop growth, tree growth, and future entity processing

---

### PE-22: Implement deterministic crop growth simulation
**Type:** Task
**Priority:** Highest
**Depends on:** PE-21, PE-19
**Description:**
In the tick loop, iterate cached chunks and find `CROP` blocks. Advance growth stage deterministically based on `currentTick - plantedAtTick` vs fixed growth durations (e.g., wheat = ~24,000 ticks). Mark fully grown crops as harvestable.

**Acceptance Criteria:**
- [ ] Crops planted via `SimulationWorldView.setBlock()` have `plantedAtTick` set to current simulation tick
- [ ] Each tick, crops with elapsed ticks > stage threshold advance their growth stage
- [ ] Fully grown crops are distinguishable (max growth stage) for future harvesting logic
- [ ] Growth is deterministic — same elapsed ticks always produce same growth stage

---

### PE-23: Implement deterministic tree growth simulation
**Type:** Task
**Priority:** Highest
**Depends on:** PE-21, PE-19
**Description:**
Saplings planted at tick T become trees at tick T + growth_duration. On growth, replace sapling with LOG blocks and add LEAVES blocks using simple tree templates (relative position lists for oak, birch shapes).

**Acceptance Criteria:**
- [ ] Tree templates defined as lists of relative positions + block data (LOG positions, LEAVES positions)
- [ ] At least two templates: oak and birch
- [ ] Sapling growth triggered when `currentTick - plantedAtTick > growthDuration`
- [ ] Growth writes LOG and LEAVES blocks into cache via `SimulationWorldView`
- [ ] Each block placement enqueues a pending change for reconciliation

---

### PE-24: Verification — tick loop and growth simulation
**Type:** Test / QA
**Priority:** High
**Depends on:** PE-22, PE-23
**Description:**
Validate time-based simulation correctness and performance.

**Acceptance Criteria:**
- [ ] Plant a crop via `setBlock()`, advance simulation ticks — growth stage increases on schedule
- [ ] Plant a sapling, advance past growth threshold — LOG and LEAVES appear in cache and pending changes
- [ ] With 10,000+ crops in cache, tick handler respects time budget (does not exceed 5ms, defers remaining work)

---

## Phase 7+: Entity Simulation Layer (Future — Design Separately)

### PE-25: Design entity simulation architecture
**Type:** Spike / Design
**Priority:** Medium
**Depends on:** PE-24
**Description:**
Design the entity simulation layer built on top of `SimulationWorldView`. Covers: entity data model (position, inventory, task queue), pathfinding through cached blocks, task assignment system, and behavior implementations (mining, farming, harvesting, building).

**Scope:**
- Entity data model (pure data, not Minecraft entities)
- Pathfinding using cached block categories and heightmap
- Task queue and assignment system
- Mining behavior (navigate underground, collect ores, avoid liquids)
- Farming behavior (plant, wait for growth, harvest, replant)
- Tree harvesting behavior (break logs/leaves, collect wood, plant saplings)
- Building behavior (place blocks from inventory per blueprint templates)
- Inventory management
- Blueprint/template system for building
- Entity materialization (spawning visible entities when player is nearby)

*This phase will be broken into its own set of tickets after Phases 1–6 are complete.*

---

## Summary

| Phase | Tickets | Key Deliverable |
|-------|---------|-----------------|
| 1 — Region & Persistence | PE-1 to PE-4 | Simulation region defined, persisted, loaded |
| 2 — Block Cache | PE-5 to PE-9 | Palette-compressed block cache with categorization |
| 3 — Chunk Lifecycle | PE-10 to PE-14 | Auto-cache on unload, reconcile on load |
| 4 — Serialization | PE-15 to PE-18 | Cache & pending changes survive restarts |
| 5 — Simulation API | PE-19 to PE-20 | Clean `SimulationWorldView` interface |
| 6 — Tick Loop | PE-21 to PE-24 | Time-budgeted tick loop with crop/tree growth |
| 7+ — Entity Layer | PE-25 | Design spike (future) |

**Total: 25 tickets across 7 phases**
