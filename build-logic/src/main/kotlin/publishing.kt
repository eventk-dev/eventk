import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import gradle.kotlin.dsl.accessors._05f7fd8afcbc155a1402654be1db5c57.mavenPublishing
import org.gradle.api.Project

fun MavenPublishBaseExtension.publishingConfig() {
    pom {
        name.set("eventk")
        description.set("EventK is a library for building event-sourcing applications in Kotlin")
        url.set("https://eventk.dev")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            connection.set("https://github.com/eventk-dev/eventk.git")
            url.set("https://github.com/eventk-dev/eventk")
        }
        developers {
            developer {
                name.set("Bruno Medeiros")
                email.set("brinojcm@gmail.com")
            }
        }
    }
}

fun Project.setupPublishing(mavenArtifactId: String) {
    mavenPublishing {
        publishingConfig()

        val version = project.version.toString()
        coordinates(
            groupId = "dev.eventk",
            artifactId = mavenArtifactId,
            version = version
        )
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        if (!version.endsWith("-SNAPSHOT")) {
            signAllPublications()
        }
    }
}
