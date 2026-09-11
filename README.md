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

The build is Gradle 9.1 (Kotlin DSL) and emits Java 25 bytecode ([ADR-0002](docs/decisions/ADR-0002-build-tool.md)). Gradle 9.1 runs on JDK 25 directly (use JDK 25 as the Gradle JVM in your IDE); it compiles with a Java 25 toolchain, provisioned automatically when no local JDK 25 is found.

```sh
./gradlew build
```

The shaded plugin jar is written to `ramcore-paper/build/libs/RamCore-<version>.jar`; it is a single relocated artifact identical in shape to the previous Maven output. Run all tests with `./gradlew test`. A live Paper/Folia smoke test remains a manual step (see [release readiness](RELEASE_READINESS.md) and [smoke test](docs/SMOKE_TEST.md)).

## Modules

RamCore is split into library modules plus the runtime plugin:

| Module | Role |
| --- | --- |
| `ramcore-api` | The public API and most implementations; compiles against Paper API only |
| `ramcore-nms` | Version-sensitive NMS reflection helpers |
| `ramcore-protocol` | ProtocolLib-backed packet, scoreboard and protocol event support |
| `ramcore-kotlin` | Kotlin extensions and DSLs |
| `ramcore-test` | Test-support fakes (`FakeScheduler`, proxy fakes, `FakeClock`) |
| `ramcore-paper` | The plugin main; shades the others into the runtime jar |

## Consuming RamCore

Published via [JitPack](https://jitpack.io). Depend on the module you need (usually `ramcore-api`, plus `ramcore-test` for tests).

Gradle (Kotlin DSL):

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    compileOnly("com.github.willramanand.RamCore:ramcore-api:<tag>")
    testImplementation("com.github.willramanand.RamCore:ramcore-test:<tag>")
}
```

Maven:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.willramanand.RamCore</groupId>
    <artifactId>ramcore-api</artifactId>
    <version>TAG</version>
    <scope>provided</scope>
</dependency>
```

Publishing to GitHub Packages via `maven-publish` is the alternative to JitPack; the modules already apply `maven-publish` (except `ramcore-paper`), so `./gradlew publish` with a configured repository works too. Runtime consumers just install the shaded `RamCore-<version>.jar` as a plugin; only plugins compiling against the API need the JitPack dependency.
