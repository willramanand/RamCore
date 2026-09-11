pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // lets Gradle provision the Java 25 toolchain (e.g. on JitPack) when no local JDK 25 is found
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "RamCore"

include(
    "ramcore-api",
    "ramcore-nms",
    "ramcore-protocol",
    "ramcore-kotlin",
    "ramcore-test",
    "ramcore-paper"
)
