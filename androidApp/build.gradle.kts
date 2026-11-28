plugins {
    alias(libs.plugins.convention.android.application.compose)
    alias(libs.plugins.convention.detekt)
}

dependencies {
    implementation(projects.composeApp)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
