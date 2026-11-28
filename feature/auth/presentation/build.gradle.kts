plugins {
    alias(libs.plugins.convention.cmp.feature)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.mutation)
    alias(libs.plugins.convention.detekt)
    alias(libs.plugins.convention.ktlint)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.auth.domain)
            implementation(projects.core.domain)

            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.compose.uiTest)
        }
    }
}
