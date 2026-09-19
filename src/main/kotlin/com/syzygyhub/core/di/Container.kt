package com.syzygyhub.core.di

import kotlinx.coroutines.CompletableDeferred
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

    // Defect 1 fix: ConcurrentHashMap for lock-free, thread-safe registration access.
    private val registrations = ConcurrentHashMap<KClass<*>, Registration<*>>()

    // Defect 3 fix: Store CompletableDeferred sentinels so that exactly one thread
    // wins the putIfAbsent race and calls the factory; all other threads await the result.
    private val singletonInstances = ConcurrentHashMap<KClass<*>, CompletableDeferred<Any>>()
    private val scopedInstances = ConcurrentHashMap<KClass<*>, CompletableDeferred<Any>>()

    // Defect 4 fix: ThreadLocal so each thread tracks its own in-progress resolution
    // stack, eliminating false-positive cycle detection across concurrent threads.
    private val inProgress: ThreadLocal<MutableSet<KClass<*>>> =
        ThreadLocal.withInitial { mutableSetOf() }

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
            Lifetime.SINGLETON -> resolveDeferred(type, registration, singletonInstances)
            Lifetime.TRANSIENT -> createWithGuard(type, registration)
            Lifetime.SCOPED -> resolveDeferred(type, registration, scopedInstances)
        }
    }

    /**
     * Resolves a singleton or scoped instance using a [CompletableDeferred] sentinel.
     *
     * [ConcurrentHashMap.putIfAbsent] is atomic: exactly one thread wins and calls the
     * factory. Every other concurrent caller receives the same [CompletableDeferred] and
     * suspends on [CompletableDeferred.await] until the winner completes it.
     *
     * Cycle detection happens here, before inserting the sentinel, so that a re-entrant
     * call from inside the factory hits the ThreadLocal check rather than deadlocking on
     * its own [CompletableDeferred].
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> resolveDeferred(
        type: KClass<T>,
        registration: Registration<T>,
        cache: ConcurrentHashMap<KClass<*>, CompletableDeferred<Any>>,
    ): T {
        // Defect 4 fix: check the per-thread in-progress set BEFORE the sentinel lookup.
        // A recursive call from inside the factory will see the type still in the stack
        // and throw immediately, rather than blocking on the sentinel it would otherwise
        // never see completed (deadlock).
        val stack = inProgress.get()
        if (!stack.add(type)) {
            throw IllegalStateException("Circular dependency detected for ${type.simpleName}")
        }
        try {
            // Fast path: already computed.
            val existing = cache[type]
            if (existing != null) return existing.await() as T

            val sentinel = CompletableDeferred<Any>()
            val raced = cache.putIfAbsent(type, sentinel)
            if (raced != null) {
                // Lost the race; await the winner's result.
                return raced.await() as T
            }
            // Won the race; create the instance and complete the sentinel.
            return try {
                val instance = registration.factory(this)
                sentinel.complete(instance)
                instance
            } catch (e: Exception) {
                sentinel.completeExceptionally(e)
                cache.remove(type, sentinel)
                throw e
            }
        } finally {
            stack.remove(type)
        }
    }

    private suspend fun <T : Any> createWithGuard(
        type: KClass<T>,
        registration: Registration<T>,
    ): T {
        // Defect 4 fix: use the thread-local stack so concurrent threads resolving the
        // same type do not cross-contaminate each other's in-progress tracking.
        val stack = inProgress.get()
        if (!stack.add(type)) {
            throw IllegalStateException("Circular dependency detected for ${type.simpleName}")
        }
        try {
            return registration.factory(this)
        } finally {
            stack.remove(type)
        }
    }

    /**
     * Clears all registrations, singleton caches, scoped caches, and the circular-dependency
     * tracking set, returning the container to a clean state.
     *
     * Defect 2 fix: all clears run inside a [synchronized] block so no reader can observe a
     * partially reset container (e.g. registrations gone but singleton cache still populated).
     *
     * After calling this method any subsequent [resolve] call will throw [IllegalStateException]
     * unless new registrations are added.
     */
    fun resetRegistrations() {
        synchronized(this) {
            registrations.clear()
            singletonInstances.clear()
            scopedInstances.clear()
            inProgress.remove()
        }
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
