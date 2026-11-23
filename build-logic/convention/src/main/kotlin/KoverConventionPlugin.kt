import com.plcoding.cardiganapp.convention.applyCoverageExclusions
import com.plcoding.cardiganapp.convention.isCoverageRequested
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Opts a module into coverage reports. Kover is applied only when a coverage task is requested,
 * so normal test runs have no Kover tasks, agent or binary reports at all.
 */
class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Disabling instrumentation isn't enough: Kover still adds koverFindJar to every test task.
            if (!isCoverageRequested) return

            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<KoverProjectExtension> {
                reports {
                    filters {
                        applyCoverageExclusions()
                    }
                    total {
                        verify {
                            onCheck.set(false)
                        }
                    }
                }
            }
        }
    }
}
