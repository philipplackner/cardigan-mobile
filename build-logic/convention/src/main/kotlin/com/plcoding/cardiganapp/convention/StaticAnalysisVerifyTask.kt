package com.plcoding.cardiganapp.convention

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Fails `staticAnalysis` at the end when detekt reported a finding or a Konsist check failed.
 * detekt and `konsistTest` ignore their failures under `staticAnalysis`, so both always run.
 */
@DisableCachingByDefault(because = "Only checks the results of other tasks")
abstract class StaticAnalysisVerifyTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val sarifReports: ConfigurableFileCollection

    @get:Internal
    abstract val konsistResultsDir: DirectoryProperty

    @TaskAction
    fun verify() {
        val findings = sarifReports.files.filter { it.isFile }.sumOf(::countSarifResults)
        val failedChecks =
            konsistResultsDir
                .get()
                .asFile
                .listFiles { file -> file.extension == "xml" }
                .orEmpty()
                .sumOf(::countFailedTests)

        val failures =
            listOfNotNull(
                "detekt reported $findings finding(s). Merged report: build/reports/detekt/merged.sarif"
                    .takeIf { findings > 0 },
                "Konsist: $failedChecks check(s) failed. See the :tooling:konsistTest output above."
                    .takeIf { failedChecks > 0 },
            )
        if (failures.isNotEmpty()) {
            throw GradleException(failures.joinToString("\n"))
        }
    }

    private fun countSarifResults(file: File): Int {
        val sarif = JsonSlurper().parse(file) as Map<*, *>
        return (sarif["runs"] as? List<*>).orEmpty().sumOf { run ->
            ((run as? Map<*, *>)?.get("results") as? List<*>)?.size ?: 0
        }
    }

    private fun countFailedTests(file: File): Int {
        val suite =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(file)
                .documentElement
        return listOf("failures", "errors").sumOf { suite.getAttribute(it).toIntOrNull() ?: 0 }
    }
}
