# RamCore Project TODO

## High-Value Improvements

- [ ] Add a service registry and module lifecycle system.
  - Register services, dependencies, startup order, and shutdown behavior.
  - Make plugin composition cleaner than manual wiring in `onEnable`.

- [ ] Build a typed configuration API.
  - Support defaults, reloads, validation, and clear startup errors.
  - Start with Bukkit YAML, with room for JSON or TOML later.

- [ ] Add a centralized message and translation system.
  - Use Adventure `Component` output consistently.
  - Support prefixes, colors, placeholders, MiniMessage, reusable message keys, and eventual locale support.

- [ ] Improve permission helpers.
  - Add utilities for permission checks, fallback messages, grouped permissions, and command visibility.
  - Integrate with the command API where appropriate.

- [ ] Polish the scheduler facade.
  - Provide a high-level Folia-aware API for global, async, region, entity, and player scheduling.
  - Make common scheduling paths consistent and hard to misuse.

## Command And Developer Experience

- [ ] Continue improving the command API.
  - Review additional Brigadier interop use cases.
  - Add more examples for command modules, suggestions, requirements, and async execution.
  - Consider command cooldowns and permission-aware generated help extensions.

- [ ] Expand the Kotlin DSL.
  - Cover config, scheduling, events, messages, menus, and persistent data helpers.
  - Keep Java APIs as the primary public contract and Kotlin as ergonomic sugar.

- [ ] Add stronger developer-facing error handling.
  - Fail fast when APIs are misused.
  - Prefer actionable exceptions that explain what the plugin developer should change.

## RamCore 2.0 Platform Features

- [ ] Add a content registry system.
  - Provide central registries for custom items, mobs, bosses, loot tables, menus, effects, regions, quests, commands, rewards, and templates.
  - Support namespaced keys, plugin ownership, lifecycle binding, validation, conflict detection, reload behavior, and lookup by id.
  - Allow consuming plugins to register content in code first, then later support config-backed content definitions.

- [ ] Add a template system.
  - Support reusable templates for items, mobs, bosses, loot tables, GUIs, NPCs, holograms, display entities, regions, rewards, and effects.
  - Include inheritance or composition so common settings can be shared across related templates.
  - Add validation and clear diagnostics for missing references, invalid materials/entities, broken loot entries, and unsupported version-specific fields.

- [ ] Add a region and rule engine.
  - Provide lightweight regions independent of WorldGuard, with optional WorldGuard integration later.
  - Support cuboid, polygon, sphere/cylinder, chunk, world, and composite regions.
  - Add enter, exit, stay, tick, block, entity, combat, command, loot, and interaction rules.
  - Support priorities, inheritance, deny/allow/pass results, temporary regions, arena boundaries, dungeon rooms, and region-scoped metadata.

- [ ] Add a generic reward engine.
  - Support items, money, XP, commands, permissions, cooldown resets, loot rolls, messages, titles, sounds, particles, boss-bar updates, and custom callbacks.
  - Add reward contexts for player, party, region, mob, command, quest, boss, dungeon, and event rewards.
  - Support conditional rewards, weighted rewards, guaranteed rewards, preview mode, dry-run validation, and audit logging.
  - Keep this separate from instanced loot so quests, crates, bosses, dungeons, events, and commands can reuse the same reward pipeline.

- [ ] Add an effect and presentation API.
  - Unify titles, subtitles, action bars, boss bars, chat messages, sounds, particles, glowing, fake equipment, entity animations, and timed sequences.
  - Support per-player and grouped presentation contexts.
  - Add reusable scripted sequences for polished gameplay feedback.
  - Make all timed effects terminable and scheduler-aware.

- [ ] Add a display entity and hologram toolkit.
  - Support text displays, item displays, block displays, hologram stacks, transforms, interpolation, billboarding, brightness, shadows, and background styling.
  - Add per-player visibility, lifecycle cleanup, Folia-safe updates, and template-backed displays.
  - Provide simple APIs for common labels, floating damage numbers, objective markers, NPC nameplates, region labels, and loot previews.

