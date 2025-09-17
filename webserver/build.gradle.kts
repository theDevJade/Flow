val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    id("io.ktor.plugin") version "3.3.0"
    application
}

group = "com.thedevjade.flow.webserver"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

kotlin {
    jvmToolchain(23)
}

// Task to run WebSocket server
tasks.register<JavaExec>("runWebSocket") {
    group = "application"
    description = "Run the WebSocket server"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.thedevjade.flow.webserver.WebSocketMainKt"
}

// Task to build and test webserver module
tasks.register("testBuild") {
    group = "verification"
    description = "Build webserver module without running tests"
    dependsOn("build")
}

dependencies {
    implementation(project(":flow"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-webjars")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.webjars:jquery:3.2.1")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // SQLite database dependencies
    implementation("org.jetbrains.exposed:exposed-core:0.53.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.53.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.53.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.53.0")
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation(project(":common"))
}

tasks.build {
    dependsOn("buildFatJar")
}

