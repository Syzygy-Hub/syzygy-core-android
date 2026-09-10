package com.syzygyhub.core.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLifecycleTrackerTest {
    @Test
    fun `initial state is active`() {
        val tracker = AppLifecycleTracker()
        assertEquals(AppLifecycleState.ACTIVE, tracker.currentState)
    }

    @Test
    fun `transition updates current state`() {
        val tracker = AppLifecycleTracker()
        tracker.transition(AppLifecycleState.BACKGROUND)
        assertEquals(AppLifecycleState.BACKGROUND, tracker.currentState)
    }

    @Test
    fun `observers are notified on transition`() {
        val tracker = AppLifecycleTracker()
        val received = mutableListOf<AppLifecycleState>()
        tracker.addObserver(
            object : AppLifecycleObserver {
                override fun onLifecycleChange(state: AppLifecycleState) {
                    received.add(state)
                }
            },
        )
        tracker.transition(AppLifecycleState.INACTIVE)
        tracker.transition(AppLifecycleState.BACKGROUND)
        assertEquals(listOf(AppLifecycleState.INACTIVE, AppLifecycleState.BACKGROUND), received)
    }

    @Test
    fun `removed observer is not notified`() {
        val tracker = AppLifecycleTracker()
        var called = false
        val observer =
            object : AppLifecycleObserver {
                override fun onLifecycleChange(state: AppLifecycleState) {
                    called = true
                }
            }
        tracker.addObserver(observer)
        tracker.removeObserver(observer)
        tracker.transition(AppLifecycleState.TERMINATED)
        assertEquals(false, called)
    }

    @Test
    fun `same state transition is a no-op`() {
        val tracker = AppLifecycleTracker()
        var callCount = 0
        tracker.addObserver(
            object : AppLifecycleObserver {
                override fun onLifecycleChange(state: AppLifecycleState) {
                    callCount++
                }
            },
        )
        tracker.transition(AppLifecycleState.ACTIVE)
        assertEquals(0, callCount)
    }
}
