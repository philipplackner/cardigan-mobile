package com.plcoding.cardiganapp.convention

/**
 * What PIT leaves out on top of the shared [CodeExclusions]. Kover doesn't read these, so the
 * mutation report excludes more code than the coverage report.
 */
internal object MutationExclusions {

    /** PIT class globs for compiler-generated code, inline copies, design tokens and navigation wiring. */
    val classGlobs = listOf(
        "*\$WhenMappings",
        "*\$\$inlined\$*",
        "*\$\$serializer",
        "*.designsystem.theme.*Colors*",
        "*.designsystem.theme.App*",
        "*.designsystem.theme.ShadowsKt",
        "*.designsystem.theme.ShadowsKt\$*",
        "*.designsystem.theme.TextStylesKt",
        "*.designsystem.theme.TextStylesKt\$*",
        "*.designsystem.theme.TypographyKt",
        "*.designsystem.theme.TypographyKt\$*",
        "*.navigation.*Route",
        "*.navigation.*Route\$*",
        "*.navigation.*GraphKt",
        "*.navigation.*GraphKt\$*",
        "*.navigation.*NavigationKt",
        "*.navigation.*NavigationKt\$*",
        "*.navigation.*NavHostKt",
        "*.navigation.*NavHostKt\$*",
    )

    /** Annotations PIT's built-in FANN filter skips. Setting the list replaces PIT's defaults, so they are repeated. */
    val annotations = listOf("Composable", "Generated", "DoNotMutate", "CoverageIgnore")

    val avoidCallsTo = listOf("kotlin.jvm.internal", "kotlin.ResultKt", "co.touchlab.kermit")

    /** The `--features` value: FANN with [annotations], plus the Compose UI filter from `build-logic:pitest-filters`. */
    val features: String
        get() = "+FANN(${annotations.joinToString(" ") { "annotation[$it]" }}),+FCOMPOSEUI"
}
