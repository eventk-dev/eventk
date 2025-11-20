plugins {
    id("standard-kotlin-multiplatform")
    id("standard-serialization")
}

group = "dev.eventk"

kotlin {
    setupPlatforms(ios = false)

    sourceSets {
        commonMain {
            dependencies {
                api(project(":event-store:api"))
                implementation(project(":event-store:impl-common"))
                implementation(libs.okio.core)
                implementation(libs.kotlinx.serialization.cbor) // used for default metadata serializer only
                implementation(libs.kotlinx.serialization.protobuf)
                implementation(libs.kotlinx.atomicfu)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":event-store:test-harness"))
            }
        }
        jsMain {
            dependencies {
                implementation(libs.okio.node)
            }
        }
    }
}

setupPublishing("event-store-impl-fs")
