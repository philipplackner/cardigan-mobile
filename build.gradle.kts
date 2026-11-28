plugins {
    // Load every plugin once in the root classloader instead of once per subproject.
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.convention.coverage)
    alias(libs.plugins.convention.mutation.root)
    alias(libs.plugins.convention.static.analysis.root)
    alias(libs.plugins.convention.ktlint)
}
