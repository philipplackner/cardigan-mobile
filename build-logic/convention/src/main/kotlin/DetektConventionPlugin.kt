import com.plcoding.cardiganapp.convention.ANDROID_APP_TYPE_RESOLVED_DETEKT_TASKS
import com.plcoding.cardiganapp.convention.DETEKT_CONFIG_PATH
import com.plcoding.cardiganapp.convention.DETEKT_TASK
import com.plcoding.cardiganapp.convention.JVM_TYPE_RESOLVED_DETEKT_TASKS
import com.plcoding.cardiganapp.convention.KMP_TYPE_RESOLVED_DETEKT_TASKS
import com.plcoding.cardiganapp.convention.TOOLING_PROJECT
import com.plcoding.cardiganapp.convention.isIosOnlySourceSet
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Opts a module into detekt. `<module>:detekt` runs the type-resolved tasks of the module's
 * Android or JVM compilations; for KMP modules it also analyzes the iOS-only source sets without
 * type resolution. detekt never runs as part of `check`.
 */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.detekt")

            extensions.configure<DetektExtension> {
                config.setFrom(rootProject.layout.projectDirectory.file(DETEKT_CONFIG_PATH))
                buildUponDefaultConfig.set(true)
                parallel.set(true)
                baseline.set(null as java.io.File?)
            }

            dependencies {
                "detektPlugins"(project(TOOLING_PROJECT))
            }

            val buildDir = layout.buildDirectory.get().asFile
            tasks.withType<Detekt>().configureEach {
                reports {
                    html.required.set(true)
                    sarif.required.set(true)
                    markdown.required.set(false)
                    checkstyle.required.set(false)
                }
                exclude("**/build/**")
                // Generated source roots sit inside build/, so their relative paths never contain build/.
                exclude { it.file.startsWith(buildDir) }
            }

            removeDetektFromCheck()

            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                val iosSources =
                    files(
                        provider {
                            extensions
                                .getByType(KotlinMultiplatformExtension::class.java)
                                .sourceSets
                                .filter { isIosOnlySourceSet(it.name) }
                                .map { it.kotlin.sourceDirectories }
                        },
                    )
                tasks.named<Detekt>(DETEKT_TASK) {
                    setSource(iosSources)
                    dependsOn(KMP_TYPE_RESOLVED_DETEKT_TASKS)
                }
            }
            pluginManager.withPlugin("com.android.application") {
                tasks.named<Detekt>(DETEKT_TASK) {
                    setSource(emptyList<Any>())
                    dependsOn(ANDROID_APP_TYPE_RESOLVED_DETEKT_TASKS)
                }
            }
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                tasks.named<Detekt>(DETEKT_TASK) {
                    setSource(emptyList<Any>())
                    dependsOn(JVM_TYPE_RESOLVED_DETEKT_TASKS)
                }
            }
        }
    }

    private fun Project.removeDetektFromCheck() {
        tasks.matching { it.name == LifecycleBasePlugin.CHECK_TASK_NAME }.configureEach {
            setDependsOn(dependsOn.filterNot { it.isDetektTask() })
        }
    }

    private fun Any.isDetektTask(): Boolean =
        when (this) {
            is NamedDomainObjectProvider<*> -> name.startsWith(DETEKT_TASK)
            is Task -> name.startsWith(DETEKT_TASK)
            is String -> substringAfterLast(':').startsWith(DETEKT_TASK)
            else -> false
        }
}
