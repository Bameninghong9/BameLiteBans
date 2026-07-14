plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "de.bame"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") {
        content {
            includeGroup("com.gitlab.ruany")
        }
    }
}

dependencies {
    // Velocity API (wird vom Proxy bereitgestellt -> compileOnly)
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("com.gitlab.ruany:LiteBansAPI:0.6.1")
    compileOnly("net.luckperms:api:5.4")
    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.17")
    implementation("io.github.revxrsal:lamp.velocity:4.0.0-rc.17")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.javaParameters = true
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        // Relocate Lamp, Kotlin und TOML to prevent conflicts with other plugins
        relocate("revxrsal.commands", "de.bame.bamelitebans.shadow.lamp")
        relocate("kotlin", "de.bame.bamelitebans.shadow.kotlin")
        relocate("com.moandjiezana.toml", "de.bame.bamelitebans.shadow.toml")
    }

    build {
        dependsOn(shadowJar)
    }
}
