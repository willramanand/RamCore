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
| 1 | Config-loaded custom item with a generated resource pack | 1.6, 3.1 | `/sample pack` builds a pack for `sample:ruby_sword`; item side via `ItemStackBuilder.stat`/`ResourcePackItems` | `ResourcePackBuilderTest`, `ResourcePackPrimitivesTest`, `ResourcePackHostTest` | pending | pending |
| 2 | Player ability that spends a custom stat | 3.3, 3.4 | `/sample cast` casts `sample:strike` (cost `sample:power`, look-at target) | `AbilityCasterTest`, `StatServiceTest`, `DamageCalculatorTest` | pending | pending |
| 3 | NPC dialogue that starts an objective | 3.5 | `/sample talk` runs the `sample:guide` dialogue; `Dialogues.onClick` attaches to an NPC | `DialogueSessionTest`, `DialogueModelTest`, `DialogueSpecTest` | pending | pending |
| 4 | Party entering an instanced dungeon | 3.2 | `/sample dungeon` creates a throwaway world from `world-templates/dungeon` | `WorldInstanceServiceTest`, `WorldInstancesTest` (in-memory backend) | pending (Paper only) | expected refusal (Folia unsupported) |
| 5 | Reward paid through the Vault bridge | 1.5 | dialogue action hook (`DialogueActions.reward` / economy) | economy/reward tests from Phase B | pending | pending |

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
