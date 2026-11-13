package com.plcoding.cardiganapp.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKotlinMultiplatform() {
    extensions.configure<KotlinMultiplatformExtension> {
        extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
            namespace = pathToPackageName()
            compileSdk = libs.versionInt("projectCompileSdkVersion")
            minSdk = libs.versionInt("projectMinSdkVersion")

            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }

            withHostTestBuilder {}.configure {}
            withDeviceTestBuilder {
                sourceSetTreeName = "test"
            }.configure {
                instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }

        iosArm64()
        iosSimulatorArm64()

        applyDefaultHierarchyTemplate()

        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xexpect-actual-classes",
                "-opt-in=kotlin.time.ExperimentalTime",
                "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            )
        }
    }

    // Only the Android host JVM run is filtered; iOS and device test tasks aren't of type Test.
    tasks.withType<Test>().configureEach {
        filter {
            excludeTestsMatching("*UiTest")
            isFailOnNoMatchingTests = false
        }
    }
}
