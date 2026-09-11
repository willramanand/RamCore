// Example consumer plugin. Included in the build so the demonstrated API usage stays compilable,
// but it produces only an example jar (no shadowing) and is not shipped.
val paperVersion = "26.1.2.build.60-stable"

dependencies {
    // Provided at runtime by the installed RamCore plugin; compileOnly so we compile against its API.
    compileOnly(project(":ramcore-api"))
    compileOnly(project(":ramcore-paper"))
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
}
