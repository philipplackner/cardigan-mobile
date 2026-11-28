package com.plcoding.cardiganapp.convention

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * Merges the PIT results of the selected modules into one HTML report, prints mutation score and
 * test strength per module and in total, and checks the optional minimum score.
 */
@DisableCachingByDefault(because = "Aggregates reports and prints to the terminal")
abstract class MutationReportTask : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Classpath
    abstract val aggregatorClasspath: ConfigurableFileCollection

    @get:Input
    abstract val modulePaths: ListProperty<String>

    /** One report dir per entry of [modulePaths], in the same order. */
    @get:Internal
    abstract val moduleReportDirs: ListProperty<String>

    @get:Internal
    abstract val sourceDirs: ConfigurableFileCollection

    @get:Internal
    abstract val compiledCodeDirs: ConfigurableFileCollection

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:Internal
    abstract val htmlReportPath: Property<String>

    @get:Input
    @get:Optional
    abstract val minScore: Property<Double>

    @TaskAction
    fun report() {
        val results =
            modulePaths.get().zip(moduleReportDirs.get()).mapNotNull { (path, dir) ->
                val xml = File(dir, "mutations.xml")
                if (xml.isFile) ModuleResult(path, File(dir), readMutationCounts(xml)) else null
            }
        val withMutants = results.filter { it.counts.total > 0 }

        if (withMutants.isEmpty()) {
            logger.lifecycle("No mutants in the selected modules. No combined report was written.")
            return
        }

        workerExecutor
            .classLoaderIsolation {
                classpath.from(aggregatorClasspath)
            }.submit(PitestAggregateAction::class.java) {
                mutationFiles.from(withMutants.map { File(it.reportDir, "mutations.xml") })
                lineCoverageFiles.from(withMutants.map { File(it.reportDir, "linecoverage.xml") })
                sourceDirs.from(this@MutationReportTask.sourceDirs.filter { it.isDirectory })
                compiledCodeDirs.from(this@MutationReportTask.compiledCodeDirs.filter { it.isDirectory })
                outputDir.set(this@MutationReportTask.outputDir)
            }
        workerExecutor.await()

        val moduleLabel = if (withMutants.size == 1) "module" else "modules"
        logger.lifecycle("Mutation testing (${withMutants.size} $moduleLabel with mutants):")
        withMutants.forEach { logger.lifecycle("  ${it.path}: ${it.counts.describe()}") }
        val total = withMutants.map { it.counts }.reduce(MutationCounts::plus)
        logger.lifecycle("Total: ${total.describe()}")
        logger.lifecycle("HTML: ${htmlReportPath.get()}")

        val threshold = minScore.orNull ?: return
        if (total.score < threshold) {
            throw GradleException(
                "Mutation score ${total.score.formatPercent()} is below mutation.minScore=${threshold.formatThreshold()}",
            )
        }
    }

    private class ModuleResult(
        val path: String,
        val reportDir: File,
        val counts: MutationCounts,
    )
}

private fun Double.formatThreshold(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
