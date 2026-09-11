val paperVersion = "26.1.2.build.60-stable"

dependencies {
    api(project(":ramcore-api"))
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
}
