package com.plcoding.cardiganapp.convention

import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig
import org.gradle.api.Project

internal const val KOVER_CONVENTION_PLUGIN_ID = "com.plcoding.cardiganapp.convention.kover"

internal const val COVERAGE_NEEDS_INSTRUMENTATION =
    "Coverage needs instrumentation. Run with the full task name, e.g. ./gradlew coverageReport."

private val coverageTaskNames = setOf("coverageReport", "coverageDiff")

/**
 * True when the requested task names ask for coverage: `coverageReport`, `coverageDiff` or any
 * `kover*` task, matched on the last path segment. Abbreviated names such as `cR` don't match.
 */
internal val Project.isCoverageRequested: Boolean
    get() = gradle.startParameter.taskNames.any { isCoverageTaskName(it.substringAfterLast(':')) }

internal fun isCoverageTaskName(name: String): Boolean = name in coverageTaskNames || name.startsWith("kover")

/**
 * The exclusions shared by the module reports and the merged root report.
 * They match by annotation and naming pattern so new code that follows the project layout is
 * excluded without a build change.
 */
internal fun KoverReportFiltersConfig.applyCoverageExclusions() {
    excludes {
        annotatedBy("androidx.compose.runtime.Composable")
        classes(CodeExclusions.classes)
        packages(CodeExclusions.packages)
        // Kover only sees the direct library superclass, not android.app.Activity further up.
        // Nested classes of an activity are caught by the *Activity$* name pattern instead.
        inheritedFrom("android.app.Activity", "androidx.activity.ComponentActivity")
    }
}
