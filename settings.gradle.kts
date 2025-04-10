pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "8.1.1" apply false
        id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
        id("io.micronaut.aot") version "4.4.4" apply false
        id("io.micronaut.application") version "4.4.4" apply false
        id("io.micronaut.test-resources") version "4.4.4" apply false
        id("org.jetbrains.kotlin.jvm") version "1.9.25" apply false
        id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25" apply false
        id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            // versions
            version("aws-sdk", "2.22.9")
            version("aws-cdk", "2.130.0")
            version("aws-lambda", "1.2.3")
            version("bcrypt", "0.10.2")
            version("jackson", "2.16.2")
            version("jakarta-validation", "3.0.2")
            version("junit", "5.10.2")
            version("kotlin", "1.9.25")
            version("ksuid", "1.1.3")
            version("logback", "1.5.6")
            version("micronaut", "4.7.4")
            version("mockito", "5.11.0")
            version("mockito-kotlin", "5.2.1")
            version("mockk", "1.13.10")
            version("reactor", "3.6.7")
            version("resend", "3.1.0")
            version("slf4j", "2.0.12")
            version("snakeyaml", "2.2")
            version("testcontainers", "1.19.7")

            // BOM
            library("aws-sdk-bom", "software.amazon.awssdk", "bom").versionRef("aws-sdk")
            library("micronaut-bom", "io.micronaut.platform", "micronaut-platform").versionRef("micronaut")

            // dependencies
            library("aws-dynamodb", "software.amazon.awssdk", "dynamodb").withoutVersion()
            library("aws-sqs", "software.amazon.awssdk", "sqs").withoutVersion()
            library("aws-ssm", "software.amazon.awssdk", "ssm").withoutVersion()
            library("aws-cdk-lib", "software.amazon.awscdk", "aws-cdk-lib").versionRef("aws-cdk")
            library("aws-lambda-java-events", "com.amazonaws", "aws-lambda-java-events").versionRef("aws-lambda")
            library("micronaut-starter-aws-cdk", "io.micronaut.starter", "micronaut-starter-aws-cdk").versionRef("micronaut")
            library("slf4j-simple", "org.slf4j", "slf4j-simple").versionRef("slf4j")
            library("bcrypt", "at.favre.lib", "bcrypt").versionRef("bcrypt")
            library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")
            library("jackson-module-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").versionRef("jackson")
            library("jakarta-validation", "jakarta.validation", "jakarta.validation-api").versionRef("jakarta-validation")
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").versionRef("kotlin")
            library("ksuid", "com.github.ksuid", "ksuid").versionRef("ksuid")
            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("reactor-core", "io.projectreactor", "reactor-core").versionRef("reactor")
            library("reactor-test", "io.projectreactor", "reactor-test").versionRef("reactor")
            library("resend", "com.resend", "resend-java").versionRef("resend")
            library("snakeyaml", "org.yaml", "snakeyaml").versionRef("snakeyaml")
            library("micronaut-aws-apigateway", "io.micronaut.aws", "micronaut-aws-apigateway").withoutVersion()
            library("micronaut-aws-lambda-events-serde", "io.micronaut.aws", "micronaut-aws-lambda-events-serde").withoutVersion()
            library("micronaut-aws-sdk-v2", "io.micronaut.aws", "micronaut-aws-sdk-v2").withoutVersion()
            library("micronaut-cache-caffeine", "io.micronaut.cache", "micronaut-cache-caffeine").withoutVersion()
            library("micronaut-cache-core", "io.micronaut.cache", "micronaut-cache-core").withoutVersion()
            library("micronaut-crac", "io.micronaut.crac", "micronaut-crac").withoutVersion()
            library("micronaut-http-client-jdk", "io.micronaut", "micronaut-http-client-jdk").withoutVersion()
            library("micronaut-http-validation", "io.micronaut", "micronaut-http-validation").withoutVersion()
            library("micronaut-inject", "io.micronaut", "micronaut-inject").withoutVersion()
            library("micronaut-kotlin-runtime", "io.micronaut.kotlin", "micronaut-kotlin-runtime").withoutVersion()
            library("micronaut-security", "io.micronaut.security", "micronaut-security").withoutVersion()
            library("micronaut-security-annotations", "io.micronaut.security", "micronaut-security-annotations").withoutVersion()
            library("micronaut-security-jwt", "io.micronaut.security", "micronaut-security-jwt").withoutVersion()
            library("micronaut-serde-api", "io.micronaut.serde", "micronaut-serde-api").withoutVersion()
            library("micronaut-serde-jackson", "io.micronaut.serde", "micronaut-serde-jackson").withoutVersion()
            library("micronaut-serde-processor", "io.micronaut.serde", "micronaut-serde-processor").withoutVersion()
            library("micronaut-test-junit5", "io.micronaut.test", "micronaut-test-junit5").withoutVersion()
            library("micronaut-test-resources-testcontainers", "io.micronaut.testresources", "micronaut-test-resources-testcontainers").withoutVersion()
            library("micronaut-validation", "io.micronaut.validation", "micronaut-validation").withoutVersion()
            library("micronaut-validation-processor", "io.micronaut.validation", "micronaut-validation-processor").withoutVersion()
            library("junit-jupiter-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
            library("mockk", "io.mockk", "mockk").versionRef("mockk")
            library("mockito-core", "org.mockito", "mockito-core").versionRef("mockito")
            library("mockito-kotlin", "org.mockito.kotlin", "mockito-kotlin").versionRef("mockito-kotlin")
            library("testcontainers-junit-jupiter", "org.testcontainers", "junit-jupiter").versionRef("testcontainers")
        }
    }
}

rootProject.name = "helloworld"

include("apps:backend:auth:app")
include("apps:backend:auth:infra")
include("apps:backend:email:app")
include("apps:backend:email:infra")
include("apps:backend:helloworld:app")
include("apps:backend:helloworld:infra")

include("libs:jvm-shared-lib")
