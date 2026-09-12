# RamCore Phase D — Release Readiness

Status of the Phase D (gameplay platform) definition-of-done scenarios, demonstrated by
`examples/sample-plugin` (`dev.willram.ramcore.example.SamplePlugin`).

The example ships in two forms — `examples/sample-plugin` (Java) and `examples/sample-plugin-kotlin`
(Kotlin, using the RamCore command DSL and SAM APIs) — both included in the Gradle build so their API
usage stays compilable (`./gradlew :examples:sample-plugin:compileJava`,
`./gradlew :examples:sample-plugin-kotlin:compileKotlin`); they ship nothing. The Kotlin twin also
needs the Kotlin stdlib on the server at runtime. Live server smoke tests must be
run manually against a Paper and a Folia server (26.1) — automated tests cover each subsystem's logic
off-server (see the per-task `Outcome` notes in `docs/ROADMAP_EXECUTION_PLAN.md`).

## DoD scenarios

| # | Scenario | Tasks | Demonstrated in example | Automated coverage | Paper smoke | Folia smoke |
| - | --- | --- | --- | --- | --- | --- |
| 1 | Config-loaded custom item with a generated resource pack | 1.6, 3.1 | `/sample pack` builds a pack for `sample:ruby_sword`; item side via `ItemStackBuilder.stat`/`ResourcePackItems` | `ResourcePackBuilderTest`, `ResourcePackPrimitivesTest`, `ResourcePackHostTest` | PASS (Paper 26.1) | PASS (Folia 26.1.2) |
| 2 | Player ability that spends a custom stat | 3.3, 3.4 | `/sample cast` casts `sample:strike` (cost `sample:power`, look-at target) | `AbilityCasterTest`, `StatServiceTest`, `DamageCalculatorTest` | PASS (Paper 26.1) | PASS (Folia 26.1.2) |
| 3 | NPC dialogue that starts an objective | 3.5 | `/sample talk` runs the `sample:guide` dialogue; `Dialogues.onClick` attaches to an NPC | `DialogueSessionTest`, `DialogueModelTest`, `DialogueSpecTest` | PASS (Paper 26.1) | PASS (Folia 26.1.2) |
| 4 | Party entering an instanced dungeon | 3.2 | `/sample dungeon` creates a throwaway world from `world-templates/dungeon` | `WorldInstanceServiceTest`, `WorldInstancesTest` (in-memory backend) | PASS (Paper 26.1) — world created + swept | PASS — actionable refusal (Folia unsupported, expected) |
| 5 | Reward paid through the Vault bridge | 1.5 | dialogue action hook (`DialogueActions.reward` / economy) | economy/reward tests from Phase B | pending | not tested |

## Smoke log

- **2026-09-11, Folia 26.1.2:** scenarios 1–4 PASS (see table). Two live bugs found and fixed during the run: dialogue chat clicks threw `ApiMisuseException` on stale/repeat clicks (fixed via a presentation-generation guard, `5b0d721`), and `/sample dungeon` swallowed the create() failure so nothing showed (sample now surfaces it via `exceptionallyAsync`, `582d051`). No Folia region-ownership errors observed. Scenario 5 (reward/Vault) not tested. Paper run still pending (dungeon creation + startup sweep).
- **2026-09-12, Paper 26.1:** scenarios 1–4 PASS. `/sample pack`, `/sample cast`, `/sample talk` all ran without errors. `/sample dungeon` created the instance world (`ramcore_inst_dungeon_5c8d0f12`), spawn area prepared, world dir carried the `ramcore-instance.json` marker; restart → startup sweep deleted the leftover instance. No teleport in scope (create-only path). Scenario 5 (reward/Vault) not tested (needs an economy plugin). Phase D live-server gate now met on both platforms except scenario 5.

## Notes

- **Instanced worlds are Paper-only.** `WorldInstanceService.create` refuses on Folia via
  `PaperWorldBackend.supportsInstances()` (returns false when `RegionizedServer` is present) with an
  actionable message. The Folia "smoke test" is verifying that refusal, not a created world.
- **Scheduling / hot-reload** (3.8 / 3.7) are exercised through the diagnostics command
  (`/ramcore diagnostics reload <pack>`) and `RealTimeScheduler`; both have off-server tests with
  `FakeClock`/`FakeScheduler`.
- To run the example: build RamCore (`./gradlew build`), install the RamCore jar and the sample jar on
  a test server, and drive the `/sample …` commands above. Step-by-step Paper + Folia instructions and
  per-scenario expectations are in `docs/SMOKE_TEST.md`.

## Full build

`./gradlew build` — all modules compile and every module's test suite passes (JDK 25 toolchain).
