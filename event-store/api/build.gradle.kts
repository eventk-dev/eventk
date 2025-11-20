plugins {
    id("standard-kotlin-multiplatform")
}

group = "dev.eventk"

kotlin {
    setupPlatforms()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

setupPublishing("event-store-api")
