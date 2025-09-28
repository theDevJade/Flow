import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    `java-library`
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.gradleup.shadow") version "8.3.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0" // Generates plugin.yml based on the Gradle config
}

group = "com.thedevjade.flow"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

dependencies {
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    compileOnly(project(":common"))
    compileOnly(project(":webserver"))
    compileOnly(project(":flow"))
    implementation(project(":lang"))
    implementation("org.reflections:reflections:0.10.2")

}


val targetJavaVersion = 23
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn(":app:build")
}

bukkitPluginYaml {
    main = "com.thedevjade.flow.flowPlugin.Flow"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    name = "flow"
    authors.add("theDevJade")
    apiVersion = "1.21"

    commands {
        create("flow") {
            description = "Main Flow command"
            usage = "/flow <status|killServer|startServer|reload|database|sessions>"
            permission = "flow.admin"
        }
        create("flowlang") {
            description = "FlowLang script management"
            usage = "/flowlang <load|reload|unload>"
            permission = "flow.admin"
        }
    }
}

