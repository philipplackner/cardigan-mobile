plugins {
    `java-library`
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
