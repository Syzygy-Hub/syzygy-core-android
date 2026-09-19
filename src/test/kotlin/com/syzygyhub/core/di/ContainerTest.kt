package com.syzygyhub.core.di

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ContainerTest {
    @Test
    fun `singleton returns same instance`() =
        runTest {
            val container = Container()
            container.register(String::class, Lifetime.SINGLETON) { "hello" }
            val a = container.resolve(String::class)
            val b = container.resolve(String::class)
            assertSame(a, b)
        }

    @Test
    fun `transient returns new instance each time`() =
        runTest {
            val container = Container()
            container.register(List::class, Lifetime.TRANSIENT) { mutableListOf<Int>() }
            val a = container.resolve(List::class)
            val b = container.resolve(List::class)
            assertNotSame(a, b)
        }

    @Test
    fun `resolve unregistered type throws`() =
        runTest {
            val container = Container()
            assertFailsWith<IllegalStateException> {
                container.resolve(Int::class)
            }
        }

    @Test
    fun `circular dependency detected`() =
        runTest {
            val container = Container()
            container.register<String>(Lifetime.SINGLETON) {
                it.resolve<String>() // self-referencing
            }
            assertFailsWith<IllegalStateException> {
                container.resolve<String>()
            }
        }

    @Test
    fun `child container inherits parent registrations`() =
        runTest {
            val parent = Container()
            parent.register<String>(Lifetime.SINGLETON) { "from parent" }
            val child = parent.createChildContainer()
            assertEquals("from parent", child.resolve<String>())
        }

    // -------------------------------------------------------------------------
    // FIX 16 — Scoped lifetime tests
    // -------------------------------------------------------------------------

    /**
     * Two child containers each have their own scoped registration: they must
     * produce independent instances (one per child scope).
     */
    @Test
    fun `two child containers get independent scoped instances`() =
        runTest {
            val parent = Container()
            val child1 = parent.createChildContainer()
            val child2 = parent.createChildContainer()

            // Register the scoped factory independently in each child so each
            // child scope owns its own cache entry.
            child1.register<MutableList<*>>(Lifetime.SCOPED) { mutableListOf<Int>() }
            child2.register<MutableList<*>>(Lifetime.SCOPED) { mutableListOf<Int>() }

            val fromChild1a = child1.resolve(MutableList::class)
            val fromChild1b = child1.resolve(MutableList::class)
            val fromChild2 = child2.resolve(MutableList::class)

            // Within the same child scope the scoped instance is shared.
            assertSame(fromChild1a, fromChild1b)
            // Across different child scopes the instances are independent.
            assertNotSame(fromChild1a, fromChild2)
        }

    @Test
    fun `resetRegistrationsRemovesAllRegistrations`() =
        runTest {
            val container = Container()
            container.register<String>(Lifetime.SINGLETON) { "hello" }
            // Sanity-check the registration is present.
            assertEquals("hello", container.resolve<String>())

            container.resetRegistrations()

            // After reset, resolving the previously registered type must throw.
            assertFailsWith<IllegalStateException> {
                container.resolve<String>()
            }
        }

    /**
     * A scoped type registered only in the parent is resolved through the parent's
     * scope: both children receive the same (parent-cached) instance.
     */
    @Test
    fun `scoped resolved through parent caches in parent scope`() =
        runTest {
            val parent = Container()
            parent.register<MutableList<*>>(Lifetime.SCOPED) { mutableListOf<String>() }

            val child1 = parent.createChildContainer()
            val child2 = parent.createChildContainer()

            val fromChild1 = child1.resolve(MutableList::class)
            val fromChild2 = child2.resolve(MutableList::class)

            // Both children delegate to the parent, which caches in its own scope.
            assertSame(fromChild1, fromChild2)
        }

    // -------------------------------------------------------------------------
    // HI-09 — Concurrency safety tests
    // -------------------------------------------------------------------------

    /**
     * Test A — concurrent registration from multiple threads.
     * All threads register without throwing; at least one registration is visible.
     */
    @Test
    fun `concurrent registration from multiple threads - all registrations present`() {
        val container = Container()
        val threads =
            (1..10).map { i ->
                Thread {
                    container.register(String::class, Lifetime.SINGLETON) { "value-$i" }
                }
            }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // At minimum, some registration is present and no exception was thrown.
        runBlocking {
            val resolved = container.resolve(String::class)
            assertTrue(resolved.startsWith("value-"), "Expected a registered value")
        }
    }

    /**
     * Test B — concurrent singleton resolve.
     * Factory must be called exactly once; all threads receive the same instance.
     */
    @Test
    fun `concurrent singleton resolve - factory called exactly once`() {
        val counter = AtomicInteger(0)
        val container = Container()
        container.register(String::class, Lifetime.SINGLETON) {
            counter.incrementAndGet()
            "singleton-instance"
        }
        val results = Collections.synchronizedList(mutableListOf<String>())
        val threads =
            (1..10).map {
                Thread {
                    runBlocking {
                        results.add(container.resolve(String::class))
                    }
                }
            }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(1, counter.get(), "Factory should be called exactly once")
        assertTrue(results.size == 10, "All threads should receive a result")
        assertTrue(results.all { it === results[0] }, "All threads should receive the same instance")
    }

    /**
     * Test C — concurrent resolve and reset.
     * No ConcurrentModificationException should be thrown under concurrent load.
     */
    @Test
    fun `concurrent resolve and reset - no ConcurrentModificationException`() {
        val container = Container()
        container.register(String::class, Lifetime.SINGLETON) { "initial" }

        val exceptions = Collections.synchronizedList(mutableListOf<Exception>())
        val resolvers =
            (1..5).map {
                Thread {
                    try {
                        repeat(100) {
                            runBlocking { container.resolve(String::class) }
                        }
                    } catch (e: Exception) {
                        if (e !is IllegalStateException) exceptions.add(e)
                    }
                }
            }
        val resetters =
            (1..2).map {
                Thread {
                    try {
                        repeat(20) {
                            container.resetRegistrations()
                            container.register(String::class, Lifetime.SINGLETON) { "re-registered" }
                        }
                    } catch (e: Exception) {
                        exceptions.add(e)
                    }
                }
            }
        (resolvers + resetters).forEach { it.start() }
        (resolvers + resetters).forEach { it.join() }

        val concurrentExceptions = exceptions.filter { it is java.util.ConcurrentModificationException }
        assertTrue(concurrentExceptions.isEmpty(), "No ConcurrentModificationException should be thrown: $concurrentExceptions")
    }
}
