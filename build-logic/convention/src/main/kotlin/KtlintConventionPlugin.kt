import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.versionString
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.configure
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * Opts a project in to `ktlintCheck` and `ktlintFormat`. ktlint runs only when one of those tasks is
 * called: `check` and `build` never run it. On the root project, both tasks also cover build-logic.
 */
class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            val buildDir = layout.buildDirectory.get().asFile
            extensions.configure<KtlintExtension> {
                version.set(libs.versionString("ktlint"))
                outputToConsole.set(true)
                verbose.set(true)
                ignoreFailures.set(false)
                reporters {
                    reporter(ReporterType.SARIF)
                    reporter(ReporterType.CHECKSTYLE)
                }
                filter {
                    exclude { it.file.startsWith(buildDir) }
                }
            }

            // ktlint-gradle hooks its tasks into check per source set, some of them during evaluation.
            afterEvaluate {
                tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
                    setDependsOn(dependsOn.filterNot { it.isKtlintTask() })
                }
            }

            if (this == rootProject) {
                val buildLogic = gradle.includedBuild(BUILD_LOGIC)
                BUILD_LOGIC_PROJECTS.forEach { path ->
                    tasks.named(KTLINT_CHECK_TASK) { dependsOn(buildLogic.task("$path:$KTLINT_CHECK_TASK")) }
                    tasks.named(KTLINT_FORMAT_TASK) { dependsOn(buildLogic.task("$path:$KTLINT_FORMAT_TASK")) }
                }
            }
        }
    }

    private fun Any.isKtlintTask(): Boolean =
        when (this) {
            is NamedDomainObjectProvider<*> -> name.startsWith(KTLINT_PREFIX)
            is Task -> name.startsWith(KTLINT_PREFIX)
            is String -> substringAfterLast(':').startsWith(KTLINT_PREFIX)
            else -> false
        }

    private companion object {
        const val KTLINT_PREFIX = "ktlint"
        const val KTLINT_CHECK_TASK = "ktlintCheck"
        const val KTLINT_FORMAT_TASK = "ktlintFormat"
        const val BUILD_LOGIC = "build-logic"
        val BUILD_LOGIC_PROJECTS = listOf("", ":convention", ":pitest-filters")
    }
}
