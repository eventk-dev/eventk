import java.net.URI

plugins {
    kotlin("jvm")
    id("maven-publish")
}

group = "dev.eventk"

kotlin {
    jvmToolchain(17)
    explicitApi()
}

java {
    withSourcesJar()
}

dependencies {
    api(project(":event-store:api"))
    api(project(":hex-arch-ports"))
    api(project(":hex-arch-adapters-common"))

    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.core)

    compileOnly(libs.spring6.context)
    compileOnly(libs.spring6.beans)
    compileOnly(libs.spring6.tx)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "hex-arch-adapters-spring6"
            from(components["java"])
        }
    }
    repositories {
        // from https://docs.github.com/en/actions/publishing-packages/publishing-java-packages-with-gradle#publishing-packages-to-github-packages
        maven {
            url = URI("https://maven.pkg.github.com/eventk-dev/eventk")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
