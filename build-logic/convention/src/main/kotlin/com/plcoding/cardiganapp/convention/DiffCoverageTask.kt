package com.plcoding.cardiganapp.convention

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Runs a pinned diff-cover through `uvx` on the merged Kover XML report.
 * Never fails because of low coverage.
 */
@DisableCachingByDefault(because = "Depends on the git working tree, which Gradle doesn't track")
abstract class DiffCoverageTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val xmlReport: RegularFileProperty

    @get:Input
    abstract val diffCoverVersion: Property<String>

    @get:Input
    abstract val excludeNewer: Property<String>

    @get:Input
    abstract val compareBranch: Property<String>

    @get:Input
    abstract val sourceRoots: ListProperty<String>

    @get:Internal
    abstract val repositoryDir: DirectoryProperty

    @get:Internal
    abstract val htmlReport: RegularFileProperty

    @TaskAction
    fun run() {
        val workingDir = repositoryDir.get().asFile
        val uvx = findOnPath("uvx") ?: throw GradleException(
            "diffCoverage needs uv on the PATH to run diff-cover. " +
                "Install it from https://docs.astral.sh/uv/getting-started/installation/",
        )
        val ref = compareBranch.get()
        verifyRef(ref, workingDir)

        val html = htmlReport.get().asFile
        html.parentFile.mkdirs()

        execOperations.exec {
            this.workingDir = workingDir
            commandLine(
                buildList {
                    add(uvx.absolutePath)
                    add("--exclude-newer")
                    add(excludeNewer.get())
                    add("diff-cover==${diffCoverVersion.get()}")
                    add(xmlReport.get().asFile.absolutePath)
                    add("--compare-branch=$ref")
                    add("--include-untracked")
                    add("--format")
                    add("html:${html.absolutePath}")
                    val roots = sourceRoots.get()
                    if (roots.isNotEmpty()) {
                        add("--src-roots")
                        addAll(roots)
                    }
                },
            )
        }
        logger.lifecycle("HTML: ${html.relativeTo(workingDir).path}")
    }

    private fun verifyRef(ref: String, workingDir: File) {
        val output = ByteArrayOutputStream()
        val result = execOperations.exec {
            this.workingDir = workingDir
            commandLine("git", "rev-parse", "--verify", "--quiet", "--end-of-options", "$ref^{commit}")
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }
        if (result.exitValue != 0) {
            throw GradleException(
                "coverage.compareBranch '$ref' doesn't resolve to a commit. " +
                    "Check the name, or run 'git fetch origin' if it's a remote branch.",
            )
        }
    }

    private fun findOnPath(executable: String): File? =
        System.getenv("PATH").orEmpty()
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map { File(it, executable) }
            .firstOrNull { it.isFile && it.canExecute() }
}
