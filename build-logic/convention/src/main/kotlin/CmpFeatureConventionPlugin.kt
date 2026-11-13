import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class CmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("convention-cmp-library").get().get().pluginId)
            }

            dependencies {
                "commonMainImplementation"(project(":core:presentation"))
                "commonMainImplementation"(project(":core:designsystem"))

                "commonMainImplementation"(platform(libs.library("koin-bom")))
                "commonMainImplementation"(libs.library("koin-compose"))
                "commonMainImplementation"(libs.library("koin-compose-viewmodel"))

                "commonMainImplementation"(libs.library("androidx-lifecycle-viewmodel"))
                "commonMainImplementation"(libs.library("androidx-lifecycle-viewmodelCompose"))
                "commonMainImplementation"(libs.library("androidx-lifecycle-runtimeCompose"))
                "commonMainImplementation"(libs.library("androidx-lifecycle-viewmodelSavedstate"))
                "commonMainImplementation"(libs.library("androidx-savedstate"))
                "commonMainImplementation"(libs.library("androidx-navigation-compose"))

                "androidMainImplementation"(platform(libs.library("koin-bom")))
                "androidMainImplementation"(libs.library("koin-android"))
                "androidMainImplementation"(libs.library("koin-androidx-compose"))
            }
        }
    }
}
