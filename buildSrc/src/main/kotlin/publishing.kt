import gradle.kotlin.dsl.accessors._ce8aa6c9428f2097fde07b3227749f5c.publishing
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.withType
import java.net.URI

fun Project.setupPublishing(mavenArtifactId: String) {
    publishing {
        publications.withType<MavenPublication> {
            artifactId = if (name == "kotlinMultiplatform") {
                mavenArtifactId
            } else {
                "$mavenArtifactId-${name.lowercase()}"
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
}
