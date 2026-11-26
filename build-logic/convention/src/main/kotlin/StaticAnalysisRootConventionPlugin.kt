import com.plcoding.cardiganapp.convention.DETEKT_CONVENTION_PLUGIN_ID
import com.plcoding.cardiganapp.convention.DETEKT_TASK
import com.plcoding.cardiganapp.convention.JVM_TYPE_RESOLVED_DETEKT_TASKS
import com.plcoding.cardiganapp.convention.KONSIST_TASK
import com.plcoding.cardiganapp.convention.KONSIST_TEST_TASK
import com.plcoding.cardiganapp.convention.STATIC_ANALYSIS_GROUP
import com.plcoding.cardiganapp.convention.STATIC_ANALYSIS_TASK
import com.plcoding.cardiganapp.convention.STATIC_ANALYSIS_VERIFY_TASK
import com.plcoding.cardiganapp.convention.StaticAnalysisVerifyTask
import com.plcoding.cardiganapp.convention.TOOLING_PROJECT
import com.plcoding.cardiganapp.convention.detektAnalysisTaskNames
import com.plcoding.cardiganapp.convention.detektSarifReport
import com.plcoding.cardiganapp.convention.isStaticAnalysisRequested
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.report.ReportMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Root plugin for `detekt`, `konsist` and `staticAnalysis`. `detekt` runs every module with the
 * detekt convention plugin plus build-logic and merges their SARIF reports. `staticAnalysis` runs
 * both tools to the end and fails afterwards if either found a problem.
 */
class StaticAnalysisRootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(this == rootProject) { "$STATIC_ANALYSIS_ROOT_PLUGIN must be applied to the root project" }

            val staticAnalysisRequested = isStaticAnalysisRequested
            val rootDetektRequested = staticAnalysisRequested ||
                gradle.startParameter.taskNames.any { it == DETEKT_TASK || it == ":$DETEKT_TASK" }

            val buildLogicBuildDir = rootDir.resolve("build-logic/convention/build")
            val sarifReportFiles = JVM_TYPE_RESOLVED_DETEKT_TASKS
                .map { detektSarifReport(buildLogicBuildDir, it) }
                .toMutableList()
            val moduleDetektTaskPaths = mutableListOf<String>()

            val merge = tasks.register<ReportMergeTask>(DETEKT_MERGE_TASK) {
                description = "Merges the SARIF reports of all detekt tasks into one file."
                input.from(provider { sarifReportFiles.toList() })
                output.set(layout.buildDirectory.file("reports/detekt/merged.sarif"))
            }

            val detekt = tasks.register(DETEKT_TASK) {
                group = STATIC_ANALYSIS_GROUP
                description = "Runs detekt on every opted-in module and build-logic."
                dependsOn(gradle.includedBuild(BUILD_LOGIC).task(":convention:$DETEKT_TASK"))
                finalizedBy(merge)
            }

            val konsist = tasks.register(KONSIST_TASK) {
                group = STATIC_ANALYSIS_GROUP
                description = "Runs the Konsist architecture checks."
                dependsOn("$TOOLING_PROJECT:$KONSIST_TEST_TASK")
            }

            val verify = tasks.register<StaticAnalysisVerifyTask>(STATIC_ANALYSIS_VERIFY_TASK) {
                description = "Fails when detekt reported a finding or a Konsist check failed."
                sarifReports.from(provider { sarifReportFiles.toList() })
                konsistResultsDir.set(
                    rootDir.resolve("tooling/build/test-results/$KONSIST_TEST_TASK"),
                )
                mustRunAfter(merge)
            }

            tasks.register(STATIC_ANALYSIS_TASK) {
                group = STATIC_ANALYSIS_GROUP
                description = "Runs detekt and Konsist, then fails if either found a problem."
                dependsOn(detekt, konsist)
                finalizedBy(verify)
            }

            gradle.projectsEvaluated {
                subprojects
                    .filter { it.pluginManager.hasPlugin(DETEKT_CONVENTION_PLUGIN_ID) }
                    .sortedBy { it.path }
                    .forEach { module ->
                        detekt.configure { dependsOn("${module.path}:$DETEKT_TASK") }
                        val buildDir = module.layout.buildDirectory.get().asFile
                        module.detektAnalysisTaskNames().forEach { taskName ->
                            sarifReportFiles += detektSarifReport(buildDir, taskName)
                            moduleDetektTaskPaths += "${module.path}:$taskName"
                        }
                        if (staticAnalysisRequested) {
                            module.tasks.withType<Detekt>().configureEach { ignoreFailures.set(true) }
                        }
                    }

                val modulePaths = moduleDetektTaskPaths.toList()
                merge.configure { mustRunAfter(modulePaths) }
                verify.configure { mustRunAfter(modulePaths, "$TOOLING_PROJECT:$KONSIST_TEST_TASK") }
                if (rootDetektRequested) {
                    // Finalizing each module task keeps the merged report even when one of them fails.
                    modulePaths.forEach { path ->
                        project(path.substringBeforeLast(':')).tasks.named(path.substringAfterLast(':')) {
                            finalizedBy(merge)
                        }
                    }
                }

                if (staticAnalysisRequested) {
                    project(TOOLING_PROJECT).tasks.named<Test>(KONSIST_TEST_TASK) { ignoreFailures = true }
                }
            }
        }
    }

    private companion object {
        const val STATIC_ANALYSIS_ROOT_PLUGIN = "com.plcoding.cardiganapp.convention.static.analysis.root"
        const val DETEKT_MERGE_TASK = "detektReportMerge"
        const val BUILD_LOGIC = "build-logic"
    }
}
