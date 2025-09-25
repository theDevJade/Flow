dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"

}

include(":app")
include(":plugin")
include(":webserver")
include(":common")
include(":flow")
include(":lang")

rootProject.name = "Flow"

