package com.syzygyhub.core.eventbus

import kotlin.test.Test

class EventBusTest {
    @Test
    fun `bus initialises`() {
        val bus = EventBus()
        assert(bus != null)
    }
}
