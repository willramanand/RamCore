# RamCore 2.x Roadmap — Execution Plan

Companion to `RAMCORE_ROADMAP_PROMPT.md`. That file says *what*; this file says *in which order, why, and how each task is cut*. It was written after a full survey of the codebase at commit `b0cb86a` plus the uncommitted `2.0.0` version bump, so every design note below starts from what actually exists rather than from the roadmap's description of it.

Baseline verified on 2026-09-10: `mvn test` with JDK 25 → 196 tests, 0 failures. Maven must be run with `JAVA_HOME` pointing at a JDK 25 (`C:\Program Files\Microsoft\jdk-25.0.1.8-hotspot` locally); the shell default is GraalVM 21 and will fail on `--release 25`.

---

## 1. Ground truth vs. roadmap premises

The roadmap was written from memory of the code. These are the places where the code differs, and they change how the tasks should be cut.

| Roadmap says | Code actually has | Consequence |
| --- | --- | --- |
| 1.1: `DataRepository` has "no async load/save, no dirty tracking" | `FileDataRepository` already has `queueSave`, `queueSaveDirty`, `saveDirty`, `flushQueuedSaves`, `migrateTo`; `DataItem` has `dirty()/markDirty()/markClean()` | `Store<K,V>` should *extract* the file I/O from `FileDataRepository`, not rebuild it. `FileDataRepository` becomes a deprecated facade over `FileStore`. |
| 1.5: "wire the existing money reward type" | No concrete `RewardAction` exists anywhere. `reward` is a pure pipeline (`RewardAction` functional interface, `RewardEngine`, `RewardPlan`). The only "money" is a string in an error hint. | 1.5 creates `RewardActions.money(...)` from scratch, plus a `RewardSubjects` helper because `RewardContext.subject` is an untyped `Object`. |
| 1.3: "keep the existing single-file API working" | `MessageCatalog` has no file loading at all. It is builder-only, built in code. No `Locale` reference anywhere in `message` or `text`. | The "single-file API" to preserve is the builder. File loading and locales are both new. |
| 2.3: audit for callbacks on the wrong region | `Promise`/`ThreadContext` know only `SYNC` and `ASYNC`. Region/entity awareness lives only in `TaskContext`/`Schedulers`. On Folia, `thenApplySync` means "global region thread", never the entity's region. | The audit's main outcome is an additive `Promise.thenApply(TaskContext, fn)` family. This is a design change, not just bug fixes. |
| 2.1: add a `FakeScheduler` | `SchedulerBackend` is package-private and bound in a `static final` field with no setter. `Promise.supplyingSync` → `Bukkit.isPrimaryThread()` NPEs off-server. `RamExceptions` logs via `LoaderUtils.getPlugin()` → `JavaPlugin.getProvidingPlugin`, also unusable off-server. | Every Promise-returning API added in Phase 1 is untestable until the seam exists. **2.1 must precede 1.1.** |
| 1.5 / 3.6: region name placeholder, region enter/exit events | `region` is a pure rule engine. No listener, no enter/exit detection, no `regionsAt(Position)` lookup, no `region(ContentId)` getter. | Add `RegionRuleEngine.regionsAt(...)`/`region(id)` and a small `RegionTracker` listener (player movement → enter/exit) in 1.5. 3.6 consumes it. |
| 3.2: instanced worlds | Zero world create/load/unload code in the repo. Folia has historically not supported runtime `Bukkit.createWorld`. | 3.2 is greenfield and must be capability-gated; it may end up Paper-only. |
| 3.1: resource pack builder | `resourcepack` holds ids and prompt tracking only. Nothing hashes, zips, writes `pack.mcmeta`, or serves HTTP. | 3.1 is greenfield. `ItemComponentProfile.itemModel(Key)` and `customModelData(...)` already exist for the item side. |
| 2.2: "the build does not enforce boundaries" | Confirmed. Also: `menu.Gui` imports `reflect.MinecraftVersion` (for a 1.16 tick-delay check), `scoreboard` imports `protocol`+`reflect`, `event.ProtocolSubscription` sits in the core `event` package but is ProtocolLib-typed, and `nms.api` capability records are imported by 11 packages. | `nms.api` must land in `ramcore-api`; only `nms.reflect` goes to `ramcore-nms`. `Gui`'s reflect import is dead weight on Paper 26.1 and should be removed. |
| — | `paper-plugin.yml` hardcodes `version: '1.0.0-SNAPSHOT'` while `pom.xml` says `2.0.0`, even though resource filtering is on. | Fix in Phase 0 with `${project.version}`. |
| — | `RamPlugin.onDisable()` calls the static `RamExecutors.shutdown()`. RamCore is a runtime plugin shared by all consumer plugins, so the first consumer to disable kills the shared async executor for everyone. | Regression test + fix in 2.3. |
| — | No CI. No `examples/` dir. No `src/test/kotlin`. `RamCoreKtx.kt` is untested. Two Kotlin extensions (`CommandContext.get/resolve`) are shadowed by the Java members and never dispatch. | CI in Phase 0. Kotlin tests arrive with 2.4. Remove the two dead extensions in 2.4. |
| — | `ConfigValidationException` and `TemplateValidationException` have identical shape (`List<String> errors()`) but no shared base. | 1.6 introduces `ValidationException` as the base and needs file+path on each error; retrofit both. |

Test-suite shape today: 42 JUnit 4 files, 196 tests, no MockBukkit/Mockito, Bukkit fakes are hand-rolled `java.lang.reflect.Proxy` handlers nested inside individual tests. `TestContext implements ServiceContext` is duplicated in two files; `MutableClock` exists once. These become the seed of `ramcore-test`.

---

## 2. Recommended order

The roadmap's suggested order is `1.1 → 1.2 → 2.1 → 2.3 → 1.3–1.6 → 2.2/2.4/2.5/2.6 → Phase 3`. This plan deviates in two places, both for hard dependency reasons:

1. **2.1 (test modernization + FakeScheduler) and 2.3 (scheduler/Promise audit) move before 1.1.** `Store<K,V>` returns `Promise`, and `Promise` cannot run off-server today. Writing 1.1's tests first in JUnit 4 and then migrating them is wasted work. 2.3 must land before Phase 1 adds the two heaviest new scheduler users (stores, player data).
2. **2.6 (version policy) and the Gradle question are decided up front, in Phase 0.** They are cheap decisions that shape 2.2, and 2.2 is the natural moment to switch build tools if that is the choice. Switching after the split would mean doing the split twice.

The rest of the roadmap order is kept, with 2.1 split into 2.1a (now, inside `src/test`) and 2.1b (publish as `ramcore-test` during 2.2, because a published module needs the multi-module build).

