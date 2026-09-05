package com.syzygyhub.core.configuration

/**
 * Environment identifier for configuration switching.
 */
enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
}

/**
 * In-memory configuration registry with typed access and environment switching.
 */
class ConfigRegistry(
    private val environment: Environment = Environment.PRODUCTION,
) {
    // TODO: key-value storage, environment-based overlays, typed getters
}
