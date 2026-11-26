package com.plcoding.cardiganapp.convention

/**
 * What PIT leaves out on top of the shared [CodeExclusions]. Kover doesn't read these, so the
 * mutation report excludes more code than the coverage report.
 */
internal object MutationExclusions {

    /** PIT class globs for compiler-generated code and inline copies. */
    val classGlobs = listOf(
        "*\$WhenMappings",
        "*\$\$inlined\$*",
        "*\$\$serializer",
    )

    /** Annotations PIT's built-in FANN filter skips. Setting the list replaces PIT's defaults, so they are repeated. */
    val annotations = listOf("Composable", "Generated", "DoNotMutate", "CoverageIgnore")

    val avoidCallsTo = listOf("kotlin.jvm.internal", "kotlin.ResultKt", "co.touchlab.kermit")

    /** The `--features` value: FANN with [annotations], plus the Compose UI filter from `build-logic:pitest-filters`. */
    val features: String
        get() = "+FANN(${annotations.joinToString(" ") { "annotation[$it]" }}),+FCOMPOSEUI"
}
