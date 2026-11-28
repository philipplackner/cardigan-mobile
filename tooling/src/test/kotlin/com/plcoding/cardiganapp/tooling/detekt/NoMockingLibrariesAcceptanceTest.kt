package com.plcoding.cardiganapp.tooling.detekt

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isNull
import dev.detekt.api.Issue
import dev.detekt.api.suppressed
import dev.detekt.tooling.api.AnalysisMode
import dev.detekt.tooling.api.DetektProvider
import dev.detekt.tooling.api.spec.ProcessingSpec
import dev.detekt.tooling.api.spec.RulesSpec
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertNotNull

class NoMockingLibrariesAcceptanceTest {
    @TempDir
    lateinit var tmp: Path

    private val controlFile = "x/src/commonTest/kotlin/com/example/ControlTest.kt"

    private fun repoConfig(): Path {
        val path = System.getProperty(CONFIG_PROPERTY)
        assertNotNull(
            path,
            "System property $CONFIG_PROPERTY is not set. Run the tests through ./gradlew :tooling:test.",
        )
        return Path(path)
    }

    private fun lines(vararg lines: String) = lines.joinToString("\n")

    private fun write(
        root: Path,
        relativePath: String,
        content: String,
    ) {
        val file = root.resolve(relativePath)
        file.parent.createDirectories()
        file.writeText(content)
    }

    private fun writeControlFile(root: Path) {
        write(root, controlFile, lines("package com.example", "", "import io.mockk.every", "", "class ControlTest"))
    }

    private fun runDetekt(
        root: Path,
        config: Path = repoConfig(),
    ): List<Issue> {
        val output = StringBuilder()
        val spec =
            ProcessingSpec {
                project {
                    basePath = root
                    inputPaths = listOf(root)
                    analysisMode = AnalysisMode.light
                }
                config {
                    configPaths = listOf(config)
                    useDefaultConfig = true
                }
                rules {
                    failurePolicy = RulesSpec.FailurePolicy.NeverFail
                }
                extensions {
                    fromClassloader { NoMockingLibrariesAcceptanceTest::class.java.classLoader }
                }
                logging {
                    outputChannel = output
                    errorChannel = output
                }
            }
        val result = DetektProvider.load().get(spec).run()
        assertThat(result.error, name = "detekt error, log:\n$output").isNull()
        val container = assertNotNull(result.container, "detekt produced no result, log:\n$output")
        return container.issues.filter {
            it.ruleInstance.ruleSetId.value == "cardigan" && it.ruleInstance.id == "NoMockingLibraries" &&
                !it.suppressed
        }
    }

    private fun List<Issue>.inFile(
        root: Path,
        relativePath: String,
    ): List<Issue> {
        val file = root.resolve(relativePath).normalize()
        return filter { root.resolve(it.location.path).normalize() == file }
    }

    private fun List<Issue>.lines() = map { it.location.source.line }

    private fun assertOnlyControlFileReported(
        root: Path,
        issues: List<Issue>,
        file: String,
    ) {
        assertThat(issues.inFile(root, file), name = file).isEmpty()
        assertThat(issues.inFile(root, controlFile).lines(), name = "control file").containsExactly(3)
    }

    // tests.md: add-detekt-konsist A01
    @Test
    fun `a MockK import in any test source set is reported at that import`() {
        listOf(
            "src/commonTest/kotlin",
            "src/androidHostTest/kotlin",
            "src/androidDeviceTest/kotlin",
            "src/iosTest/kotlin",
            "src/iosArm64Test/kotlin",
            "src/iosSimulatorArm64Test/kotlin",
            "src/test/kotlin",
        ).forEachIndexed { index, sourceSet ->
            val root = tmp.resolve("run$index").createDirectories()
            val file = "x/$sourceSet/com/example/FooTest.kt"
            write(root, file, lines("package com.example", "", "import io.mockk.every", "", "class FooTest"))

            val issues = runDetekt(root)

            assertThat(issues.lines(), name = sourceSet).containsExactly(3)
            assertThat(issues.inFile(root, file).lines(), name = sourceSet).containsExactly(3)
        }
    }

    // tests.md: add-detekt-konsist A02
    @Test
    fun `star and aliased Mockito imports in a test file are each reported`() {
        val file = "x/src/commonTest/kotlin/com/example/FooTest.kt"
        write(
            tmp,
            file,
            lines(
                "package com.example",
                "",
                "import org.mockito.*",
                "import org.mockito.Mockito.mock as m",
                "",
                "class FooTest",
            ),
        )

        val issues = runDetekt(tmp)

        assertThat(issues.inFile(tmp, file).lines().sorted()).containsExactly(3, 4)
    }

