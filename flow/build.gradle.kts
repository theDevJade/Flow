plugins {
    kotlin("jvm") version "2.2.20"
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
    implementation(project(":common"))
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

// Task to run WebSocket server (delegates to webserver module)
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