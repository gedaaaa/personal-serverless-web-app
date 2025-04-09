import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("io.micronaut.application") version "4.4.4"
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
    ksp("io.micronaut.validation:micronaut-validation-processor")

    // Add Micronaut Serde dependency
    implementation("io.micronaut.serde:micronaut-serde-api:2.7.1")

    implementation("software.amazon.awssdk:ssm:2.22.9")
    implementation("software.amazon.awssdk:dynamodb:2.22.9")
    implementation("io.micronaut.aws:micronaut-aws-sdk-v2:4.10.0")
    implementation("io.micronaut.serde:micronaut-serde-jackson")

    // Micronaut Cache for @Cacheable annotation
    implementation("io.micronaut.cache:micronaut-cache-core:5.2.0")
    implementation("io.micronaut:micronaut-inject:${project.properties["micronautVersion"]}")

    implementation("com.github.ksuid:ksuid:1.1.3")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
}
