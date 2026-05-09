# Release Readiness

This file records release checks that are not fully covered by unit tests.

## Java And Paper Target

- Java runtime and bytecode target: `25`
- Paper API dependency: `26.1.2.build.60-stable`
- Runtime descriptor API version: `26.1.2`

Paper documents Java 25 as the recommended runtime for `26.1+`, so RamCore 2.0 can intentionally require Java 25 for downstream consumers.

## Server Smoke Tests

Latest local smoke run:

| Server | Version | Build | Result |
| --- | --- | --- | --- |
| Paper | `26.1.2` | `61` | RamCore loaded, enabled, and server reached `Done`. |
| Folia | `26.1.2` | `8` | RamCore loaded, enabled, and server reached `Done`. |

Smoke logs were generated under `C:\tmp\ramcore-smoke`. Both runs showed `RamCore v1.0.0-SNAPSHOT`, `ENABLE COMPLETE`, and no RamCore load failure.

## Shade Review

`mvn package` no longer reports Maven Shade overlapping-file warnings. Nested Java module descriptors are excluded with `META-INF/versions/*/module-info.class`.

## Diagnostics Default

The bundled `/ramcore` diagnostics command remains enabled by default for public releases because the command is paste-safe and permission-gated by `ramcore.diagnostics`, which defaults to op. Hosts that do not want the command surface can start the server with `-Dramcore.diagnostics=false` to skip registration entirely.
