plugins {
    kotlin("multiplatform")
}

group = "dev.eventk.example"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(17)

    jvm()
    linuxX64()
//    linuxArm64()
    macosX64()
    macosArm64()

    sourceSets {
        commonMain {
            dependencies {
                api("dev.eventk:event-store-api:local-SNAPSHOT")
                api("dev.eventk:hex-arch-ports:local-SNAPSHOT")
                api("dev.eventk:test-harness-model:local-SNAPSHOT")
                api("com.benasher44:uuid:0.8.2")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
