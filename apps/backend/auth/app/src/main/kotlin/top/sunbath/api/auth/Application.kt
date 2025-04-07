package top.sunbath.api.auth

import io.micronaut.runtime.Micronaut

/**
 * Main application entry point.
 * Starts the Micronaut application with appropriate environment configuration.
 */
fun main(args: Array<String>) {
    Micronaut
        .build()
        .args(*args)
        .packages("top.sunbath.api.auth", "top.sunbath.shared")
        .start()
}
