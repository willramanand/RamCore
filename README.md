# RamCore

RamCore is a Paper plugin utility library for building server plugins with cleaner lifecycle, command, scheduling, event, metadata, serialization, GUI, scoreboard, and persistence APIs.

Start with the public API guide:

- [Public API documentation](docs/API.md)
- [Module boundaries and stability policy](docs/MODULE_BOUNDARIES.md)
- [Release readiness notes](docs/RELEASE_READINESS.md)

## Requirements

- Java 25
- Paper API `26.1.2` or newer compatible `26.1+` builds

RamCore intentionally compiles with Java 25 because the current Paper `26.1+` line requires Java 25. Servers still on Paper `1.21.11` or older Java 21-era builds should stay on an older RamCore release or a backport branch.
