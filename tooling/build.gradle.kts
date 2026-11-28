plugins {
    alias(libs.plugins.convention.jvm.library)
    alias(libs.plugins.convention.detekt)
}

// Konsist bundles kotlin-compiler-embeddable 2.4.21 and detekt is built on Kotlin 2.4.0, so the two never share a runtime classpath.
val konsistTestRuntimeClasspath: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.testImplementation.get())
}

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.api)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.assertk)

    testCompileOnly(libs.detekt.core)
    testCompileOnly(libs.detekt.test)
    testRuntimeOnly(libs.detekt.core)
    testRuntimeOnly(libs.detekt.test)

    testCompileOnly(libs.konsist)
    konsistTestRuntimeClasspath(libs.konsist)
}

val detektConfig = rootProject.layout.projectDirectory.file("config/detekt/detekt.yml")

tasks.test {
    systemProperty("cardigan.detekt.config", detektConfig.asFile.absolutePath)
    inputs.file(detektConfig).withPathSensitivity(PathSensitivity.NONE).withPropertyName("detektConfig")
    exclude("com/plcoding/cardiganapp/tooling/konsist/**")
}

val konsistTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the Konsist architecture checks."
    val testSourceSet = sourceSets.test.get()
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = konsistTestRuntimeClasspath + testSourceSet.output + sourceSets.main.get().output
    include("com/plcoding/cardiganapp/tooling/konsist/**")
    doNotTrackState("Konsist reads source files that are not task inputs.")
}
