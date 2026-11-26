package com.plcoding.cardiganapp.convention

/**
 * Name and package patterns for code that coverage and mutation reports leave out: previews,
 * generated code, DI wiring, debug fixtures, androidMain `actual`s, design tokens and navigation
 * routes and graphs. Kover and PIT both read these lists, so the two reports exclude the same code.
 */
internal object CodeExclusions {

    val classes = listOf(
        "*ComposableSingletons*",
        "*PreviewKt",
        "*PreviewKt\$*",
        "*PreviewsKt",
        "*PreviewsKt\$*",
        "*PreviewState",
        "*PreviewState\$*",
        "*.BuildKonfig",
        "*.BuildKonfig\$*",
        "*Debug*Overlay*",
        "*_androidKt",
        "*_androidKt\$*",
        "*.Android*",
        "*Activity",
        "*Activity\$*",
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

    val packages = listOf(
        "*.generated.*",
        "*.di",
        "*.di.*",
        "*.fixture",
        "*.fixture.*",
    )

    /** [classes] and [packages] as PIT class globs, which match the fully qualified class name. */
    val pitClassGlobs: List<String>
        get() = (classes + packages.map { if (it.endsWith(".*")) it else "$it.*" }).distinct()
}
