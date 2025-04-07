import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jlleitschuh.gradle.ktlint")
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

repositories {
    mavenCentral()
}
dependencies {
    // Add Micronaut Serde dependency
    implementation("io.micronaut.serde:micronaut-serde-api:2.7.1")

    // AWS SSM for SsmParameterProvider
    implementation("software.amazon.awssdk:ssm:2.22.9")

    // Micronaut Cache for @Cacheable annotation
    implementation("io.micronaut.cache:micronaut-cache-core:5.2.0")
    implementation("io.micronaut:micronaut-inject:${project.properties["micronautVersion"]}")
}
