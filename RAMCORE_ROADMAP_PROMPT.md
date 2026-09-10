# RamCore 2.x Roadmap — Agent Task Prompt

You are working on **RamCore** (`willramanand/RamCore`), a Paper/Folia plugin utility library in Java 25 with Kotlin extensions, targeting Paper `26.1.2+`. The repo is a Maven project (`pom.xml`) shipped as a runtime plugin (`plugin.yml`, main class `dev.willram.ramcore.RamCore`). Consumer plugins extend `dev.willram.ramcore.RamPlugin`.

Read these before writing code:

- `README.md`
- `docs/API.md` — public API guide, package map, stability notes
- `docs/MODULE_BOUNDARIES.md` — intended module split and stability policy
- `docs/NMS_COMPATIBILITY.md`
- `todo.md` — everything listed there is complete; this document is the next phase

## Working rules

1. **Folia first.** Every new API that touches world/entity/player state must go through `dev.willram.ramcore.scheduler.Schedulers` and respect region ownership. Document which new APIs are Folia-safe by design and which need explicit scheduler context, matching the existing convention in `docs/API.md`.
2. **Follow the existing backend pattern.** `dev.willram.ramcore.loot.LootInstanceStore` + `InMemoryLootInstanceStore` and `dev.willram.ramcore.ai.MobGoalBackend` + `InMemoryMobGoalBackend` are the models: interface in the API, an in-memory implementation that runs in unit tests, and server-backed implementations behind it.
3. **Java API is the contract; Kotlin is sugar.** Add Kotlin DSL wrappers in `src/main/kotlin/dev/willram/ramcore/kotlin/RamCoreKtx.kt` only after the Java API is settled.
4. **Terminables everywhere.** Anything with lifecycle binds via `dev.willram.ramcore.terminable` so `RamPlugin.bind()` / `bindModule()` clean it up.
5. **Fail fast with actionable exceptions**, matching the style in `dev.willram.ramcore.exception` and `ConfigValidationException`.
6. **Every task ships with:** unit tests under `src/test/java` (in-memory backends, no live server), a section added to `docs/API.md`, and a stability level (stable / experimental / Paper-experimental / NMS-backed).
7. **Do not break existing public API** without a deprecation cycle. Additive changes only unless a task says otherwise.
8. Work one task at a time, in the order below unless told otherwise. After each task, summarize what changed, what was tested, and any open questions.

---

## Phase 1 — Foundations (do these first)

### Task 1.1 — Pluggable persistence layer

**Problem.** `dev.willram.ramcore.data.DataRepository` is a synchronous in-memory map with `setup()` / `saveAll()`. There is no async load/save, no dirty tracking, no SQL backend, and nothing wired to player login. `LootInstanceStore` is the only store interface; parties, cooldowns, and objective progress have no pluggable storage.

**Deliverables.**

- Introduce a generic `Store<K, V>` contract (async, returns `Promise`) with `load`, `save`, `delete`, `loadAll`, `saveDirty`, plus a `DirtyTracking` mixin.
- Implementations: `InMemoryStore`, `FileStore` (build on `FileDataRepository` + `GsonDataSerializer`), `SqlStore` (HikariCP; SQLite by default, MySQL/MariaDB/Postgres via config). Keep Hikari as an optional shaded dependency — do not force it on consumers who only use file storage.
- Retrofit `LootInstanceStore` to extend or adapt to the new contract without breaking callers.
- Add `PartyStore`, `CooldownStore`, `ObjectiveProgressStore` interfaces and migrate `PartyManager`, `CooldownTracker`, and `ObjectiveTracker` to use them (default: in-memory, so behavior is unchanged).
- Wire `DataRepositoryMigration` / `DataMigration` into the new stores with versioned schemas.
- Keep the old `DataRepository` working; mark it `@Deprecated` pointing at the replacement.

**Acceptance.** Existing tests pass. New tests cover dirty tracking, async save ordering, migration application, and SQLite round-trips (in-process SQLite is acceptable in tests).

### Task 1.2 — Player data lifecycle

**Problem.** Nothing loads player data before the player spawns.

**Deliverables.**

- `PlayerDataService`: registers typed player-data "profiles" (`PlayerDataKey<T>`), preloads them on `AsyncPlayerPreLoginEvent`, exposes them synchronously after join, marks dirty on write, saves on quit and on a configurable autosave interval, and evicts after quit.
- Handle the edge cases: login cancelled after preload, player joins before async load finishes (block or timeout with a clear kick message — make it configurable), server shutdown mid-save.
- Integrate with the `ServiceRegistry` (`dev.willram.ramcore.service`) so consumers can `services().get(PlayerDataService.class)`.

### Task 1.3 — Locale support in `MessageCatalog`

**Problem.** `dev.willram.ramcore.message.MessageCatalog` was designed for "eventual locale support" and has none.

**Deliverables.**

