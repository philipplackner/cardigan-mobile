plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.cards.domain)
            implementation(projects.core.domain)
        }
    }
}
