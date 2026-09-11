import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

val paperVersion = "26.1.2.build.60-stable"

dependencies {
    api(project(":ramcore-api"))
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}
