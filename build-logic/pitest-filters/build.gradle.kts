import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    `java-library`
    alias(libs.plugins.ktlint)
}

group = "com.plcoding.cardiganapp.buildlogic"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    compileOnly(libs.pitest.entry)
    implementation(libs.kotlin.metadata.jvm)
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

afterEvaluate {
    tasks.named("check") {
        setDependsOn(dependsOn.filterNot { (it as? NamedDomainObjectProvider<*>)?.name?.startsWith("ktlint") == true })
    }
}
