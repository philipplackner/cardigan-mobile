rootProject.name = "CardiganApp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":composeApp")

include(":core:domain")
include(":core:data")
include(":core:presentation")
include(":core:designsystem")

listOf("auth", "boards", "cards", "labels", "transfer").forEach { feature ->
    include(":feature:$feature:domain")
    include(":feature:$feature:data")
    include(":feature:$feature:presentation")
}
