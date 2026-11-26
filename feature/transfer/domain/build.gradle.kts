plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.mutation)
    alias(libs.plugins.convention.detekt)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)

            implementation(libs.kotlinx.datetime)
        }
    }
}
