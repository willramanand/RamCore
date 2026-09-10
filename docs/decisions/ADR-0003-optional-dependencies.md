# ADR-0003: Optional heavy dependencies

- Status: accepted (2026-09-10)
- Roadmap tasks: 1.1 (HikariCP, JDBC drivers), 2.4 (kotlinx-coroutines), 2.5 (bStats, Redis client)

## Context

Task 1.1 asked to keep HikariCP "as an optional shaded dependency" so consumers that only use file storage are not forced to carry it. RamCore ships as a single runtime plugin jar shared by every consumer plugin on the server, so anything shaded into it is present for everyone. `sqlite-jdbc` alone is roughly 13 MB with native libraries; MySQL, MariaDB, and PostgreSQL drivers and a Redis client add several MB more.

Options considered:

1. Shade with `<optional>true</optional>` (or Gradle `compileOnly` + shadow include). Simple, but the plugin jar carries every driver for every server.
2. Paper's `PluginLoader` API: a `RamCoreLoader` class declared in `paper-plugin.yml` uses `MavenLibraryResolver` to download the libraries from Maven Central at startup, only when RamCore's `config.yml` enables SQL or Redis. Nothing heavy is shaded. Libraries are cached by Paper under `libraries/`.

## Decision

**Option 2 for HikariCP, JDBC drivers, and the Redis client (Lettuce).** Those artifacts are `provided`/`compileOnly` at build time and `test` scope for in-process tests (SQLite in-memory).

Classes that reference them (`SqlStore`, `RedisMessageBus`) are loaded lazily through factories (`Stores.sql(...)`, `MessageBuses.redis(...)`) that check class presence first and throw an `ApiMisuseException` naming the config key to enable ("enable `storage.sql.enabled` in `plugins/RamCore/config.yml`") when the library was not resolved.

**Shade only small, pure-Java libraries** that must be relocated by their own terms or are needed by every consumer: bStats (`bstats-bukkit`, relocation required by bStats) in the plugin jar, and `kotlinx-coroutines-core` inside `ramcore-kotlin` only.

## Consequences

- The plugin jar stays small; file-storage-only servers download nothing extra.
- Servers without internet access must place the resolved libraries in Paper's `libraries/` directory manually; document this in `API.md` under the SQL store section.
- The loader reads config before the plugin's `onLoad`, so the SQL/Redis toggles live in a small, loader-owned section of `config.yml` that is parsed without Bukkit APIs.
- Unit tests never depend on the loader; they use the in-process SQLite driver at `test` scope.