- Per-locale message files (`messages_en_US.yml`, etc.) with fallback chain: player locale → default locale → key name.
- Resolve player locale from `Player.locale()`; allow an override provider.
- Keep the existing single-file API working unchanged.
- Kotlin DSL update.

### Task 1.4 — Player text input for menus

**Problem.** `dev.willram.ramcore.menu` has `Gui`, `PaginatedMenu`, `MenuSession`, but no way to ask a player for free text.

**Deliverables.**

- `PlayerInput` API returning a `Promise<String>` (or typed `Promise<T>` with a parser) backed by: chat prompt (with cancel word and timeout), anvil GUI, and sign editor. Pick the backend per call.
- Integrate with `MenuSession` so a menu can suspend, collect input, and reopen.
- All three must be Folia-safe; chat capture must not leak into public chat.

### Task 1.5 — Vault and PlaceholderAPI bridges

**Problem.** `dev.willram.ramcore.integration.StandardIntegrations` detects Vault and PlaceholderAPI but nothing uses them.

**Deliverables.**

- `Economy` abstraction (`balance`, `withdraw`, `deposit`, `has`, `format`) with a Vault-backed provider and an in-memory provider for tests. Wire the existing money reward type in `dev.willram.ramcore.reward` to it.
- `PlaceholderAPI` expansion that exposes RamCore placeholders (party, cooldown, objective progress, region name) and lets consumer plugins register their own through one API.
- Both must degrade cleanly when the plugin is absent (use the `IntegrationRegistry` capability checks).

### Task 1.6 — Config-backed content definitions

**Problem.** `dev.willram.ramcore.content.ContentRegistry` and `dev.willram.ramcore.template` are code-only. `todo.md` deferred config loading.

**Deliverables.**

- `ContentLoader` that reads YAML/HOCON (Configurate is already a dependency) into: items, loot tables, rewards, regions, templates, NPCs, displays.
- Use `TemplateComposer` inheritance semantics so config entries can `extends:` a template.
- Validation runs through the existing template validation and surfaces every error at once, with file + path, before anything registers.
- `/ramcore diagnostics validate` command extension that dry-runs a content directory.

---

## Phase 2 — Engineering quality

### Task 2.1 — Test modernization and `ramcore-test` artifact

- Migrate tests from JUnit 4 (`org.junit.Test`) to JUnit 5.
- Extract the in-memory backends and any test fakes into a separately published `ramcore-test` module so downstream plugins can unit test against RamCore without a server.
- Add a `FakeScheduler` that runs `Schedulers` calls deterministically (tick-stepped) for tests.

### Task 2.2 — Multi-module Maven split

`docs/MODULE_BOUNDARIES.md` describes boundaries the build does not enforce. Split into:

- `ramcore-api` — interfaces, records, no ProtocolLib or NMS imports
- `ramcore-paper` — Paper-backed implementations, the plugin itself
- `ramcore-protocol` — everything under `dev.willram.ramcore.protocol` and `packet` (ProtocolLib-dependent)
- `ramcore-nms` — `dev.willram.ramcore.nms`, `reflect`, `shadows`, `nbt`
- `ramcore-kotlin` — the Kotlin DSL
- `ramcore-test` — from Task 2.1

Enforce with Maven dependency scoping; the API module must not compile if it imports from the others. Add a publishing block (`distributionManagement`) or document JitPack usage in `README.md`. Question if Gradle may be more beneficial? Debate the pros and cons of switching.

### Task 2.3 — Scheduler and Promise audit

`dev/willram/ramcore/promise/Promise.java` (~1,450 lines) and `scheduler/Schedulers.java` (~1,100 lines) are the highest-risk files for Folia bugs. Audit for: callbacks running on the wrong thread/region, entity-scoped tasks outliving entity removal, exception swallowing, and cancellation races. Write regression tests for each finding using the `FakeScheduler` from 2.1.

### Task 2.4 — Kotlin coroutines bridge

- `suspend fun <T> Promise<T>.await()`
- Dispatchers for global, async, region (`Location`), entity, and player contexts, backed by `Schedulers`.
- Structured cancellation tied to terminables.
- Optional dependency: `ramcore-kotlin` only.

### Task 2.5 — Operational basics

- bStats metrics (opt-out in config).
- Update checker against GitHub releases (async, once per startup, no auto-download).
- Cross-server messaging abstraction: `MessageBus` with Redis pub/sub and Paper plugin-messaging channel implementations; in-memory for tests. Later tasks (parties, cooldowns) can opt into it.

### Task 2.6 — Version support decision

Currently Java 25 / Paper 26.1 only. Decide and document one of: (a) stay latest-only with a clear policy, or (b) maintain a backport branch for Java 21 / Paper 1.21.x. If (b), identify the API surface that differs and isolate it behind the existing `NmsAccessStrategy` / version adapters.

---

## Phase 3 — Gameplay platform features

### Task 3.1 — Content-defined items → auto-generated resource pack

