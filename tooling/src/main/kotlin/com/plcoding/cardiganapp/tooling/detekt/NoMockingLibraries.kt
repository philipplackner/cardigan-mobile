package com.plcoding.cardiganapp.tooling.detekt

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Reports imports from `org.mockito` and `io.mockk`, including star and aliased imports.
 * The project uses fakes instead of mocking libraries. Inactive unless the config activates it.
 */
class NoMockingLibraries(config: Config) : Rule(
    config,
    "Mocking libraries are not allowed. Use fakes instead.",
) {
    override fun visitImportDirective(importDirective: KtImportDirective): Unit = TODO()
}
