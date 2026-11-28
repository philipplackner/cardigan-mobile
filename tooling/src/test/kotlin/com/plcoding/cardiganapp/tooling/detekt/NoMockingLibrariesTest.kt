package com.plcoding.cardiganapp.tooling.detekt

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import dev.detekt.api.Finding
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test

class NoMockingLibrariesTest {

    private fun lint(code: String): List<Finding> = NoMockingLibraries(TestConfig("active" to true)).lint(code)

    private fun fileWithImport(import: String) = lines("package com.example", "", import, "", "class FooTest")

    private fun lines(vararg lines: String) = lines.joinToString("\n")

    private fun List<Finding>.lines() = map { it.entity.location.source.line }

    // tests.md: add-detekt-konsist C01
    @Test
    fun `every import from io mockk or org mockito is reported in all import forms`() {
        listOf(
            "import io.mockk.*",
            "import io.mockk.mockk as createMock",
            "import io.mockk.impl.annotations.MockK",
            "import org.mockito.Mockito",
            "import org.mockito.kotlin.*",
            "import org.mockito.kotlin.whenever as on",
            "import  io.mockk.every",
            "import io.mockk.every;",
            "import io.`mockk`.every",
        ).forEach { import ->
            assertThat(lint(fileWithImport(import)).lines(), name = import).containsExactly(3)
        }
    }

    // tests.md: add-detekt-konsist C02
    @Test
    fun `imports outside the two libraries are not reported`() {
        listOf(
            "import kotlin.test.Test",
            "import assertk.assertThat",
            "import io.mock.Thing",
            "import io.mockk_extra.Thing",
            "import com.example.io.mockk.Thing",
            "import org.example.mockito.Thing",
            "import com.example.FakeRepository as mockk",
            "import com.example.testing.mockk",
        ).forEach { import ->
            assertThat(lint(fileWithImport(import)), name = import).isEmpty()
        }
    }

    // tests.md: add-detekt-konsist C03
    @Test
    fun `mentions of the libraries outside import directives are not reported`() {
        listOf(
            lines("package com.example", "class FooTest { val mock: org.mockito.Mockito? = null }"),
            lines("package com.example", "val text = \"import io.mockk.every\""),
            lines("package com.example", "// import io.mockk.every", "class FooTest"),
            lines("package com.example", "/** Do not use [io.mockk.mockk]. */", "class FooTest"),
            lines("package io.mockk.fakes", "class FakeThing"),
        ).forEach { code ->
            assertThat(lint(code), name = code).isEmpty()
        }
    }

    // tests.md: add-detekt-konsist C04
    @Test
    fun `findings land on the mocking imports only in a file with mixed imports`() {
        val code = lines(
            "package com.example",
            "",
            "import kotlin.test.Test",
            "import io.mockk.every",
            "import assertk.assertThat",
            "import org.mockito.Mockito.verify",
            "import kotlin.test.assertEquals",
            "",
            "class FooTest",
        )

        assertThat(lint(code).lines()).containsExactly(4, 6)
    }

    // tests.md: add-detekt-konsist C05
    @Test
    fun `the same mocking import twice gives two findings`() {
        val code = lines(
            "package com.example",
            "",
            "import io.mockk.every",
            "import io.mockk.every",
            "",
            "class FooTest",
        )

        assertThat(lint(code).lines()).containsExactly(3, 4)
    }

    // tests.md: add-detekt-konsist C06
    @Test
    fun `the message path keeps deep star imports and drops aliases of deep members`() {
        listOf(
            "import org.mockito.kotlin.*" to
                "Import of 'org.mockito.kotlin.*' is not allowed. Use fakes instead of mocking libraries.",
            "import org.mockito.kotlin.whenever as on" to
                "Import of 'org.mockito.kotlin.whenever' is not allowed. Use fakes instead of mocking libraries.",
            "import org.mockito.*" to
                "Import of 'org.mockito.*' is not allowed. Use fakes instead of mocking libraries.",
        ).forEach { (import, message) ->
            assertThat(lint(fileWithImport(import)).map { it.message }, name = import).containsExactly(message)
        }
    }
}
