plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "com.thedevjade.io"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
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