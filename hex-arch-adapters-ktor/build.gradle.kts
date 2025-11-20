plugins {
    id("standard-kotlin-multiplatform")
}

group = "dev.eventk"

kotlin {
    setupPlatforms(ios = false, node = false)

    sourceSets {
        commonMain {
            dependencies {
                api(project(":event-store:api"))
                api(project(":hex-arch-ports"))
                api(project(":hex-arch-adapters-common"))

                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

setupPublishing("hex-arch-adapters-ktor")
