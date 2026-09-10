package com.syzygyhub.core.eventbus

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
