# NMS Compatibility

RamCore treats NMS-adjacent features as capability-checked internals. Paper APIs are preferred, RamCore adapters are second, guarded reflection is third, and version-specific implementations are last.

Current build target:

- Java runtime and bytecode target: `25`
- Paper API dependency: `26.1.2.build.60-stable`
- Optional packet dependency: ProtocolLib `5.4.0`
- Runtime version source: `NmsAccess.runtimeRegistry().minecraftVersion()`

Java 25 is an intentional downstream requirement for this release line. Paper documents Java 25 as the recommended runtime for `26.1+`, and RamCore's Maven compiler and Kotlin JVM targets both emit Java 25 bytecode.

## Compatibility Matrix

This matrix documents the current RamCore support surface after the relevant facade registration methods have been called. Plugins should still generate runtime diagnostics with `NmsAccessRegistry#compatibilityMatrix(...)` because server jars, ProtocolLib availability, and future versioned adapters can change the effective result.

The older broad buckets remain for compatibility. More specific capabilities let consuming plugins require only the exact feature they use.

### AI And Brain

| Capability | Current Status | Tier | Notes |
| --- | --- | --- | --- |
| `MOB_GOALS` | `SUPPORTED` | `PAPER_API` | Paper exposes goal add/remove/query through `MobAi.registerPaperCapability(...)`. |
| `MOB_GOAL_SNAPSHOTS` | `PARTIAL` | `PAPER_API` | RamCore can restore tracked goals; Paper does not expose complete vanilla priority snapshots. |
| `MOB_GOAL_DIAGNOSTICS` | `PARTIAL` | `PAPER_API` | Paper exposes registered/running goals; RamCore adds tracked-goal conflict diagnostics. |
| `BRAIN_MEMORY` | `PARTIAL` | `PAPER_API` | Broad selected memory-key read/write support. |
| `BRAIN_MEMORY_READ` | `PARTIAL` | `PAPER_API` | `LivingEntity#getMemory` for selected `MemoryKey` values. |
| `BRAIN_MEMORY_WRITE` | `PARTIAL` | `PAPER_API` | `LivingEntity#setMemory` for selected `MemoryKey` values. |
| `BRAIN_SENSORS` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `BRAIN_SENSOR_READ` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `BRAIN_ACTIVITY` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `BRAIN_ACTIVITY_READ` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `BRAIN_ACTIVITY_WRITE` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |

### Pathfinding And Entity Control

| Capability | Current Status | Tier | Notes |
| --- | --- | --- | --- |
| `PATHFINDING` | `PARTIAL` | `PAPER_API` | Paper movement, path points, door toggles, float toggles, and route tasks are wrapped. |
| `PATHFINDING_ROUTES` | `SUPPORTED` | `RAMCORE_ADAPTER` | RamCore waypoint routes and patrol controllers. |
| `PATHFINDING_NAVIGATION_PROFILE` | `PARTIAL` | `PAPER_API` | Door/floating navigation toggles are public; node penalties need an adapter. |
| `PATHFINDING_NODE_CONTROL` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `ENTITY_MOVEMENT_CONTROLLERS` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Raw move/look/jump/flying/swimming controllers need adapters. |
| `ENTITY_CONTROL` | `PARTIAL` | `PAPER_API` | Stable flags, targets, equipment, attributes, spawning, and temporary snapshots are wrapped. |
| `ENTITY_SPAWNING` | `SUPPORTED` | `PAPER_API` | Configured spawn callbacks and spawn reasons. |
| `ENTITY_TEMPLATES` | `SUPPORTED` | `RAMCORE_ADAPTER` | RamCore templates apply public Bukkit/Paper state. |
| `ENTITY_TEMPORARY_MODIFIERS` | `SUPPORTED` | `RAMCORE_ADAPTER` | Terminable snapshot/restore for supported entity state. |
| `ENTITY_EQUIPMENT` | `PARTIAL` | `PAPER_API` | Equipment and drop chance configuration. |
| `ENTITY_LOOK_CONTROL` | `PARTIAL` | `PAPER_API` | Location/teleport orientation is public; raw look controller needs an adapter. |
| `ENTITY_MOVE_CONTROL` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `ENTITY_JUMP_CONTROL` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |

### Combat And Items

| Capability | Current Status | Tier | Notes |
| --- | --- | --- | --- |
| `ENTITY_ATTRIBUTES` | `PARTIAL` | `PAPER_API` | Broad vanilla attributes, modifiers, damage, and invulnerability controls. |
| `ATTRIBUTE_MODIFIERS` | `SUPPORTED` | `PAPER_API` | Vanilla attribute instances and modifiers. |
| `DAMAGE_APPLICATION` | `PARTIAL` | `PAPER_API` | Damage calls and `DamageSource` where Paper exposes it. |
| `COMBAT_INVULNERABILITY_TICKS` | `PARTIAL` | `PAPER_API` | No-damage tick controls; hurt timer internals need adapters. |
| `COMBAT_ATTACK_COOLDOWN` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `COMBAT_HURT_TIMERS` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `COMBAT_DAMAGE_ROUTING` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `ITEM_DATA_COMPONENTS` | `PARTIAL` | `PAPER_API` | Paper experimental components are wrapped behind RamCore value objects. |
| `ITEM_COMPONENT_PATCHES` | `PARTIAL` | `PAPER_API` | Component read, diff, copy, reset, and patch helpers. |
| `ITEM_COMPONENT_PROFILES` | `PARTIAL` | `PAPER_API` | High-level profiles over Paper experimental component types. |
| `ITEM_NBT` | `PARTIAL` | `PAPER_API` | Binary serialization, meta/PDC/components, diffs, identities; raw SNBT needs an adapter. |
| `ITEM_BINARY_SERIALIZATION` | `SUPPORTED` | `PAPER_API` | Bukkit/Paper binary item serialization. |
| `ITEM_SNBT` | `UNSUPPORTED` | `GUARDED_REFLECTION` | Requires a guarded NMS adapter. |
| `ITEM_DIFFS` | `SUPPORTED` | `RAMCORE_ADAPTER` | RamCore item snapshot diffing. |
| `CUSTOM_ITEM_IDENTITY` | `SUPPORTED` | `RAMCORE_ADAPTER` | Namespaced item identity through metadata/PDC. |

