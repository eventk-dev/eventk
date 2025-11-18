repositories {
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation(kotlin("serialization", version = "1.9.22"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.32.0")
}
