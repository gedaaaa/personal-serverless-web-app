plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("java")
}

version = "0.1"
group = "top.sunbath.shared"

repositories {
    mavenCentral()
}
dependencies {}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
}