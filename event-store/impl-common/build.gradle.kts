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

setupCompiler()

setupPublishing("event-store-impl-common")
