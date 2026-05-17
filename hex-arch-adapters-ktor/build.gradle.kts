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
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(project(":event-store:impl-memory"))
            }
        }
    }
}

setupPublishing("hex-arch-adapters-ktor")
