plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    `maven-publish`
}

group = "com.thedevjade.io"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("FlowLang")
                description.set("A Kotlin-based scripting language converted from C# Skribe")
                url.set("https://github.com/thedevjade/Flow")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("thedevjade")
                        name.set("The Dev Jade")
                    }
                }
            }
        }
    }
}