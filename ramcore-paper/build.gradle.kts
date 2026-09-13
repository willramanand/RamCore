buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("com.gradleup.shadow:shadow-gradle-plugin:8.3.9")
        // ASM 9.8 reads Java 25 (class file major 69)
        classpath("org.ow2.asm:asm:9.8")
        classpath("org.ow2.asm:asm-commons:9.8")
    }
    configurations.classpath {
        resolutionStrategy {
            force("org.ow2.asm:asm:9.8", "org.ow2.asm:asm-commons:9.8", "org.ow2.asm:asm-tree:9.8", "org.ow2.asm:asm-analysis:9.8")
        }
    }
}

apply(plugin = "com.gradleup.shadow")

val paperVersion = "26.1.2.build.60-stable"

dependencies {
    implementation(project(":ramcore-api"))
    implementation(project(":ramcore-nms"))
    implementation(project(":ramcore-protocol"))
    implementation(project(":ramcore-kotlin"))

    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.zaxxer:HikariCP:6.3.0")
    compileOnly("io.lettuce:lettuce-core:6.3.2.RELEASE") // runtime-resolved (ADR-0003)
    implementation("org.bstats:bstats-bukkit:3.0.2")     // shaded + relocated

    testImplementation(project(":ramcore-test"))
    testImplementation(project(":ramcore-nms"))
    testImplementation(project(":ramcore-protocol"))
    testImplementation(project(":ramcore-kotlin"))
    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    testImplementation("net.dmulloy2:ProtocolLib:5.4.0")
    testImplementation("com.zaxxer:HikariCP:6.3.0")
    testImplementation("org.xerial:sqlite-jdbc:3.46.0.0")
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand("project" to project)
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RamCore")
    archiveClassifier.set("")
    manifest {
        attributes(
            "Main-Class" to "dev.willram.ramcore.RamCore",
            "paperweight-mappings-namespace" to "spigot"
        )
    }

    // configurate, typesafe-config, flow-math and snakeyaml (transitive via configurate-yaml) are
    // part of ramcore-api's public ABI (declared api(...) and exposed in ConfigurationNode/Vector3d
    // signatures), so they must NOT be relocated: a consumer plugin compiled against ramcore-api
    // emits the un-relocated names and would hit NoClassDefFoundError against a relocated jar.
    // See docs/MODULE_BOUNDARIES.md. Only non-API internals stay relocated.
    relocate("kotlinx.coroutines", "dev.willram.ramcore.libs.kotlinx.coroutines")
    relocate("org.bstats", "dev.willram.ramcore.libs.bstats")

    exclude("module-info.class")
    exclude("META-INF/versions/*/module-info.class")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
}

tasks.named("build") {
    dependsOn("shadowJar")
}
