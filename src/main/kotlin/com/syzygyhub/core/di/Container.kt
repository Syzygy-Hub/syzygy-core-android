package com.syzygyhub.core.di

/**
 * Lifetime scope for a registered dependency.
 */
enum class Lifetime {
    SINGLETON,
    TRANSIENT,
    SCOPED,
}

/**
 * Thread-safe dependency injection container.
 */
class Container {
    // TODO: registration storage, resolution, child scopes
}
