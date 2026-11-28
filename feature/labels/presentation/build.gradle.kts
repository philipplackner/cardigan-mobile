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
            implementation(projects.feature.labels.domain)
            implementation(projects.core.domain)
        }
    }
}
