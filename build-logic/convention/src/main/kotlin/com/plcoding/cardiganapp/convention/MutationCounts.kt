package com.plcoding.cardiganapp.convention

import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** PIT's own list of statuses that count as detected, so the summary matches the HTML report. */
internal val DETECTED_STATUSES = setOf("KILLED", "TIMED_OUT", "MEMORY_ERROR", "RUN_ERROR", "NON_VIABLE")

internal data class MutationCounts(
    val total: Int,
    val detected: Int,
    val noCoverage: Int,
) {
    val covered: Int get() = total - noCoverage
    val survived: Int get() = total - detected - noCoverage
    val score: Double get() = detected * 100.0 / total
    val strength: Double? get() = if (covered == 0) null else detected * 100.0 / covered

    operator fun plus(other: MutationCounts) =
        MutationCounts(total + other.total, detected + other.detected, noCoverage + other.noCoverage)

    fun describe(): String =
        "mutation score ${score.formatPercent()}, test strength ${strength.formatPercent()} " +
            "($detected killed, $survived survived, $noCoverage no coverage, $total total)"
}

/** Counts the mutants in a PIT `mutations.xml` by their `status`. */
internal fun readMutationCounts(xml: File): MutationCounts {
    val mutations =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(xml)
            .getElementsByTagName("mutation")
    val statuses = (0 until mutations.length).map { (mutations.item(it) as Element).getAttribute("status") }
    return MutationCounts(
        total = statuses.size,
        detected = statuses.count { it in DETECTED_STATUSES },
        noCoverage = statuses.count { it == "NO_COVERAGE" },
    )
}

internal fun Double?.formatPercent(): String = if (this == null) "n/a" else String.format(Locale.ROOT, "%.1f%%", this)
