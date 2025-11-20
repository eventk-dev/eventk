plugins {
    id("standard-kotlin-multiplatform")
    id("standard-serialization")
}

group = "dev.eventk"

kotlin {
    setupPlatforms()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":event-store:api"))
                api(libs.uuid)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.cbor)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
