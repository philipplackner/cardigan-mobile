plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.transfer.domain)
            implementation(projects.core.domain)
        }
    }
}
