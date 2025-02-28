plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.118.0")
    implementation("software.constructs:constructs:10.3.0")
    implementation("io.micronaut.cdk:micronaut-function-aws-cdk:2.5.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.register("synth") {
    dependsOn("compileKotlin")
    doLast {
        exec {
            workingDir(projectDir)
            commandLine("cdk", "synth")
        }
    }
} 