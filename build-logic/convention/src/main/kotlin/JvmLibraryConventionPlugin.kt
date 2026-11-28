import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Plain Kotlin/JVM module on the same JDK 17 toolchain as build-logic, with tests on the JUnit Platform. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(JVM_VERSION)
            }

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }

    private companion object {
        const val JVM_VERSION = 17
    }
}
