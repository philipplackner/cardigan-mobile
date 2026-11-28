import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    // Loads the Kotlin plugin next to ktlint-gradle, which needs its classes in the same classloader.
    `kotlin-dsl` apply false
    alias(libs.plugins.ktlint)
}

// A lambda written directly in the script would capture the script object, which the configuration cache rejects.
fun ktlintFilesUnder(dir: File): Spec<FileTreeElement> = Spec { it.file.startsWith(dir) }
val ktlintBuildDirFiles = ktlintFilesUnder(layout.buildDirectory.get().asFile)

// Same settings as KtlintConventionPlugin, which build-logic can't apply to itself.
ktlint {
    version.set(libs.versions.ktlint)
    outputToConsole.set(true)
    verbose.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.SARIF)
        reporter(ReporterType.CHECKSTYLE)
    }
    filter {
        exclude(ktlintBuildDirFiles)
    }
}
