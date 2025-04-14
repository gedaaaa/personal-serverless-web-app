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
    // Platform BOMs
    implementation(platform(libs.aws.sdk.bom))
    implementation(platform(libs.micronaut.bom))

    // KSP Annotation Processors
    ksp(libs.micronaut.validation.processor)

    // Micronaut Dependencies
    implementation(libs.micronaut.cache.core)
    implementation(libs.micronaut.inject)
    implementation(libs.micronaut.security)
    implementation(libs.micronaut.security.jwt)
    implementation(libs.micronaut.serde.api)
    implementation(libs.micronaut.serde.jackson)

    // AWS SDK Dependencies
    implementation(libs.aws.dynamodb)
    implementation(libs.aws.ssm)

    // Other Dependencies
    implementation(libs.jakarta.validation)
    implementation(libs.ksuid)
    runtimeOnly(libs.jackson.module.kotlin)

    // Test Dependencies
    testImplementation(libs.mockk)
    testImplementation(libs.micronaut.test.junit5)
}