| Phase | Tasks in order | Gate to next phase |
| --- | --- | --- |
| 0 — Prep | 0.1 build hygiene + CI · 0.2 decisions (2.6, build tool, optional-dependency strategy) | CI green on `master`; decisions recorded as ADRs under `docs/decisions/` |
| A — Test foundation | 2.1a JUnit 5 + FakeScheduler + testkit · 2.3 audit | 196+ tests green on JUnit 5; every audit finding has a regression test |
| B — Roadmap Phase 1 | 1.1 · 1.2 · 1.3 · 1.4 · 1.5 · 1.6 | Each task: tests, `docs/API.md` section, `MODULE_BOUNDARIES.md` row |
| C — Engineering | 2.2 split (+2.1b `ramcore-test`) · 2.4 coroutines · 2.5 operational | Consumer can depend on `ramcore-api` from JitPack; plugin jar unchanged for servers |
| D — Gameplay | 3.6 · 3.4 · 3.3 · 3.1 · 3.5 · 3.7 · 3.8 · 3.2 · examples | `examples/` plugin demonstrates all five DoD scenarios on Paper and Folia |

Phase D reorders the roadmap's list: 3.6 (session recorder) first because it is small and instruments every later task; 3.4 before 3.3 as mandated; 3.2 (instanced worlds) last because it has the highest manual-test burden and the example dungeon needs everything else anyway.

Sizes below are in agent sessions: **S** = 1, **M** = 2–3, **L** = 4–6, **XL** = 7+. One PR per task unless a split is listed.

---

## 3. Per-task rules (apply to every task)

Taken from the roadmap's working rules and made checkable:

- [ ] New interface lives in a package that will end up in `ramcore-api`; Paper/SQL/Redis-backed implementations live in a sibling `*.paper`/`*.sql`/`*.redis` subpackage or class named `Paper*`/`Sql*`. From 2.1a on, an import-scan test (`ApiBoundaryTest`) enforces "no `com.comphenix`, `net.minecraft`, `dev.willram.ramcore.{protocol,packet,nms.reflect,reflect,shadows,nbt}` imports" in api-destined packages.
- [ ] Every lifecycle-owning object implements `Terminable` (or `AutoCloseable`) and is bindable via `RamPlugin.bind()`; services implement `Service`.
- [ ] Preconditions via `RamPreconditions.checkArgument/checkState/misuse` with a problem + fix sentence. Aggregated validation throws a `ValidationException` subtype exposing `errors()`.
- [ ] Anything touching world/entity/player state goes through `Schedulers`/`TaskContext`. The `docs/API.md` section states "Folia-safe by design" or "requires explicit scheduler context" per type.
- [ ] Unit tests under `src/test/java` using the in-memory backend and `FakeScheduler`. No live server.
- [ ] `docs/API.md` section added with stability level; `docs/MODULE_BOUNDARIES.md` gets a row; `README.md` if consumer-visible setup changes.
- [ ] Additive only. Anything replaced gets `@Deprecated(since = "2.1")` with `@see` pointing at the replacement.
- [ ] Kotlin DSL only after the Java API is settled, in `RamCoreKtx.kt` (or the `ramcore-kotlin` module after 2.2).
- [ ] Task summary at the end: what changed, what was tested, open questions.

Branching: one branch per phase named `phase/<letter>-<slug>` (for example `phase/a-test-foundation`), one commit per task or PR-sized step on that branch, merged into `master` when the phase gate passes. CI must be green at merge.

---

## 4. Phase 0 — Prep

### 0.1 Build hygiene and CI — **S**

- `paper-plugin.yml`: `version: '${project.version}'`. Add `description` and `authors`.
- Commit the pending `2.0.0` version bump.
- Pin `maven-surefire-plugin` 3.5.x explicitly (needed for JUnit 5 in 2.1a).
- `.github/workflows/ci.yml`: JDK 25 (Temurin), `mvn -B verify`, cache `~/.m2`. Runs on push and PR.
- `README.md`: state the JDK 25 requirement for building and the `JAVA_HOME` note.

### 0.2 Decisions — **S**

Record each as `docs/decisions/ADR-000N-*.md`.

**ADR-0001 — Version support policy (roadmap 2.6).** Recommendation: **(a) latest-only.** Reasons: the code already emits Java 25 bytecode and uses 26.1 data-component APIs; a Java 21 / 1.21.x backport means a second test matrix and a second CI target, and the capability system (`NmsAccessRegistry`) would report most advanced features `UNSUPPORTED` there anyway, so the backport would be a materially smaller library. Policy text: "RamCore tracks the current Paper major line. When Paper moves to a new line, RamCore cuts a `release/<old>` branch that receives fixes only, for one Minecraft release cycle, only on request." If the user picks (b) instead, the isolation work is: `item.component` (data-component types moved between 1.21 and 26.1), `Player.locale()`, `MenuType`/`AnvilView`, `ClickCallback`, and the regionised scheduler API surface — each gets a `VersionAdapter` behind `NmsAccessStrategy`.

**ADR-0002 — Build tool for the multi-module split (roadmap 2.2 question).** Trade-off:

| | Maven (stay) | Gradle Kotlin DSL (switch) |
| --- | --- | --- |
| Multi-module + scoping enforcement | Works; api module simply lacks deps on the others | Same, plus `java-library` `api`/`implementation` gives finer consumer-facing scoping |
| Mixed Java + Kotlin | Works, but needs the current awkward "disable default-compile, run kotlin first" dance | First-class |
| Shading/relocation per module | `maven-shade-plugin`, already configured | `shadow` plugin, equivalent |
| Paper ecosystem tooling | `paperweight-userdev` (Mojang-mapped NMS), `run-paper`/`run-folia` (one-command smoke server), `resource-factory` (generated `paper-plugin.yml`) are **Gradle-only** | All available |
| Publishing | `distributionManagement` or JitPack | `maven-publish` or JitPack; JitPack supports both |
| Migration cost | Zero | Roughly one session now (pom is ~300 lines); grows after the split |
| Risk | None | IDE reindex, contributor familiarity |

Recommendation: **switch to Gradle Kotlin DSL as the first commit of 2.2**, before creating modules. The deciding factor is `run-paper`/`run-folia`: every Phase 3 task needs manual server smoke tests on both, and the smoke logs in `RELEASE_READINESS.md` were produced by hand. If NMS versioned adapters (`nms.v1_21_x`, mentioned in `todo.md`) ever materialise, `paperweight-userdev` is the only sane way to write them. If the user prefers zero churn, Maven is fully viable and the rest of this plan does not change.

**ADR-0003 — Optional heavy dependencies (Hikari, JDBC drivers, Redis client, coroutines).** Options: (i) shade with `<optional>true</optional>` — the runtime plugin jar carries them for everyone; sqlite-jdbc alone is ~13 MB with natives; (ii) Paper `PluginLoader` + `MavenLibraryResolver` in a `RamCoreLoader` class — resolves from Maven Central at startup only when `plugins/RamCore/config.yml` enables SQL/Redis, nothing shaded. Recommendation: **(ii)** for Hikari, JDBC drivers, and Lettuce; shade only small pure-Java libs (bStats, kotlinx-coroutines in the Kotlin module). Classes referencing those libraries (`SqlStore`, `RedisMessageBus`) are loaded lazily through a factory that checks class presence and throws an actionable `ApiMisuseException` ("enable `storage.sql` in RamCore's config.yml") otherwise. Tests use the libraries at `test` scope in-process.

---

## 5. Phase A — Test foundation

### 2.1a Test modernization, FakeScheduler, testkit — **M** (2 PRs)

