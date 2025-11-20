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
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    implementation(kotlin("serialization", version = "2.2.21"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.32.0")
}
