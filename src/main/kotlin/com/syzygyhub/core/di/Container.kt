package com.syzygyhub.core.di

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
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

    /**
     * Tracks types currently being resolved to detect circular dependencies.
     * Uses [ConcurrentHashMap] so the check-and-add is atomic: [ConcurrentHashMap.newKeySet]
     * returns a set whose [MutableSet.add] returns false when the element is already present,
     * allowing us to detect a concurrent or recursive resolution of the same type without a
     * separate read followed by a separate write.
     */
    private val resolving: MutableSet<KClass<*>> = ConcurrentHashMap.newKeySet()

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
        // Atomic check-and-add: ConcurrentHashMap.newKeySet().add() returns false when the
        // element was already present, so the check and mark happen in one operation with no
        // window for a concurrent caller to slip through between the two steps.
        if (!resolving.add(type)) {
            throw IllegalStateException("Circular dependency detected for ${type.simpleName}")
        }
        try {
            return registration.factory(this)
        } finally {
            resolving.remove(type)
        }
    }

    /**
     * Clears all registrations, singleton caches, scoped caches, and the circular-dependency
     * tracking set, returning the container to a clean state.
     *
     * After calling this method any subsequent [resolve] call will throw [IllegalStateException]
     * unless new registrations are added.
     */
    fun resetRegistrations() {
        registrations.clear()
        singletonInstances.clear()
        scopedInstances.clear()
        resolving.clear()
    }

    /**
     * Creates a child container that inherits registrations from this container
     * but maintains its own scoped instance cache.
     *
     * **Scoped-through-parent behaviour**: if a [Lifetime.SCOPED] type is registered
     * *only* in the parent (not overridden in the child), resolution from the child
     * delegates entirely to the parent via [resolve], and the parent's scoped cache
     * is used.  Both children will therefore receive the *same* instance for that type.
     * To give each child scope its own independent instance, register the factory
     * directly on each child container.
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
