# Phantom Engine

**An out-of-chunk entity simulation engine for NeoForge 1.21.1**

Phantom Engine enables thousands of autonomous entities to mine, farm, harvest, and build in the Minecraft world — even when their chunks aren't loaded. It maintains an abstract simulation layer that mirrors unloaded chunk data, runs entity behavior against that cache, and seamlessly reconciles all changes back to the real world when chunks load again.

## The Problem

Minecraft entities freeze when their chunk unloads. If you want NPCs, workers, or autonomous agents that operate independently of player proximity, you're forced to keep their chunks loaded — which means tick overhead, memory pressure, and a hard ceiling on scale. At 1,000 entities across 1,000 chunks, a vanilla server collapses.

Phantom Engine decouples entity simulation from chunk loading entirely.

## How It Works

### Simulation Regions

A simulation region is a set of chunk positions where abstract simulation is active. Regions are defined via command and stored as a flat set of `ChunkPos` entries — no merging logic, no overlap conflicts. Running the command multiple times with different coordinates unions new chunks into the existing set.

```
/phantom start <x> <z> <radius>    — Add chunks to the simulation region
/phantom stop <x> <z> <radius>     — Remove chunks from the simulation region
/phantom status                     — Show active simulation stats
```

### The Cache Layer

When a chunk inside a simulation region unloads, Phantom Engine scans it and builds a lightweight representation:

- **Block category grid** — Every block compressed to a single byte (AIR, LIQUID, SOFT, STONE, HARD, ORE, LOG, LEAVES, CROP, BUILDABLE), with optional metadata for block identity, crop growth stage, and planting time
- **Heightmap** — Surface Y per column for fast terrain queries
- **Ore index** — Sparse list of ore positions and types for efficient mining queries

While a chunk is loaded, the real world is the source of truth. The cache only governs behavior in unloaded chunks.

### Abstract Simulation

Entities in the simulation are pure data objects — position, inventory, task queue, and pathfinding state. They are **not** Minecraft entities. They tick in an abstract loop driven by the server tick, subject to a per-tick time budget to protect TPS.

Entities interact with the world exclusively through `SimulationWorldView`, which reads and writes the block cache. They never touch chunk loading, real block state, or the entity tick pipeline.

### Reconciliation

Every block change made by a simulated entity gets recorded in a pending change queue. When a chunk loads (a player walks nearby), the engine replays those changes against the real world:

1. For each pending change, compare the real block against the expected "before" state
2. If it matches, apply the change (place/break the real block)
3. If it doesn't match (another player or mod changed it), skip and handle the conflict
4. Clear pending changes for that chunk
5. Remove the chunk from the cache (real world takes over)

This means a player walking into a simulation region sees the cumulative result of all entity activity — trees harvested, ores mined, farms tended, structures built — applied instantly on chunk load.

## Supported Activities

### Mining

Entities navigate underground by "mining" through the block cache. They consume blocks along their path based on block hardness, collect ore drops into their inventory, and avoid liquid blocks. Path cost is computed from cached block categories.

### Farming

Entities plant crops by writing CROP blocks into the cache with a planting timestamp. The simulation tick loop advances crop growth deterministically based on elapsed ticks (using averaged vanilla growth durations rather than random ticks). Entities harvest fully grown crops and replant.

### Tree Harvesting

Entities break LOG and LEAVES blocks in the cache, collecting wood. Saplings can be planted, and tree growth is simulated using simple templates (block offset lists for oak, birch, etc.) triggered after a fixed growth duration.

### Building

Entities place blocks from their inventory according to blueprint templates — lists of relative positions and block types. Each placement deducts material from inventory and enqueues a pending block change. Blueprints are data-driven and can represent arbitrary structures.

## Architecture

```
┌──────────────────────────────────────────────────┐
│                 SimulationManager                 │
│          (SavedData — persists to disk)           │
├──────────────┬──────────────┬────────────────────┤
│  Simulation  │    Chunk     │     Pending        │
│    Region    │    Cache     │     Change         │
│  (ChunkPos   │   Manager    │     Queue          │
│    Set)      │              │                    │
├──────────────┴──────┬───────┴────────────────────┤
│            SimulationWorldView                    │
│  getBlock / setBlock / findOres / getHeight       │
├───────────────────────┬──────────────────────────┤
│  SimulationTickHandler│     Entity Layer          │
│  (crop growth, tree   │  (pathfinding, tasks,     │
│   growth, time mgmt)  │   inventory, behavior)    │
└───────────────────────┴──────────────────────────┘

Chunk Lifecycle:
  Chunk Unloads → scan → populate cache
  Chunk Loads   → replay pending changes → remove from cache
```

## Implementation Phases

### Phase 1 — Region Definition & Persistence
Define simulation regions via command, persist to `SavedData`, load on server start.

### Phase 2 — Block Cache Data Structure
Palette-compressed per-section block storage with heightmap, categorization mapper, and read/write API.

### Phase 3 — Chunk Lifecycle Hooks
Cache chunks on unload, reconcile pending changes on load, enforce "only simulate unloaded chunks" invariant.

### Phase 4 — Cache Serialization
Full NBT serialization of block cache and pending change queue. Survives server restarts.

### Phase 5 — Simulation Block API
`SimulationWorldView` — the clean public interface for all entity interaction with cached world state.

### Phase 6 — Simulation Tick Loop
Server-tick-driven loop with time budgeting. Deterministic crop and tree growth simulation.

### Phase 7+ — Entity Simulation Layer
Pathfinding, task assignment, inventory management, mining/farming/harvesting/building behaviors. *(Designed separately on top of the Phase 1–6 foundation.)*

## Design Principles

- **Only simulate what's unloaded.** Loaded chunks are governed by the real world. The cache never conflicts with live state.
- **Entities are data, not Minecraft entities.** No `Entity` subclasses in the simulation. Real entities spawn only as visual representations when a player is nearby.
- **Reconciliation is conflict-aware.** Pending changes check preconditions before applying. Player actions always win conflicts.
- **Budget everything.** The tick loop enforces a millisecond cap per tick. Thousands of entities must not degrade TPS.
- **One flat region set.** No multi-region merging. Chunks either participate in simulation or they don't.

## Technical Details

- **Platform:** NeoForge 1.21.1
- **Persistence:** `SavedData` (NBT) attached to the overworld `ServerLevel`
- **Block storage:** Palette-compressed sections (mirrors vanilla chunk format), ~1 byte effective per block with good compression on homogeneous sections
- **Chunk events:** `ChunkEvent.Load` / `ChunkEvent.Unload` on the NeoForge event bus
- **Tick hook:** `ServerTickEvent.Post`

## Future Considerations

- **Entity-to-entity interaction** — Simulated entities communicating, trading, or collaborating on tasks
- **Cross-boundary pathfinding** — Entities moving between simulated and loaded chunks seamlessly
- **Real entity materialization** — Spawning visible Minecraft entities when players enter a simulation region, despawning when they leave
- **Configurable simulation fidelity** — Per-region tick rates or simplified behavior for distant/low-priority areas
- **External API** — Hooks for other mods or AI systems to issue commands to simulated entities