**Idea.** A custom item declares its texture/model in code or config; RamCore builds the pack, hashes it, hosts it, and prompts players.

- Build on `dev.willram.ramcore.resourcepack.ResourcePackAssetRegistry`, `ResourcePackItems`, `ResourcePackSounds`, and `ResourcePackPromptTracker`.
- Generate item model JSON using the modern item-model / custom-model-data component path (check current Paper data component API; wrap it behind `dev.willram.ramcore.item.component`).
- Pack builder writes to disk, produces SHA-1, serves via a tiny embedded HTTP server (optional) or writes to a configured path for external hosting.
- Rebuild is incremental and diffable; report what changed.

### Task 3.2 — Instanced worlds

**Idea.** Dungeons: clone a template world per party, run an encounter in it, tear it down.

- `WorldInstanceService`: `create(templateName, options) -> Promise<WorldInstance>`, `WorldInstance.close()`.
- Copy region files from a template directory, load with a unique name, apply a `Region` boundary and rule set from `dev.willram.ramcore.region`, bind lifecycle to a `PartyGroup` and/or `EncounterInstance`.
- Eviction: teleport players out, unload, delete files. Handle crashes/restarts by sweeping orphaned instance worlds on startup.
- Folia: world creation is a global-region operation; document it.

### Task 3.3 — Player ability framework

**Idea.** `dev.willram.ramcore.encounter.EncounterAbility` is boss-only. Give players the same.

- `Ability` definition: id, cooldown (use `CooldownTracker`), resource cost (hook to Task 3.4 stats), cast time, targeting (use `dev.willram.ramcore.selector`), effects (use `PresentationEffects`), and an `execute(AbilityContext)` callback.
- Triggers: item use, hotbar slot, command, keybind-via-swap-hands, custom.
- `AbilityRegistry` in the content registry; config-loadable via Task 1.6.
- Interrupts, channelling, and combo chains as experimental extensions.

### Task 3.4 — Custom stat system

**Idea.** `dev.willram.ramcore.combat.AttributeModifierSpec` only covers vanilla attributes.

- `Stat` registry (crit chance, crit damage, mana, lifesteal, elemental resistances, custom).
- Stat sources: equipped items (read via data components / PDC), buffs (`AttributeBuff`), party bonuses, region rules.
- `StatSnapshot` computed per player, cached, invalidated on equipment change.
- Feed into `DamageProfile` / `CombatControls` so damage calculations consume custom stats.

### Task 3.5 — Dialogue system

- Branching `Dialogue` graph: nodes with text (Adventure components via `MessageCatalog`), choices, conditions (reuse `LootCondition`-style predicates or `PermissionRequirement`), and actions (rewards, objective progress, commands, open menu).
- Presentation: chat with clickable choices, and a menu-backed mode.
- Attach to `NpcSpec` click handlers and to `ObjectiveDefinition` steps.
- Config-loadable via Task 1.6.

### Task 3.6 — Session recorder / debug timeline

- Per-player ring buffer of structured events: rewards granted, loot rolls (with seed and table id), region enter/exit, ability casts, objective updates, cooldown hits, menu opens.
- `/ramcore diagnostics timeline <player> [n]` dumps it; `dev.willram.ramcore.diagnostics` already has the export machinery for issue-safe pastes.
- Off by default in production, cheap when on (bounded buffer, no allocation on the hot path beyond the record).

### Task 3.7 — Hot-reloadable content packs with diff reporting

- `ContentReloadService.reload(pack)` returns a `ContentDiff`: added, removed, changed entries; broken references; and which live objects (menus, holograms, NPCs, regions) were rebuilt or could not be.
- Live objects re-resolve their template on reload rather than holding stale copies.

### Task 3.8 — Real-time and cron scheduling

- `RealTimeScheduler`: run at wall-clock times / cron expressions / intervals, persistent across restarts (uses Task 1.1 stores for "last run").
- Use cases: world events, trader restocks, weekly rotations, daily objectives.
- Timezone-aware; missed runs while offline can be skipped or caught up per job.

---

## Suggested order

1. 1.1 → 1.2 (persistence, then player data)
2. 2.1 → 2.3 (tests + scheduler audit; do this before adding more scheduler users)
3. 1.3, 1.4, 1.5, 1.6 (any order)
4. 2.2, 2.4, 2.5, 2.6
5. Phase 3 in listed order — 3.4 before 3.3, and 1.6 before 3.5/3.7

## Definition of done for the whole roadmap

- `docs/API.md` has a section for every new subsystem with a stability level and Folia note.
- `ramcore-test` lets a consumer plugin unit-test commands, menus, rewards, and stores without a server.
- A sample consumer plugin under `examples/` demonstrates: config-loaded custom item with generated pack, a player ability using a custom stat, an NPC dialogue that starts an objective, a party entering an instanced dungeon, and a reward paid through the Vault economy bridge.
