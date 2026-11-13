plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.boards.domain)
            implementation(projects.core.domain)
        }
    }
}
