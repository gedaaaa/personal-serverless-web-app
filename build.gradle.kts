plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25" apply false
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
} 