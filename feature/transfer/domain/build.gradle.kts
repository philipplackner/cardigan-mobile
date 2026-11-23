plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.kover)
    alias(libs.plugins.convention.mutation)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)

            implementation(libs.kotlinx.datetime)
        }
    }
}
