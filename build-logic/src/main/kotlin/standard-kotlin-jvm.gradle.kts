import org.gradle.kotlin.dsl.kotlin

plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
    id("com.vanniktech.maven.publish")
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