- [ ] Add an NPC toolkit.
  - Support static NPCs, click handlers, nameplates, look-at-player behavior, dialogue triggers, per-player visibility, and lifecycle cleanup.
  - Explore skin support where practical.
  - Provide packet-backed fake NPCs as an advanced mode while keeping server-backed entities as the simpler default.
  - Integrate NPCs with commands, regions, quests/objectives, rewards, and dialogue systems.

- [ ] Add a party and group API.
  - Support parties, temporary groups, leaders, invites, membership rules, group chat hooks, shared loot scope, shared objectives, and group teleport.
  - Add damage contribution tracking for bosses and events.
  - Add eligibility checks for loot, quests, arenas, dungeons, and rewards.
  - Keep storage optional so simple plugins can use in-memory groups and larger plugins can persist them.

- [ ] Add an objective and quest progress API.
  - Provide reusable progress trackers for killing mobs, collecting items, entering regions, interacting with blocks/entities, running commands/actions, timers, and chained objectives.
  - Support player and party progress, objective reset, partial completion, hidden objectives, and progress events.
  - Keep the core generic enough for achievements, tutorials, battle passes, daily tasks, dungeons, and quests.

- [ ] Add a boss and encounter framework.
  - Support boss phases, ability timers, target rules, damage contribution, arena boundaries, enrage timers, boss bars, wipe detection, reset conditions, and reward distribution.
  - Integrate with mob AI templates, NMS entity control, regions, parties, instanced loot, rewards, and presentation effects.
  - Provide reusable ability primitives such as targeted attack, area attack, summon adds, teleport, shield, heal, phase transition, and scripted sequence.

- [ ] Add an optional integration layer.
  - Provide capability-checked adapters for LuckPerms, Vault, PlaceholderAPI, MiniPlaceholders, WorldGuard, ProtocolLib, Citizens, ItemsAdder, and Oraxen.
  - Keep integrations optional and isolated from core modules.
  - Expose a consistent capability API so consuming plugins can check whether an integration is available and what it supports.

- [ ] Add resource pack helper utilities.
  - Track custom model data, item model keys, sound keys, font glyph keys, asset ids, and pack metadata.
  - Add helpers for resource-pack prompts, accepted/declined tracking, and timeout handling.
  - Integrate with item templates, custom items, GUI icons, NPCs, effects, and display entities.

## Gameplay Plugin Utilities

- [ ] Improve cooldown and rate-limit APIs.
  - Support player cooldowns, command cooldowns, action throttling, expiry callbacks, and grouped cooldown keys.

- [ ] Build an inventory and menu framework.
  - Include declarative buttons, pagination, click handling, viewer state, update ticks, and Folia-safe execution.

- [ ] Improve persistent data container helpers.
  - Add typed keys, optional values, defaults, namespacing, and object serialization helpers.

- [ ] Add reusable player and entity selector utilities.
  - Expose selector behavior outside the command API for other systems.

- [ ] Add a placeholder and text formatting layer.
  - Reuse placeholder resolution across messages, command output, scoreboards, menus, and configs.
  - Prefer typed context objects over raw string maps where practical.

- [ ] Add an instanced loot toolkit.
  - Provide reusable infrastructure for per-player, per-party, per-group, and global loot instances.
  - Keep gameplay rules in consuming plugins; RamCore should expose the engine, context objects, persistence hooks, and events.
  - Support loot generation contexts using player, party, region, world, mob, killer, damage contributors, difficulty, luck, permissions, cooldowns, and custom metadata.
  - Add weighted loot tables, loot entries, roll modifiers, conditional entries, guaranteed entries, bonus rolls, and empty-roll handling.
  - Support personal reward containers, per-player mob drops, block/chest loot snapshots, delayed rewards, claim tokens, and expiring loot.
  - Add claim tracking, anti-dupe guards, pending/unclaimed loot persistence, cleanup scheduling, and audit/debug output.
  - Expose events such as personal loot generate, roll, claim, expire, reroll, and deny events.
  - Consider multiple renderers: virtual inventory, physical drops with owner metadata, real container snapshots, and optional packet-level illusions for advanced use cases.

