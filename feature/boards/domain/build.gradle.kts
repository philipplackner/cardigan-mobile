plugins {
    alias(libs.plugins.convention.kmp.library)
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
