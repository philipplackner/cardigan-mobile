package com.plcoding.cardiganapp.tooling.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Reports imports from `org.mockito` and `io.mockk`, including star and aliased imports.
 * The project uses fakes instead of mocking libraries. Inactive unless the config activates it.
 */
class NoMockingLibraries(
    config: Config,
) : Rule(
        config,
        "Mocking libraries are not allowed. Use fakes instead.",
    ) {
    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val importedName = importDirective.importedFqName?.asString() ?: return
        if (MOCKING_PACKAGES.none { importedName == it || importedName.startsWith("$it.") }) return

        val path = if (importDirective.isAllUnder) "$importedName.*" else importedName
        report(
            Finding(
                Entity.from(importDirective),
                "Import of '$path' is not allowed. Use fakes instead of mocking libraries.",
            ),
        )
    }

    private companion object {
        val MOCKING_PACKAGES = listOf("org.mockito", "io.mockk")
    }
}
