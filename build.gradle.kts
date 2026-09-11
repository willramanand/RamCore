plugins {
    kotlin("jvm") version "2.3.20" apply false
}

val paperVersion = "26.1.2.build.60-stable"
val junitVersion = "5.11.4"

allprojects {
    group = "dev.willram"
    version = "2.0.0"
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://jitpack.io")
        maven("https://repo.extendedclip.com/releases/")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
        options.encoding = "UTF-8"
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:$junitVersion")
        // Gradle 9 no longer puts the JUnit Platform launcher on the test runtime classpath
        // automatically (IntelliJ's bundled Gradle is 9.x); add it explicitly so tests run on
        // both the 8.8 wrapper and Gradle 9+.
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.11.4")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // Every module except the shaded runtime plugin is a consumable library (JitPack / Maven).
    if (name != "ramcore-paper") {
        apply(plugin = "maven-publish")
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }
    }
}
