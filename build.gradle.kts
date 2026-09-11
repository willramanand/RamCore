import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("com.gradleup.shadow:shadow-gradle-plugin:8.3.6")
        // ASM 9.8 reads Java 25 (class file major 69); the version shadow bundles does not
        classpath("org.ow2.asm:asm:9.8")
        classpath("org.ow2.asm:asm-commons:9.8")
    }
    configurations.classpath {
        resolutionStrategy {
            force("org.ow2.asm:asm:9.8", "org.ow2.asm:asm-commons:9.8", "org.ow2.asm:asm-tree:9.8", "org.ow2.asm:asm-analysis:9.8")
        }
    }
}

plugins {
    `java-library`
    kotlin("jvm") version "2.3.20"
}

apply(plugin = "com.gradleup.shadow")

group = "dev.willram"
version = "2.0.0"

val javaVersion = 25
val hikariVersion = "6.3.0"
val sqliteVersion = "3.46.0.0"
val junitVersion = "5.11.4"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    // shaded into the plugin jar (relocated below)
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains:annotations:26.0.2")
    implementation("org.spongepowered:configurate-core:4.2.0")
    implementation("org.spongepowered:configurate-hocon:4.2.0")
    implementation("org.spongepowered:configurate-yaml:4.1.2") {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "org.spongepowered", module = "configurate-core")
    }
    implementation("com.typesafe:config:1.4.1")
    implementation("com.flowpowered:flow-math:1.0.3")
    implementation("me.lucko:shadow-bukkit:1.20.1")

    // provided at runtime by the server or resolved by the plugin loader
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.60-stable")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.zaxxer:HikariCP:$hikariVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.20")
    testImplementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    // tests compile against the provided APIs too
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.60-stable")
    testImplementation("com.zaxxer:HikariCP:$hikariVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withSourcesJar()
}

kotlin {
    jvmToolchain(javaVersion)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersion)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand("project" to project)
    }
}

// Reproduce the Maven single shaded artifact exactly: RamCore-2.0.0.jar with relocations.
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    manifest {
        attributes(
            "Main-Class" to "dev.willram.ramcore.RamCore",
            "paperweight-mappings-namespace" to "spigot"
        )
    }

    relocate("org.spongepowered.configurate", "dev.willram.ramcore.libs.configurate")
    relocate("com.typesafe.config", "dev.willram.ramcore.libs.typesafe.config")
    relocate("com.flowpowered.math", "dev.willram.ramcore.libs.flowpowered.math")
    relocate("org.yaml.snakeyaml", "dev.willram.ramcore.libs.snakeyaml")

    exclude("module-info.class")
    exclude("META-INF/versions/*/module-info.class")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
}

tasks.named("build") {
    dependsOn("shadowJar")
}
