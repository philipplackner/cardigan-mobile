import com.android.build.api.dsl.ApplicationExtension
import com.plcoding.cardiganapp.convention.configureKotlin
import com.plcoding.cardiganapp.convention.libs
import com.plcoding.cardiganapp.convention.versionInt
import com.plcoding.cardiganapp.convention.versionString
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }

            extensions.configure<ApplicationExtension> {
                val applicationId = libs.versionString("projectApplicationId")
                namespace = applicationId
                compileSdk = libs.versionInt("projectCompileSdkVersion")

                defaultConfig {
                    this.applicationId = applicationId
                    minSdk = libs.versionInt("projectMinSdkVersion")
                    targetSdk = libs.versionInt("projectTargetSdkVersion")
                    versionCode = libs.versionInt("projectVersionCode")
                    versionName = libs.versionString("projectVersionName")
                }
                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            configureKotlin()
        }
    }
}
