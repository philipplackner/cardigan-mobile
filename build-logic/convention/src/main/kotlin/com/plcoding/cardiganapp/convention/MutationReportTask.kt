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
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Merges the PIT results of the selected modules into one HTML report, prints mutation score and
 * test strength per module and in total, and checks the optional minimum score.
 */
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
        val results = modulePaths.get().zip(moduleReportDirs.get()).mapNotNull { (path, dir) ->
            val xml = File(dir, "mutations.xml")
            if (xml.isFile) ModuleResult(path, File(dir), readCounts(xml)) else null
        }
        val withMutants = results.filter { it.counts.total > 0 }

        if (withMutants.isEmpty()) {
            logger.lifecycle("No mutants in the selected modules. No combined report was written.")
            return
        }

        workerExecutor.classLoaderIsolation {
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
        val total = withMutants.map { it.counts }.reduce(Counts::plus)
        logger.lifecycle("Total: ${total.describe()}")
        logger.lifecycle("HTML: ${htmlReportPath.get()}")

        val threshold = minScore.orNull ?: return
        if (total.score < threshold) {
            throw GradleException(
                "Mutation score ${total.score.format()} is below mutation.minScore=${threshold.formatThreshold()}",
            )
        }
    }

    private fun readCounts(xml: File): Counts {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml)
        val mutations = document.getElementsByTagName("mutation")
        var detected = 0
        var noCoverage = 0
        for (i in 0 until mutations.length) {
            val element = mutations.item(i) as org.w3c.dom.Element
            if (element.getAttribute("detected") == "true") detected++
            if (element.getAttribute("status") == "NO_COVERAGE") noCoverage++
        }
        return Counts(total = mutations.length, detected = detected, noCoverage = noCoverage)
    }

    private class ModuleResult(val path: String, val reportDir: File, val counts: Counts)

    private data class Counts(val total: Int, val detected: Int, val noCoverage: Int) {
        val covered: Int get() = total - noCoverage
        val score: Double get() = detected * 100.0 / total
        val strength: Double? get() = if (covered == 0) null else detected * 100.0 / covered

        operator fun plus(other: Counts) =
            Counts(total + other.total, detected + other.detected, noCoverage + other.noCoverage)

        fun describe(): String =
            "mutation score ${score.format()}, test strength ${strength.format()} " +
                "($detected killed, ${total - detected - noCoverage} survived, $noCoverage no coverage, $total total)"
    }
}

private fun Double?.format(): String =
    if (this == null) "n/a" else String.format(Locale.ROOT, "%.1f%%", this)

private fun Double.formatThreshold(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
