package com.syzygyhub.core.scheduling

import kotlin.test.Test

class SchedulerTest {
    @Test
    fun `scheduler initialises`() {
        val scheduler = Scheduler()
        assert(scheduler != null)
    }
}
