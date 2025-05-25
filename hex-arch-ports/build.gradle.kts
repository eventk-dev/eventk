plugins {
    standardMultiplatformModule()
}

group = "dev.eventk"

kotlin {
    setupPlatforms(ios = false)

    sourceSets {
        commonMain {
            dependencies {
                api(project(":event-store:api"))
            }
        }
    }
}

setupPublishing("hex-arch-ports")
