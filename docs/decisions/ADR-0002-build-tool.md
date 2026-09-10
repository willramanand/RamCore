# ADR-0002: Build tool for the multi-module split

- Status: accepted (2026-09-10)
- Roadmap task: 2.2

## Context

Task 2.2 splits RamCore into `ramcore-api`, `ramcore-paper`, `ramcore-protocol`, `ramcore-nms`, `ramcore-kotlin`, and `ramcore-test`, and asked whether Gradle would be more beneficial than the current Maven build.

| Concern | Maven (stay) | Gradle Kotlin DSL (switch) |
| --- | --- | --- |
| Multi-module scoping enforcement | Works: the api module simply has no dependency on the others, so a bad import fails to compile | Same, plus `java-library` `api`/`implementation` scoping for consumers |
| Mixed Java + Kotlin | Works, but requires the current workaround of disabling `default-compile` and sequencing the Kotlin plugin first | First-class |
| Shading and relocation per module | `maven-shade-plugin`, already configured | `shadow` plugin, equivalent |
| Paper ecosystem tooling | `run-paper` / `run-folia` (one-command smoke servers), `paperweight-userdev` (Mojang-mapped NMS), `resource-factory` (generated `paper-plugin.yml`) are **Gradle-only** | All available |
| Publishing | `distributionManagement` or JitPack | `maven-publish` or JitPack (JitPack supports both) |
| Migration cost | None | About one session now (the pom is roughly 300 lines); grows after the split |
| Risk | None | IDE re-index, contributor familiarity |

Every Phase 3 task needs manual smoke tests on both Paper and Folia, and the existing smoke logs in `RELEASE_READINESS.md` were produced by hand. `todo.md` also anticipates versioned NMS adapters (`nms.v1_21_x`), for which `paperweight-userdev` is the only practical toolchain.

## Decision

**Switch to Gradle (Kotlin DSL) as the first commit of task 2.2, before any module is created.**

Sequence inside 2.2:

1. `settings.gradle.kts` + root `build.gradle.kts` reproducing the current single shaded jar exactly (same relocations, same manifest entries, same `paper-plugin.yml` filtering).
2. Verify the jar loads on Paper and Folia via `run-paper` / `run-folia` before moving any package.
3. Create the modules.

Until 2.2 starts, the Maven build stays and is what CI runs.

## Consequences

- One build-tool migration, not two.
- Smoke testing becomes `./gradlew runPaper` / `./gradlew runFolia`, and those results replace the hand-recorded table in `RELEASE_READINESS.md`.
- `pom.xml` is deleted at the end of 2.2 PR 1; JitPack builds from Gradle automatically.
