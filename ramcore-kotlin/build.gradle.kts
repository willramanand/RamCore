import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

val paperVersion = "26.1.2.build.60-stable"

dependencies {
    api(project(":ramcore-api"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.0")
    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    testImplementation(project(":ramcore-test"))
    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    testImplementation(kotlin("test-junit5"))
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}
