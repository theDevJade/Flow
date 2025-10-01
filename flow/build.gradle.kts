plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    application
}

group = "com.thedevjade.io"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation(project(":common"))
    implementation(project(":lang"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}


tasks.register("runWebSocketServer") {
    group = "application"
    description = "Run the WebSocket server"
    dependsOn(":webserver:runWebSocket")
}

// Task to build and test all modules
tasks.register("testBuild") {
    group = "verification"
    description = "Build and test all modules without running tests"
    dependsOn(":common:build", ":flow:build", ":webserver:build", ":plugin:build")
}