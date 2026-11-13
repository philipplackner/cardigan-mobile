import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import com.plcoding.cardiganapp.convention.pathToPackageName
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.StringReader
import java.util.Properties

class BuildKonfigConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.codingfeline.buildkonfig")
            }

            val localProperties = readLocalProperties()
            val androidBaseUrl = localProperties.requireBaseUrl(
                key = "BASE_URL_ANDROID",
                example = "http://10.0.2.2:8080/api/v1",
            )
            val iosBaseUrl = localProperties.requireBaseUrl(
                key = "BASE_URL_IOS",
                example = "http://localhost:8080/api/v1",
            )

            extensions.configure<BuildKonfigExtension> {
                packageName.set(pathToPackageName())

                // BuildKonfig requires every target field to also exist in defaultConfigs.
                defaultConfigs {
                    buildConfigField(FieldSpec.Type.STRING, BASE_URL_FIELD, androidBaseUrl)
                }
                targetConfigs {
                    create("android") {
                        buildConfigField(FieldSpec.Type.STRING, BASE_URL_FIELD, androidBaseUrl)
                    }
                    create("iosArm64") {
                        buildConfigField(FieldSpec.Type.STRING, BASE_URL_FIELD, iosBaseUrl)
                    }
                    create("iosSimulatorArm64") {
                        buildConfigField(FieldSpec.Type.STRING, BASE_URL_FIELD, iosBaseUrl)
                    }
                }
            }
        }
    }

    private fun Project.readLocalProperties(): Properties {
        val file = rootProject.layout.projectDirectory.file("local.properties")
        val content = providers.fileContents(file).asText.orNull.orEmpty()
        return Properties().apply { load(StringReader(content)) }
    }

    private fun Properties.requireBaseUrl(key: String, example: String): String {
        val value = getProperty(key)?.trim()
        if (value.isNullOrEmpty()) {
            throw GradleException("Missing $key in local.properties (e.g. $example)")
        }
        val normalized = value.trimEnd('/')
        val isHttp = normalized.startsWith("http://") || normalized.startsWith("https://")
        if (!isHttp || !normalized.endsWith("/api/v1")) {
            throw GradleException(
                "$key in local.properties must be an http(s) URL ending in /api/v1, got $value"
            )
        }
        return normalized
    }

    private companion object {
        const val BASE_URL_FIELD = "BASE_URL"
    }
}
