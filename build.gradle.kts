plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

version = providers.gradleProperty("pluginVersion").orElse("1.0.0").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
    withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
    }
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    runServer {
        minecraftVersion("1.20.1")
        javaLauncher.set(project.javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        })
    }
}
