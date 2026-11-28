package com.plcoding.cardiganapp.convention

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

internal const val MUTATION_CONVENTION_PLUGIN_ID = "com.plcoding.cardiganapp.convention.mutation"
internal const val MUTATION_REPORT_TASK = "mutationReport"
internal const val MUTATION_REPORT_RESET_TASK = "mutationReportReset"
internal const val PITEST_TASK = "pitestAndroidHost"
internal const val PITEST_RUN_LOCK = "pitestRunLock"
internal const val MUTATION_GROUP = "verification"

/** The PIT plugin from `build-logic:pitest-filters`, resolved from the included build. */
internal const val PITEST_FILTERS_DEPENDENCY = "com.plcoding.cardiganapp.buildlogic:pitest-filters"

internal const val MUTATION_NEEDS_FULL_TASK_NAME =
    "Mutation testing needs the full task name, e.g. ./gradlew mutationReport."

private val mutationTaskNames = setOf(MUTATION_REPORT_TASK, PITEST_TASK)

internal fun isMutationTaskName(name: String): Boolean = name in mutationTaskNames || name == MUTATION_REPORT_RESET_TASK

/** True when a mutation task is requested by its full name. Abbreviations such as `mR` don't match. */
internal val Project.isMutationRequested: Boolean
    get() = gradle.startParameter.taskNames.any { it.substringAfterLast(':') in mutationTaskNames }

internal val Project.isMutationReportRequested: Boolean
    get() = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == MUTATION_REPORT_TASK }

internal data class MutationSettings(
    val threads: Int,
    val minScore: Double?,
) {
    companion object {
        val DEFAULT =
            MutationSettings(
                threads = Runtime.getRuntime().availableProcessors(),
                minScore = null,
            )
    }
}

/**
 * Reads `mutation.threads` and `mutation.minScore`. Invalid values fail
 * the build at configuration time, so no test runs.
 */
internal fun Project.parseMutationSettings(): MutationSettings =
    MutationSettings(
        threads = parseThreads(),
        minScore = parseMinScore(),
    )

/**
 * Reads `mutation.modules`. Returns null when the property is left out, which means all modules.
 * An empty value or an empty entry fails.
 */
internal fun Project.parseMutationModules(): List<String>? {
    val raw = providers.gradleProperty("mutation.modules").orNull ?: return null
    val entries = raw.split(',').map { it.trim() }
    if (entries.any { it.isEmpty() }) {
        throw GradleException(
            "mutation.modules must be a comma-separated list of module paths with no empty entries, got '$raw'.",
        )
    }
    return entries.distinct()
}

private fun Project.parseThreads(): Int {
    val raw = providers.gradleProperty("mutation.threads").orNull ?: return MutationSettings.DEFAULT.threads
    val value = raw.trim().toIntOrNull()
    if (value == null || value < 1) {
        throw GradleException("mutation.threads must be a positive whole number, got '$raw'.")
    }
    return value
}

private fun Project.parseMinScore(): Double? {
    val raw = providers.gradleProperty("mutation.minScore").orNull ?: return null
    val value = raw.trim().toDoubleOrNull()
    if (value == null || value.isNaN() || value < 0.0 || value > 100.0) {
        throw GradleException("mutation.minScore must be a number from 0 to 100, got '$raw'.")
    }
    return value
}

/** Shared by all module PIT tasks with one parallel usage, so module runs never overlap. */
abstract class PitestRunLock : BuildService<BuildServiceParameters.None>