## Advanced NMS And Versioned Minecraft Utilities

- [ ] Define an NMS access strategy before adding more internals.
  - Prefer Paper API first, then RamCore adapters, then guarded reflection or version-specific implementations only when necessary.
  - Keep all NMS entry points behind public RamCore interfaces so plugins do not depend on raw `net.minecraft` classes.
  - Add version capability checks, clear unsupported-version errors, and diagnostics output for which adapters loaded.
  - Consider separate packages such as `nms.api`, `nms.reflect`, and `nms.v1_21_x` so unstable internals stay isolated.

- [ ] Build a mob AI facade on top of Paper Goals, with NMS fallbacks for missing behavior.
  - Add fluent helpers for adding, removing, replacing, pausing, and restoring goals by key, type, priority, or owner plugin.
  - Add goal snapshots so a plugin can temporarily override vanilla AI and restore the original goal set safely.
  - Add common custom goals: follow entity, guard area, patrol waypoints, flee from entity/type, attack target selector, look-at target, return home, leash-to-region, idle animation, and conditional goal wrappers.
  - Add goal decorators: cooldown, timeout, random chance, predicate activation, distance gate, line-of-sight gate, health gate, and metadata/PDC gate.
  - Expose better debugging for active goals, blocked goal types, priority conflicts, current target, and last activation/stop reason.

- [ ] Explore NMS Brain, Memory, Sensor, and Activity adapters for modern mobs.
  - Paper Goals cover classic pathfinder goals, but many newer mobs rely heavily on brain activities, memories, and sensors.
  - Add safe wrappers for reading/writing selected memory modules, clearing memories, forcing activities, and inspecting sensor state.
  - Start read-only where possible: current activity, memories present, nearby visible players/entities, attack target memory, walk target memory, home/meeting/job-site memories, and cooldown memories.
  - Only add mutation APIs behind explicit version support because brain internals change often.

- [ ] Improve pathfinding utilities beyond Paper's basic Pathfinder API.
  - Add path request objects with destination, speed, max distance, stuck timeout, repath interval, and completion callbacks.
  - Add Folia-safe path tasks bound to the entity scheduler.
  - Add path progress tracking: next point, final point, remaining distance, stuck detection, failure reason, and cancellation reason.
  - Explore NMS navigation controls for node penalties, door behavior, water/lava handling, flying/swimming navigation, and custom movement controllers.
  - Add waypoint routes and patrol controllers that can reuse the mob AI facade.

- [ ] Add advanced entity control utilities.
  - Expose safe wrappers for entity look control, move control, jump control, target selection, anger state, persistence, despawn behavior, pickup rules, equipment drop chances, and invulnerability flags.
  - Add helpers for spawning configured mobs from templates.
  - Add entity templates covering attributes, equipment, data components/items, scoreboard tags, PDC, metadata, AI profile, pathfinding profile, passenger stack, and spawn reason.
  - Add temporary entity modifiers with automatic restoration through terminables.

- [ ] Add attribute and combat behavior helpers.
  - Provide typed builders for vanilla attributes, modifiers, operation types, and temporary buffs.
  - Add damage profile helpers for custom mobs: knockback resistance, attack reach where available, movement speed, follow range, armor/toughness, scale, safe fall distance, gravity, and step height.
  - Explore NMS-only hooks for attack cooldowns, attack animations, hurt timers, invulnerability frames, and custom damage routing where Paper does not expose enough control.

