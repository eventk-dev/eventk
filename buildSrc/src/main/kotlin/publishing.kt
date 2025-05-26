import com.vanniktech.maven.publish.SonatypeHost
import gradle.kotlin.dsl.accessors._a752b004c685f16d54fd070b1334a77d.mavenPublishing
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
