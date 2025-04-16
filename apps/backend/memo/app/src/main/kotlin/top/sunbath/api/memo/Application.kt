package top.sunbath.api.memo

import io.micronaut.runtime.Micronaut

/**
 * Main application entry point.
 * Starts the Micronaut application with appropriate environment configuration.
 */
fun main(args: Array<String>) {
    Micronaut
        .build()
        .args(*args)
        .packages("top.sunbath.api.memo", "top.sunbath.shared")
        .start()
}
