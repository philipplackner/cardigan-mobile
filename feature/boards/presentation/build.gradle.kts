plugins {
    alias(libs.plugins.convention.cmp.feature)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.mutation)
    alias(libs.plugins.convention.detekt)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.boards.domain)
            implementation(projects.core.domain)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.compose.uiTest)
        }
    }
}
