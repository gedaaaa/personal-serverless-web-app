import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("com.google.devtools.ksp")
    id("com.github.johnrengelman.shadow")
    id("io.micronaut.application")
    id("io.micronaut.aot")
    id("io.micronaut.test-resources")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set(project.properties["ktlint.version"] as String)
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
group = "top.sunbath.api.auth"

dependencies {
    // Platform BOMs
    implementation(platform(libs.micronaut.bom))
    implementation(platform(libs.aws.sdk.bom))

    // KSP Annotation Processors
    ksp(libs.micronaut.http.validation)
    ksp(libs.micronaut.serde.processor)
    ksp(libs.micronaut.validation.processor)
    ksp(libs.micronaut.security.annotations)

    // Project Dependencies
    implementation(project(":libs:jvm-shared-lib"))

    // Micronaut Dependencies
    implementation(libs.micronaut.validation)
    implementation(libs.micronaut.security.jwt)
    implementation(libs.micronaut.security)
    implementation(libs.micronaut.aws.apigateway)
    implementation(libs.micronaut.aws.lambda.events.serde)
    implementation(libs.micronaut.aws.sdk.v2)
    implementation(libs.micronaut.crac)
    implementation(libs.micronaut.kotlin.runtime)
    implementation(libs.micronaut.serde.jackson)
    implementation(libs.micronaut.http.client.jdk)
    implementation(libs.micronaut.cache.caffeine)

    // AWS SDK Dependencies
    implementation(libs.aws.dynamodb)
    implementation(libs.aws.ssm)
    implementation(libs.aws.sqs)

    // Other Third-Party Dependencies
    implementation(libs.bcrypt)
    implementation(libs.resend)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib)
    implementation(libs.jakarta.validation)
    implementation(libs.reactor.core)
    implementation(libs.jackson.databind)

    // RuntimeOnly Dependencies
    runtimeOnly(libs.logback)
    runtimeOnly(libs.jackson.module.kotlin)
    runtimeOnly(libs.snakeyaml)

    // TestImplementation Dependencies
    testImplementation(libs.micronaut.http.client.jdk)
    testImplementation(libs.micronaut.test.junit5)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.reactor.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.micronaut.test.resources.testcontainers)
}

application {
    mainClass = "top.sunbath.api.auth.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("lambda_java")
    testRuntime("junit5")
    nativeLambda {
        lambdaRuntimeClassName = "io.micronaut.function.aws.runtime.MicronautLambdaRuntime"
    }
    processing {
        incremental(true)
        annotations("top.sunbath.api.auth.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
    args(
        "-XX:MaximumHeapSizePercent=80",
        "-Dio.netty.allocator.numDirectArenas=0",
        "-Dio.netty.noPreferDirect=true",
    )
}
