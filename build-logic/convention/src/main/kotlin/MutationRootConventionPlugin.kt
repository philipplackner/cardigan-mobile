import com.plcoding.cardiganapp.convention.MUTATION_CONVENTION_PLUGIN_ID
import com.plcoding.cardiganapp.convention.MUTATION_GROUP
import com.plcoding.cardiganapp.convention.MUTATION_NEEDS_FULL_TASK_NAME
import com.plcoding.cardiganapp.convention.MUTATION_REPORT_RESET_TASK
import com.plcoding.cardiganapp.convention.MUTATION_REPORT_TASK
import com.plcoding.cardiganapp.convention.MutationReportTask
import com.plcoding.cardiganapp.convention.MutationSettings
import com.plcoding.cardiganapp.convention.PITEST_TASK
import com.plcoding.cardiganapp.convention.PitestTask
import com.plcoding.cardiganapp.convention.isMutationReportRequested
import com.plcoding.cardiganapp.convention.isMutationRequested
import com.plcoding.cardiganapp.convention.isMutationTaskName
import com.plcoding.cardiganapp.convention.library
import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.parseMutationModules
import com.plcoding.cardiganapp.convention.parseMutationSettings
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * Root plugin for `mutationReport`. Runs PIT for the modules selected with `mutation.modules`
 * (default: every module with the mutation convention plugin) and merges their results.
 */
class MutationRootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(this == rootProject) { "$MUTATION_ROOT_PLUGIN must be applied to the root project" }

            val mutationRequested = isMutationRequested
            val reportRequested = isMutationReportRequested
            val settings = if (mutationRequested) parseMutationSettings() else MutationSettings.DEFAULT
            val requestedModules = if (reportRequested) parseMutationModules() else null

            val reportDir = layout.buildDirectory.dir("reports/pitest")

            val reset =
                tasks.register<Delete>(MUTATION_REPORT_RESET_TASK) {
                    description = "Deletes the combined mutation report before PIT runs."
                    delete(reportDir)
                }

            val selectedPaths = mutableListOf<String>()
            val selectedReportDirs = mutableListOf<String>()
            val report =
                tasks.register<MutationReportTask>(MUTATION_REPORT_TASK) {
                    group = MUTATION_GROUP
                    description =
                        "Runs PIT on the host tests of the selected modules and writes a combined HTML report."
                    doNotTrackState("Every run analyzes all mutants.")
                    dependsOn(reset)
                    aggregatorClasspath.from(
                        configurations.detachedConfiguration(
                            dependencies.create(libs.library("pitest-aggregator").get()),
                        ),
                    )
                    modulePaths.set(provider { selectedPaths.toList() })
                    moduleReportDirs.set(provider { selectedReportDirs.toList() })
                    outputDir.set(reportDir)
                    htmlReportPath.set(
                        reportDir.map {
                            it
                                .file("index.html")
                                .asFile
                                .relativeTo(rootDir)
                                .path
                        },
                    )
                    settings.minScore?.let { minScore.set(it) }
                }

            gradle.projectsEvaluated {
                if (!reportRequested) return@projectsEvaluated
                val optedIn =
                    subprojects
                        .filter { it.pluginManager.hasPlugin(MUTATION_CONVENTION_PLUGIN_ID) }
                        .sortedBy { it.path }

                val selected =
                    if (requestedModules == null) {
                        optedIn
                    } else {
                        val byPath = optedIn.associateBy { it.path }
                        val unknown = requestedModules.filterNot { it in byPath }
                        if (unknown.isNotEmpty()) {
                            throw GradleException(
                                "mutation.modules contains modules that can't be mutation tested: " +
                                    "${unknown.joinToString()}.\n" +
                                    "Valid modules:\n" + optedIn.joinToString("\n") { "  ${it.path}" },
                            )
                        }
                        requestedModules.map { byPath.getValue(it) }
                    }

                selected.forEach { module ->
                    val pitest = module.tasks.named<PitestTask>(PITEST_TASK)
                    pitest.configure { mustRunAfter(reset) }
                    selectedPaths += module.path
                    selectedReportDirs +=
                        module.layout.buildDirectory
                            .dir("reports/pitest")
                            .get()
                            .asFile.absolutePath
                    report.configure {
                        dependsOn(pitest)
                        sourceDirs.from(pitest.map { it.sourceDirs })
                        compiledCodeDirs.from(pitest.map { it.mutableCodePaths })
                    }
                }
            }

            if (!mutationRequested) {
                gradle.taskGraph.whenReady {
                    if (allTasks.any { isMutationTaskName(it.name) }) {
                        throw GradleException(MUTATION_NEEDS_FULL_TASK_NAME)
                    }
                }
            }
        }
    }

    private companion object {
        const val MUTATION_ROOT_PLUGIN = "com.plcoding.cardiganapp.convention.mutation.root"
    }
}
