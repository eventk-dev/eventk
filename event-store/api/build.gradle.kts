plugins {
    standardMultiplatformModule()
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

setupCompiler()

setupPublishing("event-store-api")
