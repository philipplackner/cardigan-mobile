package com.plcoding.cardiganapp.convention

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * Runs PIT's command line tool on one module's Android host tests. Inputs are copied from the
 * module's `testAndroidHostTest` task, so PIT sees the same classpath and JVM arguments.
 */
@DisableCachingByDefault(because = "Writes its report to a directory that is not declared as an output")
abstract class PitestTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Classpath
    abstract val pitestClasspath: ConfigurableFileCollection

    /** Main classes, test classes and dependencies of the host test run. */
    @get:Classpath
    abstract val testRuntimeClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val testClassesDirs: ConfigurableFileCollection

    @get:Classpath
    abstract val mutableCodePaths: ConfigurableFileCollection

    @get:Internal
    abstract val sourceDirs: ConfigurableFileCollection

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Input
    abstract val targetClasses: Property<String>

    @get:Input
    abstract val excludedClasses: ListProperty<String>

    @get:Input
    abstract val threads: Property<Int>

    @get:Internal
    abstract val reportDir: DirectoryProperty

    @TaskAction
    fun run() {
        val reportDirFile = reportDir.get().asFile
        reportDirFile.deleteRecursively()

        val hasTests = testClassesDirs.asFileTree.matching { include("**/*.class") }.any()
        if (!hasTests) {
            logger.lifecycle("$path: no host tests, skipped.")
            return
        }

        val classPathFile =
            temporaryDir.resolve("classpath.txt").apply {
                // The host tests load main classes from a jar. PIT mutates the class dirs, so they go first.
                val entries = mutableCodePaths.files + testRuntimeClasspath.files
                writeText(entries.filter { it.exists() }.distinct().joinToString("\n") { it.absolutePath })
            }
        val javaExecutable =
            javaLauncher
                .get()
                .executablePath.asFile.absolutePath

        val args =
            buildList {
                add("--reportDir=${reportDirFile.absolutePath}")
                add("--targetClasses=${targetClasses.get()}")
                add("--targetTests=${targetClasses.get()}")
                add("--excludedTestClasses=*UiTest")
                add("--sourceDirs=${sourceDirs.files.filter { it.isDirectory }.joinToString(",") { it.absolutePath }}")
                add("--mutableCodePaths=${mutableCodePaths.files.joinToString(",") { it.absolutePath }}")
                add("--classPathFile=${classPathFile.absolutePath}")
                add("--includeLaunchClasspath=false")
                add("--useClasspathJar=true")
                add("--mutators=DEFAULTS")
                add("--excludedClasses=${excludedClasses.get().joinToString(",")}")
                add("--excludedMethods=equals,hashCode,toString,copy,copy\$default,component*")
                add("--avoidCallsTo=${MutationExclusions.avoidCallsTo.joinToString(",")}")
                add("--features=${MutationExclusions.features}")
                add("--threads=${threads.get()}")
                add("--failWhenNoMutations=false")
                add("--outputFormats=HTML,XML")
                add("--exportLineCoverage=true")
                add("--timestampedReports=false")
                add("--inputEncoding=UTF-8")
                add("--outputEncoding=UTF-8")
                add("--jvmPath=$javaExecutable")
                val minionArgs = jvmArgs.get()
                if (minionArgs.isNotEmpty()) add("--jvmArgs=${minionArgs.joinToString(",")}")
            }

        execOperations.javaexec {
            executable = javaExecutable
            classpath = pitestClasspath
            mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
            args(args)
        }

        val mutations = reportDirFile.resolve("mutations.xml")
        if (!mutations.isFile || !mutations.readText().contains("<mutation ")) {
            logger.lifecycle("$path: no mutants, all classes are excluded. Skipped.")
        }
    }
}