**PR 1 — JUnit 5.** Replace `kotlin-test-junit` with `kotlin-test-junit5`; add `junit-jupiter` 5.11.x. Mechanical migration of 42 files: `org.junit.Test` → `org.junit.jupiter.api.Test`; `org.junit.Assert.*` → `org.junit.jupiter.api.Assertions.*` (watch the message-argument position moving last); `@Test(expected = X.class)` → `assertThrows`; `@Rule TemporaryFolder` (2 files) → `@TempDir`. Do not add `junit-vintage`; migrate everything. Acceptance: 196 tests green under surefire 3.5.

**PR 2 — Scheduler seam + FakeScheduler + testkit.**

- Make `SchedulerBackend` public and `@ApiStatus.Internal`. Replace the `static final BACKEND` with a `volatile` field plus `SchedulerBackends.install(SchedulerBackend)` / `reset()` (internal, documented as test-only). `ThreadContext.forCurrentThread()` and `Schedulers.isSyncThread()` must ask the backend rather than `Bukkit` directly.
- `RamExceptions` logging: route through a `RamLog` sink that falls back to `java.util.logging` when `LoaderUtils` has no plugin bound. Off-server code paths must never touch `Bukkit.*` statics; enumerate them during this PR (known: `LoaderUtils.getPlugin/getMainThread`, `ThreadContext.forThread`, `PaperFoliaSchedulerBackend.isSyncThread`, `NamespacedKeys.create`).
- `FakeScheduler implements SchedulerBackend`: deterministic, tick-stepped. API: `tick()`, `tick(n)`, `runAsync()` (drain the async queue inline), `pendingCount()`, `retireEntity(UUID)` (fires `retired` callbacks and drops that entity's queue), `currentContext()` so `isSyncThread()` answers per-queue. Models global, async, per-region (keyed by `TaskContext.description()`), and per-entity queues. Repeating tasks reschedule on each tick until cancelled.
- Testkit package `src/test/java/dev/willram/ramcore/testkit/`: `FakeScheduler`, `TestServiceContext` (dedupes the two copies), `FakeClock` (from `MutableClock`), `ProxyFakes` (the `Proxy`/`InvocationHandler` helpers now repeated in 15 files), `FakeItemStack`. Existing tests switch to these.
- `ApiBoundaryTest`: scans `src/main/java` imports and fails on forbidden imports in api-destined packages (list in §3). Seeds the 2.2 enforcement.
- First Promise tests: `PromiseTest` covering `supplyingAsync → thenApplySync` ordering under `FakeScheduler`, exception propagation, cancel-before-supply. There are zero Promise tests today.

Docs: `API.md` Scheduling section gains "Testing with FakeScheduler". Stability: `FakeScheduler` experimental until 2.1b publishes it.

**Outcome (done 2026-09-10).** Both PRs landed as two commits on `phase/a-test-foundation`. 211 tests on JUnit 5 after PR 2 (196 migrated + 15 new). `Gui`'s dead reflect import was removed here instead of in 2.2. `ApiBoundaryTest` carries an exemption list for the known 2.2 moves (`event.ProtocolSubscription`, `ProtocolLibIntegrationProvider`, `nms.api` → `reflect` value types). `ProxyFakes` is adopted by the new tests only; the 15 existing hand-rolled proxies switch over as their files are next touched.

### 2.3 Scheduler and Promise audit — **L** (1 PR per finding group)

Audit `Promise.java` (1,449 lines), `RamPromise.java` (758), `Schedulers.java` (1,104), `PaperFoliaSchedulerBackend.java`, `threadlock/`. Seeded findings from the survey; each becomes a regression test on `FakeScheduler` before its fix:

| # | Finding | Fix |
| --- | --- | --- |
| A1 | `ThreadContext` is SYNC/ASYNC only. On Folia, "sync" = global region thread, so `thenApplySync(e -> entity.setHealth(...))` runs off the entity's region. | Additive: `Promise.thenApply(TaskContext, fn)`, `thenAccept(TaskContext, ..)`, `thenRun(TaskContext, ..)`, `thenCompose(TaskContext, ..)`, `exceptionally(TaskContext, ..)`, plus `Promise.supplying(TaskContext, supplier)`. `ThreadContext.SYNC` documented as "global". `Schedulers.call(entity, ..)` already returns a Promise; its continuations now have a correct anchor to name. |
| A2 | Entity-scoped task where the entity is retired and no `retired` callback was given: Paper drops the task silently, so the returned `Promise` never completes. | Backend passes a default `retired` that completes the promise exceptionally with `EntityRetiredException` (new, in `exception.types`). |
| A3 | `RamPlugin.onDisable()` → `RamExecutors.shutdown()` kills the shared static executor for every RamCore-based plugin. | Executor lifetime belongs to the RamCore plugin only. `RamPlugin` subclasses that are not `RamCore` skip it. Test asserts a second plugin's disable leaves `asyncHelper()` usable. |
| A4 | `ServerThreadLock` parks the global tick thread from another thread; on Folia this is a deadlock risk and semantically wrong. Its javadoc references a stale `devcore` package. | Deprecate; throw `ApiMisuseException` when the backend reports regionised mode. Document the replacement (`Schedulers.call(context, ..).join()` from async only). |
| A5 | `Promise.thenComposeDelayedSync(ThreadContext, ..)` is misnamed (dispatches on the context). | Add `thenComposeDelayed(ThreadContext, ..)`; deprecate the old name. |
| A6 | Exception swallowing: `RamExceptions.reportPromise` logs and continues; `exceptionally*` chains that themselves throw. Verify no path drops a `Throwable` without logging; verify `RamExceptionEvent` re-entrancy latch. | Tests for: exception in supplier, in `thenApply`, in `exceptionally`; each must surface exactly once. |
| A7 | Cancellation races: `cancel()` after supplier started; `RamTask.stop()` from inside its own consumer; `runRepeating` where the consumer throws (does the timer die silently?). | Tests + fixes. Decide and document: a repeating task whose body throws keeps running (helper semantics) and reports each failure. |
| A8 | `TaskBuilder` chain offers only `sync()`/`async()`. | Add `TaskBuilder.on(TaskContext)`. |
| A9 | `RamAsyncExecutor` / `Schedulers.shutdown(plugin)` cancel tasks by plugin: verify entity/region tasks are cancelled on disable (Paper's schedulers are per-plugin; confirm every backend call passes the owning plugin). | Test via a recording fake backend. |

Also in scope: reduce `Promise.java` overload sprawl only if it falls out naturally from A1 (do not refactor for its own sake). Docs: `API.md` Promises section rewritten around `TaskContext`-anchored continuations; Folia note per method family.

**Outcome (done 2026-09-10, branch `phase/a-test-foundation`).** A1, A2, A5, A8 implemented with tests. A3 fixed via `RamPlugin.ownsSharedExecutors()` (not unit-testable off-server; verify on the next Paper smoke run by disabling a consumer plugin and checking `RamExecutors.asyncHelper()` still runs). A4 deprecated with a Folia guard. A6: causes are now logged (`RamLog.severe` with throwable); each step reports exactly once; covered by `PromiseTest`. A7 covered by `FakeSchedulerTest` (self-stopping timer, throwing repeating body). A9 documented: Paper cancels global/async tasks per plugin and drops entity/region tasks of disabled plugins; nothing to change. Three bugs found beyond the seeded list, all fixed and tested: cancel-after-completion poisoned later continuations, cancelling a derived promise did not skip its function, and `exceptionally` never ran for an upstream cancellation so derived promises hung.

---

## 6. Phase B — Roadmap Phase 1

### 1.1 Pluggable persistence layer — **XL** (4 PRs)

New package `dev.willram.ramcore.store` (api-destined).

**Contracts.**

```java
public interface Store<K, V> extends Terminable {
    Promise<Optional<V>> load(K key);
    Promise<Void> save(K key, V value);
    Promise<Boolean> delete(K key);
    Promise<Map<K, V>> loadAll();
    Promise<Set<K>> keys();
}

public interface DirtyTracking<K> {
    void markDirty(K key);
    Set<K> dirtyKeys();
    Promise<Integer> saveDirty();
}

public interface CachedStore<K, V> extends Store<K, V>, DirtyTracking<K> {
    Optional<V> cached(K key);          // synchronous read after load
    V require(K key);
    void put(K key, V value);           // marks dirty
    void evict(K key);
}
```

Design choice: raw backends (`InMemoryStore`, `FileStore`, `SqlStore`) are stateless key/value transports. Dirty tracking and the in-memory working set live in one wrapper, `CachedStore` (`Stores.cached(backend)`), so every backend gets dirty tracking for free and the SQL backend stays trivial. Entries are wrapped as `StoredRecord<V>(int dataVersion, V value)` so migrations work for any `V`, not only `DataItem`. `StoreCodec<V>` (`String encode(V)`, `V decode(String)`) built on `GsonProvider`; `DataKeyCodec<K>` reused for keys.

**PR 1 — contracts + `InMemoryStore` + `CachedStore` + migrations.** `StoreMigrations<V>` chain reusing `DataMigration<V>`/`DataRepositoryMigration<V>`, applied on load, bumping `dataVersion`, marking dirty. Tests: contract suite as an abstract `StoreContractTest` every backend extends; dirty tracking; migration application; async save ordering on `FakeScheduler` (two saves to one key serialise in call order, distinct keys may interleave).

**PR 2 — `FileStore`.** Extract the atomic-write/tmp-move/directory-scan code from `FileDataRepository` into `FileStore`; `FileDataRepository` becomes a thin deprecated adapter over `Stores.cached(FileStore)`. `Repositories.jsonByUuid/jsonByString` keep working. Tests: existing 6 `FileDataRepositoryTest` cases still pass unchanged (that is the compatibility proof) + contract suite with `@TempDir`.

**PR 3 — `SqlStore`.** Table per store: `(store_key VARCHAR(191) PRIMARY KEY, data_version INT, data TEXT, updated_at BIGINT)`. `SqlDialect { SQLITE, MYSQL, MARIADB, POSTGRES }` differing only in upsert syntax and `CREATE TABLE IF NOT EXISTS` types. `SqlStoreConfig` record (jdbcUrl, username, password, poolSize, dialect) loadable from `BukkitConfig` keys. Hikari per ADR-0003 (runtime-resolved, `provided` at compile, `test` scope in-process `sqlite-jdbc`). All JDBC work on `Schedulers.async()`; Promise anchors chosen by the caller. Tests: contract suite on in-process SQLite file + `:memory:`; dialect SQL string tests for the other three (no live DB).

**PR 4 — domain stores + retrofits.**
- `PartyStore`, `CooldownStore`, `ObjectiveProgressStore` interfaces over `Store<K, Snapshot>` with explicit serialisable snapshots: `PartySnapshot(id, leader, roles, createdAt)`, `CooldownSnapshot(key, lastTestedMillis, timeoutMillis)`, `ObjectiveProgressSnapshot(subject, objectiveId, amounts)`. `MetadataMap` and contribution trackers are *not* persisted (documented).
- `PartyManager.create(options, clock, store)`, `CooldownTracker.create(base, store)`, `ObjectiveTracker.create(store)`: default `InMemory*Store`, behaviour unchanged. Each gains `Promise<Void> load()` and write-through on mutation; `close()` flushes.
- `LootInstanceStore` retrofit: keep the interface untouched; add `PersistentLootInstanceStore` = `InMemoryLootInstanceStore` + write-through to `Store<UUID, LootInstanceSnapshot>`. `LootReward.payload` is `Object`; snapshot persists `id/amount/metadata` and a codec-provided payload string, documented as the consumer's responsibility.
- `@Deprecated` on `DataRepository`, `FileDataRepository`, `Repositories` pointing at `Stores`.

Facade `Stores`: `inMemory()`, `file(dir, keyCodec, codec)`, `sql(config, table, keyCodec, codec)`, `cached(store)`, `migrations()`. Kotlin: `store<K, V> { ... }` after PR 4. Stability: `Store`/`CachedStore`/`InMemoryStore`/`FileStore` **stable**; `SqlStore` **experimental** (dialects beyond SQLite are untested live).

**Outcome (done 2026-09-10, four commits on `phase/b-roadmap-1x`).** Landed as designed with two deviations: migrations use a store-level `StoreMigration<V>` (the existing `DataMigration` is bounded to `DataItem`), and `StoreMigrations.start().to(..)` replaced a static `to(..)` that clashed with the instance method under erasure. Cached stores flush synchronously on `close()` with a bounded wait; tests drive async backends with `FakeScheduler.runAll()` before closing. Loot payloads persist through a consumer-supplied `LootPayloadCodec`. Open item: `RamCoreLoader` (Paper `PluginLoader`) that resolves HikariCP and JDBC drivers at runtime is not written yet; it belongs to 2.5 alongside RamCore's `config.yml`. Until then SQL stores work only where a consumer plugin ships the driver.

### 1.2 Player data lifecycle — **L** (2 PRs)

`dev.willram.ramcore.playerdata`.

- `PlayerDataKey<T>(String id, Class<T> type, Supplier<T> defaultFactory)`; `PlayerDataService implements Service, Terminable`; `PlayerDataOptions(joinPolicy, loadTimeout, autosaveInterval, kickMessage)` with `JoinPolicy { KICK, DEFER }`. `BLOCK` is deliberately not offered: blocking the join thread is the region thread on Folia. `DEFER` returns `Optional.empty()` until loaded and exposes `whenReady(player) -> Promise<Void>`.
- Registration: `service.register(key, Store<UUID, T>)` (any store from 1.1; typically `Stores.cached(...)` per key). `PlayerDataService.install(RamPlugin, options)` registers the service, the listener, and binds it. Consumers reach it via `services().get(PlayerDataService.KEY)`.
- Lifecycle: `AsyncPlayerPreLoginEvent` (async thread) → `preload(uuid)` fans out `load` across keys, stores the composite `Promise` in `pending`. `PlayerLoginEvent` not `ALLOWED` or `AsyncPlayerPreLoginEvent` disallowed → evict pending. `PlayerJoinEvent` → if pending complete, promote to `loaded`; else apply `joinPolicy` (KICK with the configured message after `loadTimeout`, or DEFER). `PlayerQuitEvent` → `saveDirty` for that player then evict. Autosave: `Schedulers.runTimer(TaskContext.async(), ..)` bound to the service. Shutdown mid-save: `disable()` runs `saveAll().join()` with a bounded wait and logs any key that did not flush.
- Thread contract: `T` is read synchronously on the player's region after join. Writes call `markDirty(player, key)`. Encoding for save happens on the player's scheduler (`Schedulers.call(player, () -> codec.encode(value))`), I/O on async, so `T` need not be thread-safe. This is the documented Folia rule for the subsystem.
- Pending sweep: entries older than `loadTimeout * 4` with no join are evicted (login cancelled by another plugin after preload).

Tests with `FakeScheduler` + `InMemoryStore` + `FakeClock`: preload/join happy path, join before load with KICK and DEFER, cancelled login, quit saves and evicts, autosave only touches dirty keys, shutdown flush. Events are constructed directly (`AsyncPlayerPreLoginEvent` has a public constructor; `PlayerJoinEvent` needs a `Player` proxy from testkit). Stability: **experimental** in 2.1, stable once an example consumer has used it. Kotlin: `playerDataKey<T>("id") { default }`, `Player.data(key)`.

**Outcome (done 2026-09-10, one commit on `phase/b-roadmap-1x`).** Landed as designed with these deviations: `PlayerDataKey` gained a fourth component, `snapshot` (`UnaryOperator<T>`, identity by default), because `register(key, Store<UUID, T>)` never sees a codec; the copy runs on the player's scheduler and the async backend encodes the copy, which honours the "T need not be thread-safe" rule. `PlayerDataOptions` has a fifth field, `flushTimeout`, bounding the shutdown flush. `install` must be called from `RamPlugin.load()` because the service registry refuses registrations after load; the listener is registered in `enable`. Keys registered after players are online are loaded for them. A failed load kicks under `KICK` and fails `whenReady` under `DEFER`. Kotlin `Player.data(service, key)` takes the service explicitly rather than looking one up. Tests (28) drive the lifecycle through the service methods and, separately, through directly constructed Bukkit events; `AsyncPlayerPreLoginEvent` needs the six-argument constructor with a proxied `PlayerProfile` to avoid `Bukkit.createProfile`.

### 1.3 Locale support in `MessageCatalog` — **M**

- `MessageCatalog.Builder.locale(Locale, Map<MessageKey, String>)`, `.defaultLocale(Locale)` (default `Locale.US`), `.localeResolver(LocaleResolver)`. `LocaleResolver { Locale resolve(Audience) }`; default resolves `Player.locale()` when the audience is a `Player`, else default locale.
- New render overloads `render(Locale, key, ..)`, `renderRaw(Locale, key, ..)`. Existing `render(key, ..)` = default locale. `send(audience, key, ..)` now resolves the audience locale; with no locales registered this is byte-for-byte the old behaviour.
- Fallback chain: exact locale → language-only (`en_GB` → `en`) → default locale → `MessageKey.defaultTemplate()` → key id. Templates missing in a locale never throw.
- `MessageCatalogLoader`: `yaml(Path dir, String baseName)` loads `messages.yml` as the default locale and every `messages_<tag>.yml` (`en_US`, `de_DE`; parsed with `Locale.forLanguageTag(tag.replace('_','-'))`). Nested YAML keys flatten with dots. `copyDefaults(plugin, dir, baseName)` copies bundled resources on first run. Uses Bukkit `YamlConfiguration` like `BukkitConfig` for consistency; Configurate is not needed here.
- Kotlin: `messageCatalog { locale(Locale.GERMANY) { WELCOME to "..." } }`.

Tests: fallback chain, resolver override, loader with `@TempDir` files, existing 3 tests unchanged. Stability: **stable** (additive to a stable package). Folia-safe.

**Outcome (done 2026-09-10, one commit on `phase/b-roadmap-1x`).** Landed as designed. `LocaleResolver` is its own file with `byPlayerLocale(default)` and `fixed(locale)` factories. `MessageCatalog.Builder.message`/`messages` feed the default locale and win over a `locale(defaultLocale, ..)` set; `locale(locale, map)` merges. The loader returns a `MessageCatalogLoader.Bundle` record applied through `Builder.load(bundle)`; it keys templates by `MessageKey.of(id, template)`, which matches consumer constants because `MessageKey.equals` is id-only. `copyDefaults` takes explicit resource file names (varargs) rather than only the base name, since bundled locale files cannot be enumerated from the jar. `MessageLoadException` (a nested `RuntimeException`) reports unreadable files and unparseable locale tags. All three original catalog tests pass unchanged; 15 new tests cover the fallback chain, resolver override, the loader with `@TempDir`, and `copyDefaults` with a proxied `Plugin`.

### 1.4 Player text input — **L** (2 PRs)

`dev.willram.ramcore.input`.

- `InputRequest.Builder`: `prompt(Component)`, `timeout(ticks)`, `cancelWord(String)`, `validator(Predicate<String>, Component error)`, `retries(int)`, `backend(InputBackend)`. `InputBackend { CHAT, ANVIL, SIGN }`. `PlayerInput.request(Player, InputRequest) -> Promise<String>`; `request(Player, InputRequest, InputParser<T>) -> Promise<T>`, where parse failure consumes a retry. Cancel/timeout complete exceptionally with `InputCancelledException(reason)`.
- `InputSessionRegistry`: one active request per player; a new request cancels the previous. Player quit cancels. All completion continuations hop to `Schedulers.run(player, ..)`.
- Chat backend: `AsyncChatEvent` subscription at `LOWEST`, cancels the event and captures plain text. Because the event is async, the promise completes via the player's scheduler. Documented: the message never reaches other chat listeners because we cancel first.
- Anvil backend: Paper `MenuType.ANVIL` view; read the rename text on result-slot click (`AnvilView`/`PrepareAnvilEvent`). Verify exact 26.1 API at implementation time.
- Sign backend: needs a fake sign block (`sendBlockChange` + `openSign`) and `SignChangeEvent`; restore the block after. If `openSign` on a non-placed sign proves impossible without packets, route through `dev.willram.ramcore.packet` when ProtocolLib is available and fall back to CHAT otherwise. Stability of SIGN: **Paper-experimental**.
- `MenuSession.suspend(InputRequest) -> Promise<String>`: sets a `suspended` flag so `invalidate` skips the close handler, closes the inventory, runs the request, then `reopen()` builds a fresh inventory for the same view and preserved `MenuState`. `MenuSession` already tolerates re-open after invalidate; this PR adds the flag and a test.

Tests: registry semantics, timeout/cancel word/retries under `FakeScheduler`, chat backend by dispatching a constructed `AsyncChatEvent` with a testkit `Player` proxy, `MenuSession` suspend/reopen state preservation. Kotlin: `player.askText { timeout = 200 }`. Stability: CHAT/ANVIL **experimental**, SIGN **Paper-experimental**.

**Outcome (done 2026-09-10, one commit on `phase/b-roadmap-1x`).** CHAT backend and the whole request/registry/session core landed as designed and are unit-tested; ANVIL is implemented against `player.openAnvil` + result-slot read but is not exercised off-server; SIGN is not wired natively yet (no virtual-sign packet path) and logs a fallback to CHAT, so it stays Paper-experimental. Key shape: `InputCancelledException` carries a `Reason` enum (CANCELLED, TIMEOUT, QUIT, EXHAUSTED, SUPERSEDED, OFFLINE) rather than a free-form string. `InputSessionRegistry` holds no Bukkit registration; `InputListener` forwards events to it and tests call its `onChat`/`onQuit` directly with a constructed `AsyncChatEvent` (seven-arg constructor: async flag, player, viewers set, null `ChatRenderer`, message, original, null `SignedMessage`). Completions hop through `Schedulers.run(player, ..)`, which the `FakeScheduler` routes through the delayed-entity lane, so tests must `tick()` (not just `runAll()`) to observe them. `MenuSession` gained a `suspended` flag (invalidate skips the close handler while suspended) and `suspend(InputRequest)` returning the input promise after reopening. Kotlin `Player.askText { .. }` takes the `InputRequest.Builder` as receiver (method calls, not `timeout = 200` assignment). 17 tests cover capture, cancel word, timeout, validation/parse retries, supersede, and quit.

### 1.5 Vault and PlaceholderAPI bridges — **L** (2 PRs)

**PR 1 — economy.** `dev.willram.ramcore.economy`: `Economy { double balance(UUID); boolean has(UUID, double); EconomyResult withdraw(UUID, double); EconomyResult deposit(UUID, double); String format(double); String currencyName(boolean plural); }`, `EconomyResult(success, newBalance, message)`. `InMemoryEconomy` (concurrent map, no negative balances). `VaultEconomy` in its own class, obtained only through `Economies.detect(IntegrationRegistry) -> Optional<Economy>` so `net.milkbowl` classes never load when Vault is absent. VaultAPI 1.7.1 from JitPack, `provided`. Thread note: Vault providers are main-thread; RamCore does not hop, callers pick the context.

Reward wiring: `RewardActions.money(Economy, double)` (and, since the package has no concrete actions at all, also `command`, `message`, `item`, `permission-node-check`) plus `RewardSubjects.playerId(RewardContext)` resolving `UUID`/`OfflinePlayer`/`Player` subjects. `Rewards` facade added.

**PR 2 — placeholders.** `dev.willram.ramcore.placeholder`: `PlaceholderProvider { String id(); @Nullable String resolve(OfflinePlayer, String params); }`, `PlaceholderRegistry` (RamCore-internal resolution + MiniMessage `TagResolver` bridge into `TextContext`), and `PlaceholderApiBridge` that, when PAPI is available, registers one `PlaceholderExpansion` per provider id. Built-ins are opt-in because RamCore holds no gameplay instances: `RamCorePlaceholders.builder().parties(manager).cooldowns("combat", tracker).objectives(tracker).regions(engine).build()` → provider id `ramcore`, params `party_size`, `party_leader`, `cooldown_<name>_<key>`, `objective_<id>_<task>`, `region`. PAPI 2.11.x `provided` from the ExtendedClip repo.

Region support needed by the `region` placeholder and later by 3.6: add `RegionRuleEngine.regionsAt(Position)`, `region(ContentId)`, and `RegionTracker` (a `Listener`/`Terminable` on `PlayerMoveEvent` region-changed check + teleport/world change that fires `RegionEnterEvent`/`RegionExitEvent` Bukkit events and keeps a per-player current-region set). Folia: the move event runs on the player's region; tracker state is a concurrent map.

Tests: `InMemoryEconomy`, money reward through `RewardEngine`, placeholder resolution without PAPI, param parsing, `RegionTracker` transitions with proxied players. Both bridges tested for clean absence via `IntegrationRegistryTest`'s `FakeDetector`. Stability: **stable** for the abstractions, **experimental** for the bridges.

### 1.6 Config-backed content definitions — **XL** (3 PRs)

- Add `configurate-yaml` alongside the existing `configurate-hocon` (same shade/relocation), so YAML and HOCON both parse into `ConfigurationNode`.
- Layout: `content/<type>/*.yml|*.conf`; each file holds one or many entries. Entry: `id: ns:value`, optional `extends: ns:parent`, then type fields. Type is the directory name (`items`, `loot`, `rewards`, `regions`, `templates`, `npcs`, `displays`).
- `ContentLoader.load(Path) -> ContentLoadResult { definitions(), errors() }`. Each `ContentDefinition(id, type, parent, node, SourceRef(file, path))`. Inheritance: definitions go into a `TemplateRegistry<ConfigurationNode>` whose composer deep-merges nodes (child scalar wins, child list replaces, maps merge). Missing parents and cycles come back from the existing template validation with their `SourceRef` attached.
- Deserialisation to *specs*, not live objects: `ItemSpec`, `LootTableSpec`, `RewardPlanSpec`, `RegionSpec`, `NpcSpec` config form, `DisplaySpec` config form. Specs are pure records testable off-server; `ContentRegistrar` turns them into `ItemStack`/`RuleRegion`/etc. on the server. Reward entries use a `RewardActionFactory` registry keyed by `type:` (`money`, `command`, `message`, `item`, ..., extended by consumers), which is why 1.5 comes first.
- Validation: every error at once. New `ValidationException` base in `exception` with `List<ValidationError(source, path, message)>`; `ConfigValidationException` and `TemplateValidationException` retrofitted to extend it (source-compatible: they keep their constructors and `errors()`). `ContentValidationException` is the loader's.
- `/ramcore diagnostics validate <plugin> [subdir]` runs the loader with `executesAsync` against `plugins/<plugin>/<subdir|content>` and prints safe lines; zero registration side-effects.

Tests: loader on `@TempDir` fixture trees (valid, missing parent, cycle, bad material key, bad reward type), every-error-at-once assertion with file+path, spec round-trips. Kotlin: `contentLoader(dir) { deserializer<ItemSpec>("items") }`. Stability: **experimental**.

---

## 7. Phase C — Engineering quality

### 2.2 Multi-module split (+ 2.1b `ramcore-test`) — **XL** (3 PRs)

**PR 1 — build tool** per ADR-0002 (if Gradle: `settings.gradle.kts` + root `build.gradle.kts` reproducing the current single-jar build exactly; verify the shaded jar starts on Paper and Folia via `run-paper`/`run-folia` before any module move).

**PR 2 — modules.** Mapping derived from the import survey:

| Module | Contents | Depends on |
| --- | --- | --- |
| `ramcore-api` | Everything not listed below, including `nms.api` (capability records used by 11 packages), `scheduler` (with `SchedulerBackend` interface), `promise`, `store`, `playerdata`, `input`, `economy`, `placeholder` | `paper-api` (compileOnly) |
| `ramcore-paper` | `RamCore` main, `RamPlugin` bootstrap wiring, `PaperFoliaSchedulerBackend`, `Paper*` backends, `VaultEconomy`, `PlaceholderApiBridge`, `SqlStore`, diagnostics command, `RamCoreLoader` | api, protocol, nms, kotlin (runtime) |
| `ramcore-protocol` | `protocol`, `packet`, `scoreboard` (packet-based), `event.functional.protocol`, `event.ProtocolSubscription` (same FQN, moved jar) | api, ProtocolLib (compileOnly) |
| `ramcore-nms` | `nms.reflect`, `reflect`, `shadows`, `nbt` (the last two move together: `nbt → shadows`, `shadows.nbt → nbt`) | api |
| `ramcore-kotlin` | `RamCoreKtx.kt`, 2.4 coroutines | api |
| `ramcore-test` | `FakeScheduler`, testkit fakes, `CommandTestHarness`, `MenuClickContexts.fake(..)`, `StoreContractTest` | api, junit-jupiter (api scope) |

Pre-split cleanups: remove `menu.Gui`'s `reflect.MinecraftVersion` import (dead on 26.1); confirm `integration.ProtocolLibIntegrationProvider` moves to protocol with a `ServiceLoader`-style hook so `IntegrationRegistry.standard()` in api can still find it. Enforcement: api module has no dependency on the others, so a bad import fails compilation; `ApiBoundaryTest` from 2.1a moves into `ramcore-api` as belt and braces. The runtime plugin jar stays a single shaded artifact built from `ramcore-paper`, so servers see no change.

**PR 3 — publishing + docs.** Recommend JitPack (zero infrastructure): `com.github.willramanand.RamCore:ramcore-api:<tag>`. README gets consumer snippets for Maven and Gradle. `distributionManagement`/`maven-publish` to GitHub Packages is documented as the alternative. `ramcore-test` is published with `junit-jupiter-api` as an `api`-scope dependency. `RELEASE_READINESS.md` gains a module checklist.

### 2.4 Kotlin coroutines bridge — **M**

In `ramcore-kotlin`, `kotlinx-coroutines-core` shaded and relocated into `dev.willram.ramcore.libs.kotlinx`.

- `suspend fun <T> Promise<T>.await()` via `suspendCancellableCoroutine`; coroutine cancellation calls `Promise.cancel()`.
- `RamDispatchers.global`, `.async`, `.region(Location)`, `.entity(Entity)`, `.player(Player)`: `CoroutineDispatcher` delegating to `Schedulers.forContext(ctx).execute`; `isDispatchNeeded` uses `Schedulers.isSyncThread()` for global and `Bukkit.isOwnedByCurrentRegion(...)` for region/entity so already-anchored code does not hop.
- `TerminableConsumer.coroutineScope(dispatcher)`: `CoroutineScope(SupervisorJob() + dispatcher)` bound so `close()` cancels the job. `RamPlugin.launch { }` sugar.
- Remove the two shadowed `CommandContext.get/resolve` extensions. Add `src/test/kotlin` with `runTest` + `FakeScheduler` tests: await completes, cancellation propagates, dispatcher routes to the right fake queue.

Stability: **experimental**. Docs: Kotlin Extensions section gains "Coroutines".

### 2.5 Operational basics — **L** (3 PRs)

- **Config.** RamCore has no `config.yml`; add one via `BukkitConfig` with keys `metrics.enabled` (true), `update-checker.enabled` (true), `storage.sql.*`, `messaging.redis.*`. Loaded in `RamCore.load()`.
- **bStats** `bstats-bukkit` 3.x shaded + relocated (bStats requires relocation). Custom charts: scheduler mode, module usage counts. Plugin id `33973` is the compiled-in default; `metrics.enabled` (true) is the opt-out.
- **Update checker.** `java.net.http.HttpClient` GET on the GitHub releases API, once, on `Schedulers.async()`, semver compare against `plugin.getPluginMeta().getVersion()`, one log line, never downloads. `SemVer` parser + comparison tests with canned JSON.
- **MessageBus.** `dev.willram.ramcore.messaging`: `MessageBus { Promise<Void> publish(String channel, byte[] payload); Terminable subscribe(String channel, MessageHandler); }` + `MessageCodec<T>` (Gson). `InMemoryMessageBus` (tests, single server), `PluginMessagingBus` (Bukkit `Messenger` custom channel; documented limitation that a player must be online to send), `RedisMessageBus` (Lettuce, runtime-resolved per ADR-0003, `Promise` from Lettuce futures). Handlers run on `Schedulers.async()`; callers hop. Parties/cooldowns gain an optional `MessageBus` hook in a later task, not here.

Stability: bStats/update checker **stable**; `MessageBus` **experimental**.

### 2.6 Version support decision — done in Phase 0 (ADR-0001)

Only remaining work: the README/`MODULE_BOUNDARIES.md` policy paragraph and, if (b) was chosen, the adapter isolation work listed in ADR-0001.

---

## 8. Phase D — Gameplay platform features

Phase D tasks get a short design here and a full `docs/API.md` design pass when they start.

### 3.6 Session recorder — **M** (first in Phase D: cheap, instruments everything after it)

`dev.willram.ramcore.session`: `SessionRecorder` service with `record(UUID, SessionEventType, String detail)`; disabled instance is `SessionRecorder.NOOP` so call sites are free. Per-player fixed-size array ring buffer of `SessionEvent(long tick, Instant at, type, detail)`; no map allocation on the hot path. Hooks: `RewardEngine` gets an optional `RewardListener`; `LootInstanceListener`, `ObjectiveProgressListener`, `CooldownTracker.onDenied`, `RegionTracker` events (1.5), `MenuSession` open, `AbilityCaster` (3.3). Command `/ramcore diagnostics timeline <player> [n]` prints through `DiagnosticExporter.safeLines`. Config key `diagnostics.timeline.enabled` (false). Tests: ring semantics, formatting, redaction. Stability: **stable command contract**, Folia-safe (concurrent per-player buffers).

### 3.4 Custom stat system — **L**

`dev.willram.ramcore.stat`: `Stat(ContentId id, double base, double min, double max, StatFormat)`, `StatRegistry` (a `ContentRegistry<Stat>`), `StatModifier(statId, StatOperation {ADD, MULTIPLY}, amount, sourceKey)`, `StatSource { Collection<StatModifier> modifiers(Player) }`. Built-in sources: `ItemStatSource` (reads a `ramcore:stats` PDC map on equipped items, written via `ItemStackBuilder.stat(..)`), `BuffStatSource` (timed, `Terminable`), `PartyStatSource`, `RegionStatSource` (region metadata from 1.5's tracker). `StatService.snapshot(Player) -> StatSnapshot` (immutable, cached), invalidated on `PlayerArmorChangeEvent`, `PlayerItemHeldEvent`, buff change, party change; computed on the player's scheduler. Combat: `DamageCalculator` consuming attacker/defender snapshots (crit chance/damage, lifesteal, elemental resistances) → `DamageBreakdown`; `DamageProfile.Builder.withStats(attacker, defender)` feeds it; `CombatControls.damage(..)` unchanged for callers that do not opt in. Config type `stats`. Stability: **experimental**.

### 3.3 Player ability framework — **XL**

`dev.willram.ramcore.ability`: `Ability(id, cooldown, StatCost, castTicks, AbilityTargeting, effects, AbilityAction)`, `AbilityContext(caster, targets, snapshot, trigger)`, `AbilityRegistry` in the content registry, `AbilityCaster` per player (cooldown via `CooldownTracker<CooldownKey>`, cast timer via `Schedulers.runLater(player, ..)`, cost via 3.4). Triggers as a `TerminableModule`: item use (`PlayerInteractEvent` + `CustomItemIdentity`), hotbar slot (`PlayerItemHeldEvent`), command, swap-hands (`PlayerSwapHandItemsEvent`), custom. Experimental extensions: `AbilityInterrupt` (damage/move cancels cast), channelling (`runTimer` on the player), combo chains (state machine with a window). Config type `abilities` via 1.6. Tests: caster state machine on `FakeScheduler` with proxied players. Stability: **experimental**.

### 3.1 Content-defined items → resource pack — **XL**

`ResourcePackBuilder`: inputs are the `ResourcePackAssetRegistry` plus `AssetSource`s (bytes from plugin jar resources or paths). Writes `pack.mcmeta` (pack format configurable, default from a small version table), `assets/<ns>/items/<name>.json` (modern item-model definition), `assets/<ns>/models/item/<name>.json`, textures, sounds.json; zips; SHA-1 → `ResourcePackMetadata`. Incremental: `pack-manifest.json` with per-file hashes → `PackBuildReport(added, changed, removed, unchanged, sha1)`. Hosting: optional `com.sun.net.httpserver.HttpServer` (JDK built-in) on a configured bind/port serving the zip, or `external-path` for outside hosting. Prompting via the existing `ResourcePackPrompt`/`ResourcePackPromptTracker` (which finally gets a scheduled `sweepTimeouts`). Item side: `ItemComponentProfile.itemModel(Key)` already exists; `ContentRegistrar` (1.6) applies it. All I/O on async; Folia-safe. Stability: **experimental**.

### 3.5 Dialogue system — **L**

`dev.willram.ramcore.dialogue`: `Dialogue` graph of `DialogueNode(id, text, choices, condition, actions)`, `DialogueChoice(label, nextNodeId, condition)`, `DialogueAction` (reward plan, objective event, command, open menu, custom). Conditions reuse `Predicate<DialogueContext>` with built-ins (permission via `PermissionRequirement`, objective state, metadata). `DialogueSession` per player: chat presentation with `ClickEvent.callback` (Paper `ClickCallback`, no command registration), and menu presentation via `MenuView`. Attach points: `Dialogues.onClick(dialogue)` as an `NpcClickHandler`; `ObjectiveTask` `RUN_ACTION` target `dialogue:<id>`. Config type `dialogues`. Tests: graph traversal with fake conditions/actions. Stability: **experimental**.

### 3.7 Hot-reloadable content packs — **L**

`ContentReloadService.reload(ContentPack) -> Promise<ContentDiff>`: reruns `ContentLoader`, diffs against the previous `ContentSnapshot` (id + node hash) → `ContentDiff(added, removed, changed, brokenReferences, rebuilt, failed)`. Live objects implement `TemplateBound { ContentId templateId(); void rebind(Object resolved); }` and register in a `LiveObjectRegistry`; on reload each is rebuilt on its owner scheduler (entity for NPCs/displays, region for holograms, global for regions) and failures are collected, never thrown. `/ramcore diagnostics reload <plugin>` prints the diff. Stability: **experimental**.

### 3.8 Real-time and cron scheduling — **L**

`RealTimeScheduler` service: `Job(id, Schedule, ZoneId, MissedRunPolicy {SKIP, CATCH_UP(max)}, TaskContext, Runnable)`; `Schedule.cron("0 4 * * 1")` (5-field parser written in-house with tests; no dependency), `Schedule.every(Duration)`, `Schedule.at(LocalTime)`. State in a `Store<String, JobState(lastRun, nextRun)>` from 1.1. Runner: async timer every second computing due jobs, executing each on its declared `TaskContext`; on startup computes missed runs and applies the policy. Tests with `FakeClock` + `FakeScheduler`. Stability: **experimental**.

### 3.2 Instanced worlds — **XL** (last: highest manual-test burden)

`WorldInstanceService.create(templateName, options) -> Promise<WorldInstance>`; templates under `plugins/<plugin>/world-templates/<name>/`; copy to `<server>/ramcore_inst_<name>_<shortId>/` skipping `uid.dat`/`session.lock`; write `ramcore-instance.json` marker; `Bukkit.createWorld` on `Schedulers.runGlobal`; apply a `RuleRegion` boundary and rule set; bind to `PartyGroup`/`EncounterInstance` via terminables. `close()`: teleport players out (`teleportAsync` per player scheduler), `unloadWorld(false)`, delete the directory async. Startup sweep deletes any directory carrying the marker. Behind a `WorldBackend` interface with an in-memory fake so copy/marker/sweep logic is unit-tested. **Risk:** Folia has not supported runtime world creation; gate on an `NmsCapability.WORLD_INSTANCES` check that fails with an actionable message on regionised servers, and verify on Folia 26.1 during this task. Stability: **Paper-experimental**.

### Examples — **M**

`examples/sample-plugin` (separate module, not part of the default build): config-loaded custom item with generated pack (1.6 + 3.1), a player ability using a custom stat (3.3 + 3.4), an NPC dialogue that starts an objective (3.5), a party entering an instanced dungeon (3.2), a reward paid through the Vault bridge (1.5). Smoke-tested on Paper and Folia via `run-paper`/`run-folia` (or manually if Maven was kept); results recorded in `RELEASE_READINESS.md`.

---

## 9. Decisions (made 2026-09-10)

1. **Order deviation** — accepted: 2.1a/2.3 run before 1.1.
2. **ADR-0001 version policy** — (a) latest-only. See `docs/decisions/ADR-0001-version-support-policy.md`.
3. **ADR-0002 build tool** — Gradle Kotlin DSL as the first commit of 2.2. See `docs/decisions/ADR-0002-build-tool.md`.
4. **ADR-0003 optional dependencies** — Paper `PluginLoader` runtime resolution for Hikari/JDBC/Redis; shade only bStats and coroutines. See `docs/decisions/ADR-0003-optional-dependencies.md`.
5. **bStats plugin id** — `33973` (registered by the owner). 2.5 compiles it in as the default; `metrics.enabled` in `config.yml` remains the opt-out.
6. **Folia and instanced worlds** — accepted: 3.2 may ship Paper-only, gated by a capability check.

---

## 10. Risks and open questions

- Paper 26.1 API details to verify at implementation time: `MenuType.ANVIL`/`AnvilView` rename text access (1.4), `Player.openSign` on a virtual sign (1.4), `Bukkit.isOwnedByCurrentRegion` overloads (2.4), item-model JSON schema for the current pack format (3.1), Folia `createWorld` (3.2).
- `Material`/`ItemStack` cannot be constructed off-server on modern Paper; every config-to-item path is tested at the spec level and converted on the server (1.6, 3.1). Existing tests already use a `FakeItemStack` for the same reason.
- `Promise.java` and `Schedulers.java` are large; the audit adds a `TaskContext` overload family, growing them further. Accept it; a later 2.x task may generate the overloads.
- `LootReward.payload` being `Object` limits what `PersistentLootInstanceStore` can persist without a consumer codec. Documented, not solved.
- The `RamExecutors` static-executor bug (A3) may already bite on servers that `/reload` or hot-disable a consumer plugin. Fix early in 2.3.
