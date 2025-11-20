import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    id("com.vanniktech.maven.publish")
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        progressiveMode.set(true)
    }
}

spotless {
    kotlin {
        target("**/src/**/*.kt")
        ktlint("1.7.1")
            .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
    }
    kotlinGradle {
        target("**/build.gradle.kts", "**/settings.gradle.kts")
        ktlint("1.7.1")
            .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
    }
}
