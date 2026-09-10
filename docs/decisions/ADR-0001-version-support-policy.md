# ADR-0001: Version support policy

- Status: accepted (2026-09-10)
- Roadmap task: 2.6

## Context

RamCore 2.x targets Java 25 and Paper `26.1.2+`. The roadmap asked for a decision between (a) staying latest-only with a clear policy, or (b) maintaining a backport branch for Java 21 / Paper 1.21.x.

The code already emits Java 25 bytecode and uses Paper 26.1 data-component APIs (`item.component`), `Player.locale()`, `MenuType`, `ClickCallback`, and the regionised scheduler surface. A Java 21 / 1.21.x backport would need version adapters for each of those behind `NmsAccessStrategy`, a second CI target, a second smoke-test matrix on Paper and Folia, and the capability registry would report most advanced features `UNSUPPORTED` on the older line anyway, so the backport would be a materially smaller library carrying the same maintenance cost.

## Decision

**(a) Latest-only.** RamCore tracks the current Paper major line.

When Paper moves to a new major line:

1. RamCore cuts a `release/<old-version>` branch from the last commit that targeted the old line.
2. That branch receives fixes only, for one Minecraft release cycle, and only when someone asks for a specific fix.
3. `master` moves to the new line immediately; APIs that Paper removed or renamed are adapted on `master` under the normal deprecation policy.

No Java 21 / Paper 1.21.x backport branch is created.

## Consequences

- One CI target, one smoke matrix (Paper + Folia on the current line).
- Consumers on older Paper builds pin an older RamCore release.
- If the decision is ever reversed, the surfaces to isolate are listed above; each would become a `VersionAdapter` selected through `NmsAccessRegistry` capability checks.