### World, Loot, Trades, And Packets

| Capability | Current Status | Tier | Notes |
| --- | --- | --- | --- |
| `BLOCK_ENTITY_NBT` | `PARTIAL` | `PAPER_API` | Typed block-state snapshots and spawner helpers; raw SNBT needs an adapter. |
| `BLOCK_ENTITY_SNAPSHOTS` | `PARTIAL` | `PAPER_API` | Typed block states and PDC for common block entities. |
| `RAW_BLOCK_ENTITY_SNBT` | `UNSUPPORTED` | `GUARDED_REFLECTION` | Requires a guarded NMS adapter. |
| `STRUCTURE_SNAPSHOTS` | `PARTIAL` | `RAMCORE_ADAPTER` | Typed block snapshot capture/restore; raw block entity SNBT remains gated. |
| `SPAWNER_CONFIGURATION` | `PARTIAL` | `PAPER_API` | Spawner delays, ranges, counts, and spawn potentials. |
| `LOOT_TABLES` | `PARTIAL` | `PAPER_API` | RamCore loot pools and Paper merchant recipes are wrapped; vanilla datapack mutation is not. |
| `RAMCORE_LOOT_GENERATION` | `SUPPORTED` | `RAMCORE_ADAPTER` | Side-effect-free RamCore loot tables, pools, conditions, functions, and generated rewards. |
| `LOOT_INSTANCES` | `SUPPORTED` | `RAMCORE_ADAPTER` | Claimable generated loot instances with scope, expiry, reroll, and duplicate-claim policies. |
| `VANILLA_LOOT_TABLE_MUTATION` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `TRADE_RECIPES` | `SUPPORTED` | `PAPER_API` | Paper merchant recipe builders and application. |
| `TRADE_RESTOCK_INTERNALS` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Requires a guarded version-specific adapter. |
| `PACKETS` | `PARTIAL` or `UNSUPPORTED` | `RAMCORE_ADAPTER` | Partial only when ProtocolLib is available. |
| `PACKET_VISUAL_STATE` | `SUPPORTED` | `RAMCORE_ADAPTER` | Viewer-scoped visual state planning without server-state mutation. |
| `PACKET_PROTOCOLLIB_TRANSPORT` | `PARTIAL` or `UNSUPPORTED` | `RAMCORE_ADAPTER` | Partial only when ProtocolLib is available. |
| `PACKET_VERSIONED_FACTORIES` | `UNSUPPORTED` | `VERSIONED_IMPLEMENTATION` | Concrete packet factories require guarded versioned adapters. |

## Startup Self-Tests

Register every capability facade used by the plugin, then run a plan at startup:

```java
NmsAccessRegistry nms = NmsAccess.runtimeRegistry();
MobAi.registerPaperCapability(nms);
MobBrains.registerPaperCapabilities(nms);
Pathfinders.registerPaperCapability(nms);
EntityControls.registerPaperCapability(nms);
CombatControls.registerPaperCapability(nms);
ItemComponents.registerPaperCapability(nms);
ItemNbt.registerPaperCapability(nms);
WorldBlocks.registerPaperCapability(nms);
InstancedLoot.registerPaperCapability(nms);
Packets.registerProtocolCapability(nms, Bukkit.getPluginManager().getPlugin("ProtocolLib") != null);

NmsSelfTestPlan plan = NmsSelfTestPlan.builder()
        .required("mob-goals", NmsCapability.MOB_GOALS, "Custom mobs require Paper mob goals.")
        .optional("packets", NmsCapability.PACKETS, "Packet previews can be disabled.")
        .build();

NmsSelfTestReport report = nms.selfTest(plan);
for (String line : report.lines()) {
    logger.info(line);
}
```

Optional tests pass even when unavailable; required tests fail when a capability is unknown or unsupported.

## Quarantine Policy

On a new Minecraft or Paper release:

- `SUPPORTED` capabilities may stay enabled after startup self-tests pass.
- `PARTIAL` capabilities may stay enabled with a warning and a manual example-module check.
- `UNSUPPORTED` capabilities must be disabled or hidden from public workflows.
- `UNKNOWN` capabilities require manual review before being enabled.
- Broken guarded reflection or versioned adapters should be removed from registration or narrowed with version bounds instead of silently falling through.
- Public APIs must keep returning Bukkit, Paper, or RamCore types; raw NMS handles remain internal to adapters.

Use `NmsQuarantinePolicy.strict()` when deciding whether to expose advanced commands or modules for a capability.

## Manual Example Modules

`NmsExampleModules.defaults()` lists the manual smoke-test surfaces that should be exercised on Paper and Folia before release:

- Mob goals
- Brain memory
- Pathfinding routes and navigation profile toggles
- Entity control, templates, temporary modifiers, and equipment
- Attributes, damage application, and invulnerability ticks
- Item components, binary serialization, diffs, identities, and raw SNBT gating
- Block entity snapshots, structure restore, spawner configuration, and raw SNBT gating
- RamCore loot generation, loot instances, Paper trades, and vanilla loot mutation gating
- Packet visual state, ProtocolLib transport, and versioned packet-factory gating
