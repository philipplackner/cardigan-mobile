import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.pathToFrameworkName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class CmpApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("convention-cmp-library").get().get().pluginId)
            }

            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType(KotlinNativeTarget::class.java).configureEach {
                    binaries.framework {
                        baseName = pathToFrameworkName()
                        isStatic = true
                    }
                }
            }
        }
    }
}
