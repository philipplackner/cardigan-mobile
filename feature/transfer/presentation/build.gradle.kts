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
            implementation(projects.feature.transfer.domain)
            implementation(projects.core.domain)

            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.compose.uiTest)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}
