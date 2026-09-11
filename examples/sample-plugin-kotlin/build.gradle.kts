import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kotlin twin of examples/sample-plugin. Included in the build so the demonstrated API usage stays
// compilable; it ships nothing. Requires the Kotlin stdlib at runtime alongside the RamCore plugin.
plugins {
    kotlin("jvm")
}

val paperVersion = "26.1.2.build.60-stable"

dependencies {
    // Provided at runtime by the installed RamCore plugin; compileOnly so we compile against its API.
    compileOnly(project(":ramcore-api"))
    compileOnly(project(":ramcore-paper"))
    compileOnly(project(":ramcore-kotlin"))
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}
