import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.4.4"
    id("io.micronaut.aot") version "4.4.4"
    id("io.micronaut.test-resources") version "4.4.4"
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
group = "top.sunbath.api.email"

val kotlinVersion = project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")

    runtimeOnly("org.yaml:snakeyaml")

    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.aws:micronaut-aws-sdk-v2")
    implementation("io.micronaut.aws:micronaut-aws-lambda-events-serde")
    implementation("io.micronaut.aws:micronaut-aws-sdk-v2")
    implementation("software.amazon.awssdk:dynamodb")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    // Add shared library dependency
    implementation(project(":libs:jvm-shared-lib"))

    // Add Resend API dependency
    implementation("com.resend:resend-java:3.2.0")

    // Add Jackson databind dependency
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.2")

    // Add Validation dependencies
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
}

application {
    mainClass = "top.sunbath.api.email.ApplicationKt"
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
        annotations("top.sunbath.api.email.*")
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
