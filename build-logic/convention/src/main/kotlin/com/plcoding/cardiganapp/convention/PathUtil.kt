package com.plcoding.cardiganapp.convention

import org.gradle.api.Project
import java.util.Locale

private const val PACKAGE_ROOT = "com.plcoding.cardiganapp"

/**
 * Derives the package / namespace from the module path, e.g.
 * `:feature:boards:data` → `com.plcoding.cardiganapp.feature.boards.data`.
 */
fun Project.pathToPackageName(): String {
    val relativePackageName = path
        .replace(':', '.')
        .replace('-', '_')
        .lowercase()
    return "$PACKAGE_ROOT$relativePackageName"
}

/**
 * Derives an iOS framework name from the module path, e.g. `:composeApp` → `ComposeApp`.
 */
fun Project.pathToFrameworkName(): String {
    val parts = path.split(":", "-", "_", " ")
    return parts.joinToString("") { part ->
        part.replaceFirstChar { it.titlecase(Locale.ROOT) }
    }
}
