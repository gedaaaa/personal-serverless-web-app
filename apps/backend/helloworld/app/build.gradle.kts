import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("com.google.devtools.ksp")
    id("com.github.johnrengelman.shadow")
    id("io.micronaut.application")
    id("io.micronaut.aot")
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
group = "top.sunbath.api"

dependencies {
    // Platform BOMs
    implementation(platform(libs.aws.sdk.bom))
    implementation(platform(libs.micronaut.bom))

    // KSP Annotation Processors
    ksp(libs.micronaut.http.validation)
    ksp(libs.micronaut.security.annotations)
    ksp(libs.micronaut.serde.processor)
    ksp(libs.micronaut.validation.processor)

    // Project Dependencies
    implementation(project(":libs:jvm-shared-lib"))

    // Micronaut Dependencies
    implementation(libs.kotlin.stdlib)
    implementation(libs.micronaut.security)
    implementation(libs.micronaut.security.jwt)
    implementation(libs.micronaut.validation)
    runtimeOnly(libs.micronaut.aws.lambda.events.serde)
    runtimeOnly(libs.micronaut.http.client.jdk)
    runtimeOnly(libs.micronaut.kotlin.runtime)

    // Other Third-Party Dependencies
    runtimeOnly(libs.jackson.module.kotlin)
    runtimeOnly(libs.kotlin.reflect)
    runtimeOnly(libs.logback)
    runtimeOnly(libs.snakeyaml)

    // Test Dependencies
    testRuntimeOnly(libs.micronaut.http.client.jdk)
}

application {
    mainClass = "top.sunbath.api.ApplicationKt"
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
        annotations("top.sunbath.api.*")
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