- [ ] Build item data component utilities around Paper's experimental Data Components API.
  - Wrap component reads/writes behind RamCore interfaces because Paper marks the API experimental and version-specific.
  - Add component patch helpers: read prototype, read patch, diff patch, reset to prototype, remove component, copy selected components, and compare while ignoring selected components.
  - Add high-level builders for food, tool rules, weapon stats, equippable assets, glider behavior, consumables, rarity, enchantment glint override, max stack size, max damage, custom model data, lore, books, containers, and bundle/container contents.
  - Add migration helpers between legacy `ItemMeta`/NBT-style data and data components.
  - Add serialization for component patches so items can be stored in configs or repositories with clear version metadata.

- [ ] Expand item NBT and serialization support.
  - Add safe item SNBT import/export for debugging and migration tools.
  - Add item diff utilities that compare type, amount, meta, PDC, data components, enchantments, attributes, and raw NBT when available.
  - Add namespaced custom item identity helpers that survive cloning, stacking, serialization, and anvil/item-meta edits.
  - Add item template presets for custom tools, weapons, armor, consumables, keys, quest items, menu items, and invisible marker items.

- [ ] Add world and block internals where Paper API is thin.
  - Explore block entity NBT helpers for signs, containers, spawners, skulls, banners, lecterns, and command blocks.
  - Add safe spawner configuration helpers for spawn potentials, delays, ranges, entity templates, and weighted spawn entries.
  - Add structure and region helpers for snapshotting block states, restoring regions, and applying block entity data safely.
  - Keep all world/block mutation APIs scheduler-aware for Folia region ownership.

- [ ] Add loot table and trade manipulation utilities.
  - Add builders for loot pools, entries, conditions, functions, and weighted results where Bukkit/Paper APIs are cumbersome.
  - Add villager/wandering trader trade builders with demand, price multiplier, uses, experience, ingredients, result components, and restock behavior.
  - Explore NMS fallback only for fields missing from Bukkit/Paper.

- [ ] Add packet-level utilities only where ProtocolLib or Paper events are insufficient.
  - Build on the existing ProtocolLib integration for scoreboard, entity metadata previews, glowing, fake equipment, fake entities, and per-player visual state.
  - Keep packet utilities clearly separated from server-state mutation APIs.
  - Add diagnostics for packet adapter availability and client-version assumptions.

- [ ] Add NMS test and compatibility infrastructure.
  - Add a small compatibility matrix documenting which NMS features work on each supported Minecraft/Paper version.
  - Add startup self-tests for reflective handles and versioned adapters.
  - Add example modules for each advanced feature so breakage is obvious during manual server testing.
  - Add a policy for removing or quarantining broken internals on new Minecraft releases.

## Infrastructure And Diagnostics

- [ ] Expand plugin diagnostics.
  - Include scheduler mode, Folia/Paper detection, loaded modules, registered services, command tree dump, memory/cache stats, and version info.
  - Add admin/developer commands to inspect the item in hand, selected entity, block target, PDC data, data components, raw NBT where supported, active mob goals, brain memories where supported, and scheduler ownership/context.
  - Add commands to list registered content, validate templates/configs, test loot table rolls, preview rewards, preview effects, inspect regions, and dump integration capabilities.
  - Add safe debug exports that can be pasted into issues without exposing secrets.

- [ ] Improve the data repository layer.
  - Add simple file-backed repositories first.
  - Consider async save queues, dirty tracking, and migration hooks.

- [ ] Improve event subscription utilities.
  - Support automatic lifecycle cleanup, filtered listeners, one-shot listeners, priority helpers, and terminable binding.

## Release Readiness

- [ ] Confirm Java 25 is acceptable for downstream consumers.
- [ ] Test the plugin manually on both Folia and plain Paper.
- [ ] Review shade warnings and optionally exclude duplicate dependency metadata.
- [ ] Decide whether diagnostics should remain enabled by default for public releases.
- [ ] Define the RamCore 2.0 module boundaries before implementing platform features.
- [ ] Decide which features belong in core, optional modules, integrations, or examples.
- [ ] Define API stability levels for stable, experimental, Paper-experimental, and NMS-backed APIs.
- [ ] Document which 2.0 APIs are Folia-safe by design and which require explicit scheduler context.
