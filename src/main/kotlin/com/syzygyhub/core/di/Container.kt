package com.syzygyhub.core.di

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

/**
 * Lifetime scope for a registered dependency.
 */
enum class Lifetime {
    /** A single shared instance created on first resolution. */
    SINGLETON,

    /** A new instance created on every resolution call. */
    TRANSIENT,

    /** A single instance per child container scope; acts like singleton within a scope. */
    SCOPED,
}

/**
 * Thread-safe dependency injection container with support for singleton, transient,
 * and scoped lifetimes. Detects circular dependencies at resolution time.
 *
 * @param parent optional parent container for hierarchical resolution.
 */
class Container(private val parent: Container? = null) {
    private data class Registration<T : Any>(
        val lifetime: Lifetime,
        val factory: suspend (Container) -> T,
    )

    private val mutex = Mutex()
    private val registrations = mutableMapOf<KClass<*>, Registration<*>>()
    private val singletonInstances = mutableMapOf<KClass<*>, Any>()
    private val scopedInstances = mutableMapOf<KClass<*>, Any>()
    private val resolving = mutableSetOf<KClass<*>>()

    /**
     * Registers a factory for the given [type] with the specified [lifetime].
     *
     * @param type the KClass to register.
     * @param lifetime the scope of the created instance.
     * @param factory suspending factory that receives this container for nested resolution.
     */
    fun <T : Any> register(
        type: KClass<T>,
        lifetime: Lifetime,
        factory: suspend (Container) -> T,
    ) {
        registrations[type] = Registration(lifetime, factory)
    }

    /**
     * Resolves an instance of [type]. Throws [IllegalStateException] if the type
     * is not registered or a circular dependency is detected.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> resolve(type: KClass<T>): T {
        // Check for circular dependency (resolving set is per-call-chain)
        if (type in resolving) {
            throw IllegalStateException("Circular dependency detected for ${type.simpleName}")
        }

        val registration =
            registrations[type] as? Registration<T>
                ?: return parent?.resolve(type)
                    ?: throw IllegalStateException("No registration found for ${type.simpleName}")

        return when (registration.lifetime) {
            Lifetime.SINGLETON -> {
                mutex.withLock {
                    singletonInstances[type] as? T
                } ?: run {
                    val instance = createWithGuard(type, registration)
                    mutex.withLock { singletonInstances.getOrPut(type) { instance } as T }
                }
            }
            Lifetime.TRANSIENT -> {
                createWithGuard(type, registration)
            }
            Lifetime.SCOPED -> {
                mutex.withLock {
                    scopedInstances[type] as? T
                } ?: run {
                    val instance = createWithGuard(type, registration)
                    mutex.withLock { scopedInstances.getOrPut(type) { instance } as T }
                }
            }
        }
    }

    private suspend fun <T : Any> createWithGuard(
        type: KClass<T>,
        registration: Registration<T>,
    ): T {
        resolving.add(type)
        try {
            return registration.factory(this)
        } finally {
            resolving.remove(type)
        }
    }

    /**
     * Creates a child container that inherits registrations from this container
     * but maintains its own scoped instance cache.
     */
    fun createChildContainer(): Container = Container(parent = this)
}

/**
 * Registers a factory using reified type parameter.
 */
inline fun <reified T : Any> Container.register(
    lifetime: Lifetime,
    noinline factory: suspend (Container) -> T,
) {
    register(T::class, lifetime, factory)
}

/**
 * Resolves an instance using reified type parameter.
 */
suspend inline fun <reified T : Any> Container.resolve(): T = resolve(T::class)
