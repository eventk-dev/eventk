import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("multiplatform")
    id("com.diffplug.spotless")
    id("com.vanniktech.maven.publish")
}

kotlin {
    explicitApi()
}

spotless {
    kotlin {
        target("**/src/**/*.kt")
        ktlint("1.5.0")
            .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
    }
    kotlinGradle {
        target("**/build.gradle.kts", "**/settings.gradle.kts")
        ktlint("1.5.0")
            .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
    }
}
