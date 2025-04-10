import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.google.devtools.ksp")
    id("io.micronaut.application")
}

ktlint {
    version.set("1.5.0")
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.HTML)
    }
}
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask>().configureEach {
    enabled = true
}

tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask>().configureEach {
    enabled = true
}

version = "0.1"
group = "top.sunbath.shared"

dependencies {
    // platform BOMs
    implementation(platform(libs.micronaut.bom))
    implementation(platform(libs.aws.sdk.bom))

    // KSP Annotation Processors
    ksp(libs.micronaut.validation.processor)

    // Micronaut Dependencies
    implementation(libs.micronaut.serde.api)
    implementation(libs.micronaut.serde.jackson)
    implementation(libs.micronaut.cache.core)
    implementation(libs.micronaut.inject)
    implementation(libs.micronaut.aws.sdk.v2)

    // AWS SDK Dependencies
    implementation(libs.aws.ssm)
    implementation(libs.aws.dynamodb)

    // Other Dependencies
    implementation(libs.ksuid)
    implementation(libs.jakarta.validation)
    runtimeOnly(libs.jackson.module.kotlin)
}
