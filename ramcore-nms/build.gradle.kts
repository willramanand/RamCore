val paperVersion = "26.1.2.build.60-stable"

dependencies {
    api(project(":ramcore-api"))
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
}
