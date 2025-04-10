plugins {
    id("application")
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.micronaut.bom))
    implementation(libs.micronaut.starter.aws.cdk) {
        exclude(group = "software.amazon.awscdk", module = "aws-cdk-lib")
    }
    implementation(libs.aws.cdk.lib)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
}
application {
    mainClass = "top.sunbath.api.auth.Main"
}
tasks.withType<Test> {
    useJUnitPlatform()
}
