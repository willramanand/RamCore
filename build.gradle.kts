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
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
