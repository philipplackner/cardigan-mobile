package com.plcoding.cardiganapp.convention

/**
 * Name and package patterns for code that coverage and mutation reports leave out: previews,
 * generated code, DI wiring, debug fixtures and androidMain `actual`s. Kover and PIT both read
 * these lists, so the two reports exclude the same code.
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
