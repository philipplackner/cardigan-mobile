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

    /**
     * `ClassGlob#methodGlob` entries for members only the Compose resources runtime or a real
     * client configuration can run. The class glob matches the binary class name.
     */
    val namedMembers = listOf(
        "*.UiText#asStringAsync*",
        "*.UiText\$*#asStringAsync*",
        "*.HttpClientFactory#create\$lambda*",
    )

    /**
     * The `--features` value: FANN with [annotations], plus the filters from `build-logic:pitest-filters`
     * for Compose UI code, inline functions, navigation helpers and [namedMembers].
     */
    val features: String
        get() = listOf(
            "+FANN(${annotations.joinToString(" ") { "annotation[$it]" }})",
            "+FCOMPOSEUI",
            "+FKOTLININLINE",
            "+FNAVRECEIVER",
            "+FNAMEDEXCLUDE(${namedMembers.joinToString(" ") { "member[$it]" }})",
        ).joinToString(",")
}
