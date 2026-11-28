package com.plcoding.cardiganapp.convention

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.nio.charset.Charset

/**
 * Merges per-module PIT results into one HTML report with `pitest-aggregator`.
 * PIT is loaded only in the worker's isolated classloader, so build-logic doesn't depend on it.
 */
abstract class PitestAggregateAction : WorkAction<PitestAggregateAction.Parameters> {
    interface Parameters : WorkParameters {
        val mutationFiles: ConfigurableFileCollection
        val lineCoverageFiles: ConfigurableFileCollection
        val sourceDirs: ConfigurableFileCollection
        val compiledCodeDirs: ConfigurableFileCollection
        val outputDir: DirectoryProperty
    }

    override fun execute() {
        val loader = Thread.currentThread().contextClassLoader
        val strategyType = loader.loadClass("org.pitest.mutationtest.config.ReportDirCreationStrategy")
        val outputType = loader.loadClass("org.pitest.util.ResultOutputStrategy")
        val dirStrategy =
            loader
                .loadClass("org.pitest.mutationtest.config.UndatedReportDirCreationStrategy")
                .getConstructor()
                .newInstance()
        val outputStrategy =
            loader
                .loadClass("org.pitest.mutationtest.config.DirectoryResultOutputStrategy")
                .getConstructor(String::class.java, strategyType)
                .newInstance(
                    parameters.outputDir
                        .get()
                        .asFile.absolutePath,
                    dirStrategy,
                )

        val aggregatorType = loader.loadClass("org.pitest.aggregate.ReportAggregator")
        val builder = aggregatorType.getMethod("builder").invoke(null)
        val builderType = builder.javaClass

        fun call(
            name: String,
            type: Class<*>,
            value: Any,
        ) {
            builderType.getMethod(name, type).invoke(builder, value)
        }

        fun files(collection: ConfigurableFileCollection): List<File> = collection.files.filter { it.exists() }

        call("mutationResultsFiles", List::class.java, files(parameters.mutationFiles))
        call("lineCoverageFiles", List::class.java, files(parameters.lineCoverageFiles))
        call("sourceCodeDirectories", List::class.java, files(parameters.sourceDirs))
        call("compiledCodeDirectories", List::class.java, files(parameters.compiledCodeDirs))
        call("inputCharSet", Charset::class.java, Charsets.UTF_8)
        call("outputCharset", Charset::class.java, Charsets.UTF_8)
        call("resultOutputStrategy", outputType, outputStrategy)

        val aggregator = builderType.getMethod("build").invoke(builder)
        aggregatorType.getMethod("aggregateReport").invoke(aggregator)
    }
}
