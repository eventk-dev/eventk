dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "car-management-hexagonal-arch"

include(":domain")
include(":spring-webflux-jpa-application")