    // tests.md: add-detekt-konsist A03
    @Test
    fun `mocking imports in production source sets are not reported`() {
        val productionFiles =
            listOf(
                "x/src/commonMain/kotlin/com/example/Foo.kt",
                "x/src/androidMain/kotlin/com/example/Foo.kt",
                "x/src/iosMain/kotlin/com/example/Foo.kt",
                "y/src/main/kotlin/com/example/Foo.kt",
            )
        productionFiles.forEach {
            write(tmp, it, lines("package com.example", "", "import io.mockk.every", "", "class Foo"))
        }
        writeControlFile(tmp)

        val issues = runDetekt(tmp)

        productionFiles.forEach { assertThat(issues.inFile(tmp, it), name = it).isEmpty() }
        assertThat(issues.inFile(tmp, controlFile).lines(), name = "control file").containsExactly(3)
    }

    // tests.md: add-detekt-konsist A04
    @Test
    fun `a file-level suppression silences the rule`() {
        val file = "x/src/commonTest/kotlin/com/example/SuppressedTest.kt"
        write(
            tmp,
            file,
            lines(
                "@file:Suppress(\"NoMockingLibraries\")",
                "",
                "package com.example",
                "",
                "import io.mockk.every",
                "",
                "class SuppressedTest",
            ),
        )
        writeControlFile(tmp)

        assertOnlyControlFileReported(tmp, runDetekt(tmp), file)
    }

    // tests.md: add-detekt-konsist A05
    @Test
    fun `the finding message names the import path as written without alias`() {
        val cases =
            listOf(
                Triple(
                    "Plain",
                    "import io.mockk.every",
                    "Import of 'io.mockk.every' is not allowed. Use fakes instead of mocking libraries.",
                ),
                Triple(
                    "Star",
                    "import io.mockk.*",
                    "Import of 'io.mockk.*' is not allowed. Use fakes instead of mocking libraries.",
                ),
                Triple(
                    "Aliased",
                    "import org.mockito.Mockito.mock as m",
                    "Import of 'org.mockito.Mockito.mock' is not allowed. Use fakes instead of mocking libraries.",
                ),
            )
        cases.forEach { (name, import) ->
            write(tmp, fileFor(name), lines("package com.example", "", import, "", "class ${name}Test"))
        }

        val issues = runDetekt(tmp)

        cases.forEach { (name, import, message) ->
            assertThat(issues.inFile(tmp, fileFor(name)).map { it.message }, name = import).containsExactly(message)
        }
    }

    private fun fileFor(name: String) = "x/src/commonTest/kotlin/com/example/${name}Test.kt"

    // tests.md: add-detekt-konsist A06
    @Test
    fun `without configuration the rule reports nothing`() {
        val root = tmp.resolve("project").createDirectories()
        val file = "x/src/commonTest/kotlin/com/example/FooTest.kt"
        write(root, file, lines("package com.example", "", "import io.mockk.every", "", "class FooTest"))
        val ownConfig = tmp.resolve("detekt.yml")
        ownConfig.writeText(lines("style:", "  MaxLineLength:", "    active: false", ""))

        assertThat(runDetekt(root, ownConfig), name = "config without cardigan").isEmpty()
        assertThat(runDetekt(root).inFile(root, file).lines(), name = "repo config").containsExactly(3)
    }

    // tests.md: add-detekt-konsist A07
    @Test
    fun `a fully qualified MockK call without an import is not reported`() {
        val file = "x/src/commonTest/kotlin/com/example/FooTest.kt"
        write(
            tmp,
            file,
            lines(
                "package com.example",
                "",
                "class Foo",
                "",
                "class FooTest { fun setUp() { val foo = io.mockk.mockk<Foo>() } }",
            ),
        )
        writeControlFile(tmp)

        assertOnlyControlFileReported(tmp, runDetekt(tmp), file)
    }

    // tests.md: add-detekt-konsist A08
    @Test
    fun `packages that only share a name prefix are not reported`() {
        val file = "x/src/commonTest/kotlin/com/example/FooTest.kt"
        write(
            tmp,
            file,
            lines(
                "package com.example",
                "",
                "import io.mockkfake.Thing",
                "import org.mockitox.Thing as OtherThing",
                "",
                "class FooTest",
            ),
        )
        writeControlFile(tmp)

        assertOnlyControlFileReported(tmp, runDetekt(tmp), file)
    }

    // tests.md: add-detekt-konsist A09
    @Test
    fun `imports that differ only in letter case are not reported`() {
        val file = "x/src/commonTest/kotlin/com/example/FooTest.kt"
        write(
            tmp,
            file,
            lines(
                "package com.example",
                "",
                "import Org.Mockito.Mockito",
                "import IO.MockK.every",
                "",
                "class FooTest",
            ),
        )
        writeControlFile(tmp)

        assertOnlyControlFileReported(tmp, runDetekt(tmp), file)
    }

    private companion object {
        const val CONFIG_PROPERTY = "cardigan.detekt.config"
    }
}
