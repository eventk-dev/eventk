import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost
import kotlin.text.endsWith

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

group = "dev.eventk"

kotlin {
    jvmToolchain(17)
    explicitApi()
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

mavenPublishing {
    val version = project.version.toString()
    coordinates(
        groupId = "dev.eventk",
        artifactId = "hex-arch-adapters-spring6",
        version = version,
    )
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if (!version.endsWith("-SNAPSHOT")) {
        signAllPublications()
    }
}
