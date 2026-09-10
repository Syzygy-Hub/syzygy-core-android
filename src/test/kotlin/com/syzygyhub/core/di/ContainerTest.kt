package com.syzygyhub.core.di

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

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
}
