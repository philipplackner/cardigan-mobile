import com.plcoding.cardiganapp.convention.CodeExclusions
import com.plcoding.cardiganapp.convention.MUTATION_GROUP
import com.plcoding.cardiganapp.convention.MutationExclusions
import com.plcoding.cardiganapp.convention.PITEST_FILTERS_DEPENDENCY
import com.plcoding.cardiganapp.convention.MutationSettings
import com.plcoding.cardiganapp.convention.PITEST_RUN_LOCK
import com.plcoding.cardiganapp.convention.PITEST_TASK
import com.plcoding.cardiganapp.convention.PitestRunLock
import com.plcoding.cardiganapp.convention.PitestTask
import com.plcoding.cardiganapp.convention.isMutationRequested
import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.library
import com.plcoding.cardiganapp.convention.parseMutationSettings
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Opts a KMP module into mutation testing. Registers `pitestAndroidHost`, which runs PIT on the
 * module's Android host tests. Nothing else depends on it, so normal builds never run PIT.
 */
class MutationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val settings = if (isMutationRequested) parseMutationSettings() else MutationSettings.DEFAULT
            val runLock = gradle.sharedServices.registerIfAbsent(PITEST_RUN_LOCK, PitestRunLock::class) {
                maxParallelUsages.set(1)
            }

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

                tasks.register<PitestTask>(PITEST_TASK) {
                    group = MUTATION_GROUP
                    description = "Runs PIT mutation testing on this module's Android host tests."
                    doNotTrackState("Every run analyzes all mutants.")
                    usesService(runLock)

                    // Read the values, not mapped providers, so PIT depends on compiled classes and not on running the host tests.
                    val hostTest = tasks.named<Test>("testAndroidHostTest").get()
                    testRuntimeClasspath.from(hostTest.classpath)
                    testClassesDirs.from(hostTest.testClassesDirs)
                    jvmArgs.set(provider { hostTest.allJvmArgs })
                    javaLauncher.set(hostTest.javaLauncher)
                    mutableCodePaths.from(
                        kotlin.targets.named("android").map { it.compilations.getByName("main").output.classesDirs },
                    )
                    sourceDirs.from(
                        layout.projectDirectory.dir("src/commonMain/kotlin"),
                        layout.projectDirectory.dir("src/androidMain/kotlin"),
                    )
                    pitestClasspath.from(
                        configurations.detachedConfiguration(
                            dependencies.create(libs.library("pitest-commandLine").get()),
                            dependencies.create(PITEST_FILTERS_DEPENDENCY),
                        ),
                    )
                    targetClasses.set("com.plcoding.cardiganapp.*")
                    excludedClasses.set(CodeExclusions.pitClassGlobs + MutationExclusions.classGlobs)
                    threads.set(settings.threads)
                    reportDir.set(layout.buildDirectory.dir("reports/pitest"))
                }
            }
        }
    }
}
