dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    plugins {
    }
}

rootProject.name = "eventk-parent"

includeBuild("build-logic")

include(":event-store:api")
include(":event-store:impl-common")

include(":event-store:impl-memory")
include(":event-store:impl-fs")
include(":event-store:impl-postgresql")

include(":event-store:test-harness")
include(":event-store:test-harness-model")

include(":hex-arch-ports")
include(":hex-arch-adapters-common")
include(":hex-arch-adapters-ktor")
include(":hex-arch-adapters-spring6")
