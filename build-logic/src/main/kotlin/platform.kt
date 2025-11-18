import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.setupPlatforms(jvm: Boolean = true, native: Boolean = true, node: Boolean = true, ios: Boolean = true) {
    jvmToolchain(17)

    if (jvm) {
        jvm {
            // nothing
        }
    }

    if (node) {
        js(IR) {
            nodejs()
        }
    }

    if (ios) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    if (native) {
        linuxX64()
        linuxArm64()
        macosX64()
        macosArm64()
    }
}
