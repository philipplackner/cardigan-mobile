package com.plcoding.cardiganapp.convention

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Prints the total line and branch coverage of the merged Kover XML report and checks the
 * optional thresholds against the same counters.
 */
@DisableCachingByDefault(because = "Prints to the terminal and produces no output")
abstract class CoverageSummaryTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val xmlReport: RegularFileProperty

    @get:Input
    abstract val moduleCount: Property<Int>

    @get:Input
    abstract val htmlReportPath: Property<String>

    @get:Input
    @get:Optional
    abstract val minLine: Property<Double>

    @get:Input
    @get:Optional
    abstract val minBranch: Property<Double>

    @TaskAction
    fun summarize() {
        val counters = readRootCounters()
        val line = counters.percentage("LINE")
        val branch = counters.percentage("BRANCH")

        logger.lifecycle(
            "Coverage (${moduleCount.get()} modules): line ${line.format()}, branch ${branch.format()}",
        )
        logger.lifecycle("HTML: ${htmlReportPath.get()}")

        val failures = listOfNotNull(
            thresholdFailure("Line", line, "coverage.minLine", minLine.orNull),
            thresholdFailure("Branch", branch, "coverage.minBranch", minBranch.orNull),
        )
        if (failures.isNotEmpty()) {
            throw GradleException(failures.joinToString("\n"))
        }
    }

    private fun readRootCounters(): Map<String, Pair<Long, Long>> {
        val document = DocumentBuilderFactory.newInstance().apply {
            // Kover's XML references the JaCoCo DTD, which must not be fetched.
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }.newDocumentBuilder().parse(xmlReport.get().asFile)

        val root = document.documentElement
        return root.childNodes.let { nodes ->
            (0 until nodes.length)
                .map { nodes.item(it) }
                .filterIsInstance<org.w3c.dom.Element>()
                .filter { it.tagName == "counter" }
                .associate { counter ->
                    counter.getAttribute("type") to
                        (counter.getAttribute("covered").toLong() to counter.getAttribute("missed").toLong())
                }
        }
    }

    private fun Map<String, Pair<Long, Long>>.percentage(type: String): Double? {
        val (covered, missed) = this[type] ?: return null
        val total = covered + missed
        return if (total == 0L) null else covered * 100.0 / total
    }

    private fun Double?.format(): String =
        if (this == null) "n/a" else String.format(Locale.ROOT, "%.1f%%", this)

    private fun thresholdFailure(label: String, actual: Double?, property: String, threshold: Double?): String? {
        if (threshold == null) return null
        val value = actual ?: 0.0
        if (value >= threshold) return null
        return "$label coverage ${actual.format()} is below $property=${threshold.formatThreshold()}"
    }

    private fun Double.formatThreshold(): String =
        if (this % 1.0 == 0.0) toLong().toString() else toString()
}
