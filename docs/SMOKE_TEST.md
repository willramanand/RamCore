# RamCore Phase D — Manual Smoke Test

Everything in RamCore is covered by off-server unit tests (`FakeScheduler`/`FakeClock`/proxied Bukkit
objects/`@TempDir`). This checklist is the **live-server** validation those cannot provide, and is the
Phase D gate: the example plugin demonstrating all five DoD scenarios on **Paper** and **Folia**
(26.1). Record results in `RELEASE_READINESS.md`.

## Prerequisites

1. Build artifacts (JDK 25 toolchain; `JAVA_HOME` at a JDK 25 or let Gradle provision it):
   ```
   ./gradlew build
   # RamCore plugin:      ramcore-paper/build/libs/RamCore-2.0.0.jar
   # Java sample:         examples/sample-plugin/build/libs/*.jar
   # Kotlin sample:       examples/sample-plugin-kotlin/build/libs/*.jar   (+ Kotlin stdlib on the server)
   ```
   Note: the example modules produce plain jars (no shadowing). The Kotlin sample needs the Kotlin
   stdlib available at runtime (shade it into that jar, or drop `kotlin-stdlib` in `plugins/`).
2. Two test servers on 26.1: one **Paper**, one **Folia**.
3. On each: drop `RamCore-2.0.0.jar` + one sample jar into `plugins/`. For the dungeon test, create a
   template world folder at `plugins/RamCoreSample/world-templates/dungeon/` (copy any small world;
   the copier skips `uid.dat`/`session.lock`).

## Scenarios

Run each `/sample …` command in-game as an op. `[P]` = Paper expectation, `[F]` = Folia expectation.

| # | DoD scenario | Command / trigger | Expect (P) | Expect (F) | Result |
| - | --- | --- | --- | --- | --- |
| 1 | Custom item + generated resource pack (3.1) | `/sample pack` | Replies `Built pack, sha1=…`; `plugins/RamCoreSample/pack.zip` exists, is a valid zip with `pack.mcmeta` + `assets/sample/items/ruby_sword.json` | same (pure file I/O) | ☐ |
| 2 | Ability spending a custom stat (3.3+3.4) | hold hotbar slot 0 / `/sample cast` while looking at a mob | Status `CAST`; the looked-at mob catches fire ~3 s; recast within 3 s → `ON_COOLDOWN`; with no `power` → `INSUFFICIENT_COST` (base source grants 25, so it casts) | same; cast + fire run on the caster/target region thread without a thread error | ☐ |
| 3 | NPC dialogue that starts an objective (3.5) | `/sample talk` | Chat shows the node text + clickable `Accept`/`Not now`; clicking `Accept` shows the follow-up + `(quest objective started)`; no command registered for the click | same; clicks handled on the player thread | ☐ |
| 4 | Party entering an instanced dungeon (3.2) | `/sample dungeon` | Replies `Creating dungeon…` then `Dungeon ready: ramcore_inst_dungeon_<id>`; a new world dir appears with `ramcore-instance.json`; restart → startup sweep deletes it | Replies with the **actionable refusal** ("runtime world instances are not supported… Folia does not support runtime world creation"); no world dir created | ☐ |
| 5 | Reward via the Vault bridge (1.5) | dialogue reward action / economy | With Vault + an economy plugin present, the reward deposits; without, the in-memory economy path runs | same | ☐ |

## Cross-cutting checks

- **Diagnostics/reload (3.7):** register a `ContentPack` (or use a consumer that does), then
  `/ramcore diagnostics reload <pack>` → prints `added/changed/removed/rebuilt/failed` counts; edit a
  file and re-run → shows it as `changed`/`rebuilt`.
- **Scheduling (3.8):** register a `RealTimeScheduler` job (`Schedule.every(Duration.ofSeconds(5))`)
  and confirm it fires on its `TaskContext`; a `cron("0 4 * * *")` job fires at 04:00 server-local.
- **No thread errors:** on Folia, watch the console for `IllegalStateException`/region-ownership
  errors during any scenario — there should be none.
- **Shutdown:** stop the server cleanly; confirm no leaked instance worlds remain and no async task
  warnings on disable.

## Recording

Fill the Result column and the Paper/Folia columns in `RELEASE_READINESS.md`. Any failure →
open an issue with the scenario #, server type, and console excerpt; keep the affected package's
stability at **experimental** until it passes.
