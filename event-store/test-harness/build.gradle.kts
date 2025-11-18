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
                api(project(":event-store:impl-common"))
                api(project(":event-store:test-harness-model"))
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        jsMain {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

setupCompiler()
