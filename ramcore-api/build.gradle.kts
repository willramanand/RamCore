val paperVersion = "26.1.2.build.60-stable"

dependencies {
    // shaded into the runtime jar by ramcore-paper (relocated there)
    api("org.spongepowered:configurate-core:4.2.0")
    api("org.spongepowered:configurate-hocon:4.2.0")
    api("org.spongepowered:configurate-yaml:4.1.2") {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "org.spongepowered", module = "configurate-core")
    }
    api("com.typesafe:config:1.4.1")
    api("com.flowpowered:flow-math:1.0.3")
    api("me.lucko:shadow-bukkit:1.20.1")
    compileOnlyApi("org.jetbrains:annotations:26.0.2")

    // provided by the server or resolved by the plugin loader
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.zaxxer:HikariCP:6.3.0")

    // pure API classes (e.g. ability geometry) are unit-tested against Paper types like Vector
    testImplementation("io.papermc.paper:paper-api:$paperVersion")
}
