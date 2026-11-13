plugins {
    alias(libs.plugins.convention.cmp.application)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.data)
            implementation(projects.core.presentation)
            implementation(projects.core.designsystem)

            implementation(projects.feature.auth.domain)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.auth.presentation)
            implementation(projects.feature.boards.domain)
            implementation(projects.feature.boards.data)
            implementation(projects.feature.boards.presentation)
            implementation(projects.feature.cards.domain)
            implementation(projects.feature.cards.data)
            implementation(projects.feature.cards.presentation)
            implementation(projects.feature.labels.domain)
            implementation(projects.feature.labels.data)
            implementation(projects.feature.labels.presentation)
            implementation(projects.feature.transfer.domain)
            implementation(projects.feature.transfer.data)
            implementation(projects.feature.transfer.presentation)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
    }
}
