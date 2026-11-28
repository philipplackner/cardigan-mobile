package com.plcoding.cardiganapp.tooling.konsist

import assertk.assertThat
import assertk.assertions.isNotEmpty
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test

class DomainLayerKonsistTest {
    private val domainModulePath = Regex("^(core/domain/src/|feature/[^/]+/domain/src/)")

    private val forbiddenSegments = listOf("data", "presentation", "designsystem")

    private val forbiddenRoots = listOf("io.ktor", "org.koin", "androidx", "org.jetbrains.compose")

    private fun isForbidden(importName: String): Boolean =
        importName.split('.').any { it in forbiddenSegments } ||
            forbiddenRoots.any { importName == it || importName.startsWith("$it.") }

    @Test
    fun `domain code imports no other layers and no frameworks`() {
        val domainFiles =
            Konsist.scopeFromProject().files.filter { file ->
                domainModulePath.containsMatchIn(file.projectPath.removePrefix("/")) &&
                    file.packagee?.name?.contains(".domain") == true
            }
        assertThat(domainFiles).isNotEmpty()

        domainFiles.flatMap { it.imports }.assertFalse { isForbidden(it.name) }
    }
}
