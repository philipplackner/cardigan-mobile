import com.android.build.api.dsl.ApplicationExtension
import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("convention-android-application").get().get().pluginId)
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<ApplicationExtension> {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                "implementation"(libs.library("compose-uiToolingPreview"))
                "debugImplementation"(libs.library("compose-uiTooling"))
            }
        }
    }
}
