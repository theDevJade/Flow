plugins {
    id("com.gradleup.shadow") version "9.0.0"
    kotlin("jvm")
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

    // Shadowed libraries for Bukkit runtime
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.reflections:reflections:0.10.2")
}

// Configure shadowJar
tasks.shadowJar {
    archiveBaseName.set("Flow")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())

    // Relocation (avoid classpath conflicts in Bukkit)
    relocate("kotlinx.coroutines", "dev.mosaic.libs.coroutines")
    relocate("org.reflections", "dev.mosaic.libs.reflections")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
