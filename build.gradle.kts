plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "dev.grip"
version = property("pluginVersion") as String

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.116.3")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 21
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") { expand(props) }
    }

    test {
        useJUnitPlatform()
    }

    jar {
        archiveFileName = "Grip-${project.version}.jar"
    }

    runServer {
        minecraftVersion("1.21.11")
    }
}
