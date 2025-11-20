import com.vanniktech.maven.publish.SonatypeHost
import gradle.kotlin.dsl.accessors._05f7fd8afcbc155a1402654be1db5c57.mavenPublishing
import org.gradle.api.Project

fun Project.setupPublishing(mavenArtifactId: String) {
    mavenPublishing {
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
