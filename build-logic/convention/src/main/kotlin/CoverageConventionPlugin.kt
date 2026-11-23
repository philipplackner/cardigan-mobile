import com.plcoding.cardiganapp.convention.COVERAGE_NEEDS_INSTRUMENTATION
import com.plcoding.cardiganapp.convention.CoverageSummaryTask
import com.plcoding.cardiganapp.convention.CoverageDiffTask
import com.plcoding.cardiganapp.convention.KOVER_CONVENTION_PLUGIN_ID
import com.plcoding.cardiganapp.convention.applyCoverageExclusions
import com.plcoding.cardiganapp.convention.isCoverageRequested
import com.plcoding.cardiganapp.convention.isCoverageTaskName
import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.versionString
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Root plugin for `coverageReport` and `coverageDiff`. Merges the Kover data of the modules
 * selected with `coverage.modules` (default: every module with the kover convention plugin).
 */
class CoverageConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(this == rootProject) { "$KOVER_COVERAGE_PLUGIN must be applied to the root project" }

            // Kover is always applied here so abbreviated Kover task names resolve and hit the guard below.
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            val coverageRequested = isCoverageRequested
            val requestedModules = parseModules(providers.gradleProperty("coverage.modules").orNull)
            val minLine = if (coverageRequested) parseThreshold("coverage.minLine") else null
            val minBranch = if (coverageRequested) parseThreshold("coverage.minBranch") else null
            val compareBranch = providers.gradleProperty("coverage.compareBranch").orNull
                ?.trim()?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_COMPARE_BRANCH

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

            val selectedModulePaths = mutableListOf<String>()
            val selectedSourceRoots = mutableListOf<String>()

            gradle.projectsEvaluated {
                if (!coverageRequested) return@projectsEvaluated
                val optedIn = subprojects
                    .filter { it.pluginManager.hasPlugin(KOVER_CONVENTION_PLUGIN_ID) }
                    .sortedBy { it.path }

                val selected = if (requestedModules.isEmpty()) {
                    optedIn
                } else {
                    val byPath = optedIn.associateBy { it.path }
                    val unknown = requestedModules.filterNot { it in byPath }
                    if (unknown.isNotEmpty()) {
                        throw GradleException(
                            "coverage.modules contains modules that can't be reported: ${unknown.joinToString()}.\n" +
                                "Valid modules:\n" + optedIn.joinToString("\n") { "  ${it.path}" },
                        )
                    }
                    requestedModules.map { byPath.getValue(it) }
                }
                selected.forEach { module ->
                    selectedModulePaths += module.path
                    selectedSourceRoots += SOURCE_SETS.map { module.file("src/$it/kotlin") }
                        .filter { it.isDirectory }
                        .map { it.relativeTo(rootDir).path }
                    dependencies.add("kover", dependencies.project(mapOf("path" to module.path)))
                }
            }

            val reportsDir = layout.buildDirectory.dir("reports/kover")
            val htmlIndex = reportsDir.map { it.file("html/index.html") }

            tasks.register<CoverageSummaryTask>("coverageReport") {
                group = COVERAGE_GROUP
                description = "Runs the host tests of the selected modules and writes a merged coverage report."
                dependsOn("koverXmlReport", "koverHtmlReport")
                xmlReport.set(reportsDir.map { it.file("report.xml") })
                moduleCount.set(provider { selectedModulePaths.size })
                htmlReportPath.set(htmlIndex.map { it.asFile.relativeTo(rootDir).path })
                minLine?.let { this.minLine.set(it) }
                minBranch?.let { this.minBranch.set(it) }
            }

            tasks.register<CoverageDiffTask>("coverageDiff") {
                group = COVERAGE_GROUP
                description = "Shows coverage of the lines changed against coverage.compareBranch."
                dependsOn("koverXmlReport")
                xmlReport.set(reportsDir.map { it.file("report.xml") })
                diffCoverVersion.set(libs.versionString("diffCover"))
                excludeNewer.set(libs.versionString("diffCoverExcludeNewer"))
                this.compareBranch.set(compareBranch)
                sourceRoots.set(provider { selectedSourceRoots.toList() })
                repositoryDir.set(layout.projectDirectory)
                htmlReport.set(layout.buildDirectory.file("reports/diff-coverage/index.html"))
            }

            if (!coverageRequested) {
                gradle.taskGraph.whenReady {
                    if (allTasks.any { isCoverageTaskName(it.name) }) {
                        throw GradleException(COVERAGE_NEEDS_INSTRUMENTATION)
                    }
                }
            }
        }
    }

    private fun parseModules(value: String?): List<String> =
        value.orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    private fun Project.parseThreshold(property: String): Double? {
        val raw = providers.gradleProperty(property).orNull?.trim() ?: return null
        val value = raw.toDoubleOrNull()
        if (value == null || value.isNaN() || value < 0.0 || value > 100.0) {
            throw GradleException("$property must be a number from 0 to 100, got '$raw'.")
        }
        return value
    }

    private companion object {
        const val KOVER_COVERAGE_PLUGIN = "com.plcoding.cardiganapp.convention.coverage"
        const val COVERAGE_GROUP = "verification"
        const val DEFAULT_COMPARE_BRANCH = "origin/master"
        val SOURCE_SETS = listOf("commonMain", "androidMain")
    }
}
