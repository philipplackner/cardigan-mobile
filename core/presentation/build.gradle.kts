plugins {
    alias(libs.plugins.convention.cmp.library)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.mutation)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)

            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.compose.uiTest)
        }
    }
}
