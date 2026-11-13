import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("convention-kmp-library").get().get().pluginId)
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.compose")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    androidResources {
                        enable = true
                    }
                }
            }

            dependencies {
                "commonMainImplementation"(libs.library("compose-runtime"))
                "commonMainImplementation"(libs.library("compose-foundation"))
                "commonMainImplementation"(libs.library("compose-ui"))
                "commonMainImplementation"(libs.library("compose-material3"))
                "commonMainImplementation"(libs.library("compose-components-resources"))
                "commonMainImplementation"(libs.library("compose-uiToolingPreview"))

                // KMP library modules have no build types, so debugImplementation isn't available for previews.
                "androidMainImplementation"(libs.library("compose-uiTooling"))
            }
        }
    }
}
