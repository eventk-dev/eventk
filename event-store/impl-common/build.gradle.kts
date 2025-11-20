plugins {
    id("standard-kotlin-multiplatform")
}

group = "dev.eventk"

kotlin {
    setupPlatforms()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":event-store:api"))
            }
        }
    }
}

setupPublishing("event-store-impl-common")
