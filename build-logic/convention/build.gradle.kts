plugins {
    `kotlin-dsl`
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

    testImplementation(libs.kotlin.test)
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
    }
}
