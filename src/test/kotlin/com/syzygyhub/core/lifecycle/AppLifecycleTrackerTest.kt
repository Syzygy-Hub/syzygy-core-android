package com.syzygyhub.core.lifecycle

import kotlin.test.Test

class AppLifecycleTrackerTest {
    @Test
    fun `tracker initialises`() {
        val tracker = AppLifecycleTracker()
        assert(tracker != null)
    }
}
