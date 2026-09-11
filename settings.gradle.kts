pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
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
