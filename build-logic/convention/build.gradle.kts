import dev.detekt.gradle.Detekt

plugins {
    `kotlin-dsl`
    alias(libs.plugins.detekt)
}

group = "com.plcoding.cardiganapp.buildlogic"

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeMultiplatform.gradlePlugin)
    compileOnly(libs.buildkonfig.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)

    testImplementation(libs.kotlin.test)
    testImplementation(project(":pitest-filters"))
    testImplementation(libs.pitest.entry)
}

detekt {
    config.setFrom(layout.settingsDirectory.file("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    parallel = true
}

// build-logic can't apply its own convention plugins, so this mirrors DetektConventionPlugin for a JVM module.
val staticAnalysisRequested = (gradle.parent ?: gradle).startParameter.taskNames
    .any { it.substringAfterLast(':') == "staticAnalysis" }

// A lambda written directly in the script would capture the script object, which the configuration cache rejects.
fun fileUnder(dir: File): Spec<FileTreeElement> = Spec { it.file.startsWith(dir) }
val generatedSources = fileUnder(layout.buildDirectory.get().asFile)

tasks.withType<Detekt>().configureEach {
    reports {
        html.required = true
        sarif.required = true
        markdown.required = false
        checkstyle.required = false
    }
    exclude("**/build/**")
    exclude(generatedSources)
    if (staticAnalysisRequested) ignoreFailures = true
}

tasks.named<Detekt>("detekt") {
    setSource(emptyList<Any>())
    dependsOn("detektMain", "detektTest")
}

tasks.named("check") {
    setDependsOn(dependsOn.filterNot { (it as? NamedDomainObjectProvider<*>)?.name?.startsWith("detekt") == true })
}

tasks.validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.convention.kmp.library.get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("cmpLibrary") {
            id = libs.plugins.convention.cmp.library.get().pluginId
            implementationClass = "CmpLibraryConventionPlugin"
        }
        register("cmpFeature") {
            id = libs.plugins.convention.cmp.feature.get().pluginId
            implementationClass = "CmpFeatureConventionPlugin"
        }
        register("cmpApplication") {
            id = libs.plugins.convention.cmp.application.get().pluginId
            implementationClass = "CmpApplicationConventionPlugin"
        }
        register("androidApplication") {
            id = libs.plugins.convention.android.application.asProvider().get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = libs.plugins.convention.android.application.compose.get().pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("buildkonfig") {
            id = libs.plugins.convention.buildkonfig.get().pluginId
            implementationClass = "BuildKonfigConventionPlugin"
        }
        register("kover") {
            id = libs.plugins.convention.kover.get().pluginId
            implementationClass = "KoverConventionPlugin"
        }
        register("coverage") {
            id = libs.plugins.convention.coverage.get().pluginId
            implementationClass = "CoverageConventionPlugin"
        }
        register("mutation") {
            id = libs.plugins.convention.mutation.asProvider().get().pluginId
            implementationClass = "MutationConventionPlugin"
        }
        register("mutationRoot") {
            id = libs.plugins.convention.mutation.root.get().pluginId
            implementationClass = "MutationRootConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.convention.jvm.library.get().pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("detekt") {
            id = libs.plugins.convention.detekt.get().pluginId
            implementationClass = "DetektConventionPlugin"
        }
        register("staticAnalysisRoot") {
            id = libs.plugins.convention.static.analysis.root.get().pluginId
            implementationClass = "StaticAnalysisRootConventionPlugin"
        }
    }
}
