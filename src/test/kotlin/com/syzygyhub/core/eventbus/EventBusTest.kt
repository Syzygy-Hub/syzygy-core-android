package com.syzygyhub.core.eventbus

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventBusTest {
    data class UserEvent(val name: String)

    data class SystemEvent(val code: Int)

    @Test
    fun `publish and subscribe receives event`() =
        runTest {
            val bus = EventBus()
            val deferred =
                launch {
                    val event = bus.subscribe<UserEvent>().first()
                    assertEquals("alice", event.name)
                }
            // yield so the subscriber starts collecting
            kotlinx.coroutines.yield()
            bus.publish(UserEvent("alice"))
            deferred.join()
        }

    @Test
    fun `subscribe filters by type`() =
        runTest {
            val bus = EventBus()
            val deferred =
                launch {
                    val events = bus.subscribe<SystemEvent>().take(2).toList()
                    assertEquals(listOf(SystemEvent(1), SystemEvent(2)), events)
                }
            kotlinx.coroutines.yield()
            bus.publish(UserEvent("ignored"))
            bus.publish(SystemEvent(1))
            bus.publish(SystemEvent(2))
            deferred.join()
        }

    @Test
    fun `publish returns true on success`() {
        val bus = EventBus()
        val result = bus.publish(UserEvent("test"))
        assertEquals(true, result)
    }

    @Test
    fun `buffer full scenario returns false for overflow event`() =
        runTest {
            val bus = EventBus()
            // tryEmit only applies backpressure when a subscriber is registered.
            // Without a subscriber, tryEmit always returns true (no one to buffer for).
            // Register a subscriber that blocks in its lambda so it never consumes events;
            // then publish without yielding so the 64-event buffer fills and overflows.
            val barrier = kotlinx.coroutines.CompletableDeferred<Unit>()
            val subscriber =
                launch {
                    bus.subscribe<UserEvent>().collect { barrier.await() }
                }
            kotlinx.coroutines.yield() // let the subscriber register with the SharedFlow

            var anyFalse = false
            repeat(65) { i ->
                val result = bus.publish(UserEvent("event-$i"))
                if (!result) anyFalse = true
            }
            assertTrue(anyFalse, "Expected at least one publish() to return false when buffer is full")
            subscriber.cancel()
        }
}
