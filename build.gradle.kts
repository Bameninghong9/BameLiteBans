plugins {
    kotlin("jvm") version "1.9.23"
    id("com.gradleup.shadow") version "8.3.6"
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
    }

    build {
        dependsOn(shadowJar)
    }
}
