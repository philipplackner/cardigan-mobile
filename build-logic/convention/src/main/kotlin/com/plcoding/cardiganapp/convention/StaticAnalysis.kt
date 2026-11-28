package com.plcoding.cardiganapp.convention

import org.gradle.api.Project
import java.io.File

internal const val DETEKT_CONVENTION_PLUGIN_ID = "com.plcoding.cardiganapp.convention.detekt"
internal const val DETEKT_TASK = "detekt"
internal const val DETEKT_CONFIG_PATH = "config/detekt/detekt.yml"
internal const val KONSIST_TASK = "konsist"
internal const val KONSIST_TEST_TASK = "konsistTest"
internal const val TOOLING_PROJECT = ":tooling"
internal const val STATIC_ANALYSIS_TASK = "staticAnalysis"
internal const val STATIC_ANALYSIS_VERIFY_TASK = "staticAnalysisVerify"
internal const val STATIC_ANALYSIS_GROUP = "verification"

/** Type-resolved detekt tasks of the Android compilations of a KMP library (AGP 9 KMP library plugin). */
internal val KMP_TYPE_RESOLVED_DETEKT_TASKS =
    listOf("detektMainAndroid", "detektHostTestAndroid", "detektDeviceTestAndroid")

/** Type-resolved detekt tasks of the debug variant of an Android application. */
internal val ANDROID_APP_TYPE_RESOLVED_DETEKT_TASKS = listOf("detektDebug", "detektDebugUnitTest")

/** Type-resolved detekt tasks of a Kotlin/JVM module. */
internal val JVM_TYPE_RESOLVED_DETEKT_TASKS = listOf("detektMain", "detektTest")

/** Source sets that only iOS targets compile, analyzed without type resolution by the plain `detekt` task. */
internal fun isIosOnlySourceSet(name: String): Boolean = IOS_ONLY_SOURCE_SET_PREFIXES.any { name.startsWith(it) }

private val IOS_ONLY_SOURCE_SET_PREFIXES = listOf("ios", "apple", "native")

/** True when `staticAnalysis` is requested by its full name. */
internal val Project.isStaticAnalysisRequested: Boolean
    get() = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == STATIC_ANALYSIS_TASK }

/** The detekt tasks that a module's `detekt` task runs, by the module's kind. */
internal fun Project.detektAnalysisTaskNames(): List<String> =
    when {
        pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") -> KMP_TYPE_RESOLVED_DETEKT_TASKS + DETEKT_TASK
        pluginManager.hasPlugin("com.android.application") -> ANDROID_APP_TYPE_RESOLVED_DETEKT_TASKS
        pluginManager.hasPlugin("org.jetbrains.kotlin.jvm") -> JVM_TYPE_RESOLVED_DETEKT_TASKS
        else -> listOf(DETEKT_TASK)
    }

/** Where the detekt plugin writes the SARIF report of [taskName] inside a module's [buildDir]. */
internal fun detektSarifReport(
    buildDir: File,
    taskName: String,
): File {
    val reportName =
        if (taskName == DETEKT_TASK) {
            DETEKT_TASK
        } else {
            taskName.removePrefix(DETEKT_TASK).replaceFirstChar { it.lowercase() }
        }
    return File(buildDir, "reports/detekt/$reportName.sarif")
}
