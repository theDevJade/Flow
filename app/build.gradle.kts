plugins {
    id("com.gradleup.shadow") version "9.0.0"
    kotlin("jvm")
    id("xyz.jpenilla.run-paper") version "3.0.0-beta.1"
}

group = "dev.mosaic"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":webserver"))
    implementation(project(":flow"))
    implementation(project(":common"))
    implementation(project(":plugin"))
    implementation(project(":lang"))
    shadow(project(":plugin"))

    // Shadowed libraries for Bukkit runtime
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.reflections:reflections:0.10.2")
}

tasks {
    runServer {
        dependsOn(shadowJar)
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.8")
    }
}

// Configure shadowJar
tasks.shadowJar {
    archiveBaseName.set("Flow")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())

    // Relocation (avoid classpath conflicts in Bukkit)
    relocate("kotlinx.coroutines", "dev.flow.libs.coroutines")
    relocate("org.reflections", "dev.flow.libs.reflections")
    relocate("io.netty", "dev.flow.libs.netty")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Optionally filter out specific META-INF files if still problematic
    exclude("META-INF/io.netty.versions.properties")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
