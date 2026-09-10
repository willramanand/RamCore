# RamCore

RamCore is a Paper plugin utility library for building server plugins with cleaner lifecycle, command, scheduling, event, metadata, serialization, GUI, scoreboard, and persistence APIs.

Start with the public API guide:

- [Public API documentation](docs/API.md)
- [Module boundaries and stability policy](docs/MODULE_BOUNDARIES.md)
- [Release readiness notes](docs/RELEASE_READINESS.md)
- [2.x roadmap execution plan](docs/ROADMAP_EXECUTION_PLAN.md) and [architecture decisions](docs/decisions/)

## Requirements

- Java 25
- Paper API `26.1.2` or newer compatible `26.1+` builds

RamCore intentionally compiles with Java 25 because the current Paper `26.1+` line requires Java 25. Servers still on Paper `1.21.11` or older Java 21-era builds should stay on an older RamCore release.

## Version support policy

RamCore tracks the current Paper major line only ([ADR-0001](docs/decisions/ADR-0001-version-support-policy.md)). When Paper moves to a new line, the previous RamCore line gets a `release/<version>` branch that receives fixes only, for one Minecraft release cycle, and only on request. There is no maintained Java 21 / Paper 1.21.x backport.

## Building

Requires a JDK 25 on `JAVA_HOME`; the build emits Java 25 bytecode and fails on older JDKs even if one is on `PATH`.

```sh
export JAVA_HOME=/path/to/jdk-25
mvn -B verify
```

The shaded plugin jar is written to `target/RamCore-<version>.jar`. CI runs the same command on every push and pull request.
