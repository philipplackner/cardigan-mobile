package com.plcoding.cardiganapp.convention

import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MutationCountsTest {

    private val files = mutableListOf<File>()

    @AfterTest
    fun cleanUp() {
        files.forEach { it.delete() }
    }

    private fun xml(vararg statuses: String, detectedAttribute: Boolean? = null): File {
        val mutations = statuses.joinToString("\n") { status ->
            val detected = detectedAttribute ?: (status in DETECTED_STATUSES)
            "<mutation detected='$detected' status='$status' numberOfTestsRun='1'><mutatedClass>a.B</mutatedClass></mutation>"
        }
        return createTempFile(suffix = ".xml").toFile().apply {
            writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<mutations>\n$mutations\n</mutations>")
            files += this
        }
    }

    @Test
    fun allFiveDetectedStatusesCountAsKilled() {
        val counts = readMutationCounts(xml("KILLED", "TIMED_OUT", "MEMORY_ERROR", "RUN_ERROR", "NON_VIABLE"))

        assertEquals(MutationCounts(total = 5, detected = 5, noCoverage = 0), counts)
        assertEquals(100.0, counts.score)
        assertEquals(100.0, counts.strength)
    }

    @Test
    fun undetectedStatusesDoNotCountAsKilled() {
        val counts = readMutationCounts(xml("SURVIVED", "NO_COVERAGE", "NOT_STARTED", "STARTED"))

        assertEquals(MutationCounts(total = 4, detected = 0, noCoverage = 1), counts)
        assertEquals(3, counts.survived)
    }

    @Test
    fun statusDecidesEvenWhenTheDetectedAttributeDisagrees() {
        val counts = readMutationCounts(xml("RUN_ERROR", "SURVIVED", detectedAttribute = false))

        assertEquals(1, counts.detected)
    }

    @Test
    fun scoreUsesAllMutantsAndStrengthOnlyCoveredOnes() {
        val counts = readMutationCounts(xml("KILLED", "NON_VIABLE", "SURVIVED", "NO_COVERAGE"))

        assertEquals(50.0, counts.score)
        assertEquals(200.0 / 3, counts.strength)
        assertEquals(
            "mutation score 50.0%, test strength 66.7% (2 killed, 1 survived, 1 no coverage, 4 total)",
            counts.describe(),
        )
    }

    @Test
    fun strengthIsMissingWhenNothingIsCovered() {
        val counts = readMutationCounts(xml("NO_COVERAGE", "NO_COVERAGE"))

        assertEquals(0.0, counts.score)
        assertNull(counts.strength)
        assertEquals("mutation score 0.0%, test strength n/a (0 killed, 0 survived, 2 no coverage, 2 total)", counts.describe())
    }

    @Test
    fun countsAddUp() {
        val sum = MutationCounts(10, 4, 3) + MutationCounts(5, 5, 0)

        assertEquals(MutationCounts(total = 15, detected = 9, noCoverage = 3), sum)
    }
}
