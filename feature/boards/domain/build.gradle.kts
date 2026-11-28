plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.mutation)
    alias(libs.plugins.convention.detekt)
    alias(libs.plugins.convention.ktlint)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)

            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
    }
}
