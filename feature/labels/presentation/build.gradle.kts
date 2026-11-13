plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.labels.domain)
            implementation(projects.core.domain)
        }
    }
}
