plugins {
    id("application")
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    // Platform BOMs
    implementation(platform(libs.micronaut.bom))

    // Implementation Dependencies
    implementation(libs.aws.cdk.lib)
    implementation(libs.micronaut.starter.aws.cdk) {
        exclude(group = "software.amazon.awscdk", module = "aws-cdk-lib")
    }

    // Test Dependencies
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

application {
    mainClass = "top.sunbath.api.memo.Main"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
