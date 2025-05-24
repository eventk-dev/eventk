plugins {
    standardMultiplatformModule()
}

group = "dev.eskt"

kotlin {
    setupPlatforms(jvm = true, native = false, node = false, ios = false)

    sourceSets {
        commonMain {
            dependencies {
                api(project(":event-store:api"))
                implementation(project(":event-store:impl-common"))
                implementation(libs.kotlinx.serialization.json)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":event-store:test-harness"))
            }
        }
        jvmMain {
            dependencies {
                compileOnly(libs.postgresql.jdbc)
                compileOnly(libs.postgresql.r2dbc)
                implementation(libs.kotlinx.coroutines.reactive)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.hikaricp)
            }
        }
    }
}

setupCompiler()

setupPublishing("event-store-impl-postgresql")
