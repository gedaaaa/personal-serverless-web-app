package top.sunbath.api.email

import io.micronaut.runtime.Micronaut

/**
 * Main application entry point.
 * Starts the Micronaut application with appropriate environment configuration.
 */
fun main(args: Array<String>) {
    Micronaut
        .build()
        .args(*args)
        .packages("top.sunbath.api.email", "top.sunbath.shared")
        .start()
}
