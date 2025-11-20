import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.setupCompiler() {
    tasks.withType<KotlinCompile>()
        .configureEach {
            compilerOptions {
                progressiveMode.set(true)
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
}
