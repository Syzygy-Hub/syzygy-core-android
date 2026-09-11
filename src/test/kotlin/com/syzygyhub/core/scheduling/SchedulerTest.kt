package com.syzygyhub.core.scheduling

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerTest {
    @Test
    fun `scheduled task runs after delay`() =
        runTest {
            val scheduler = CoroutineScheduler(this)
            var executed = false
            scheduler.schedule(100) { executed = true }
            advanceTimeBy(101)
            assertTrue(executed)
        }

    @Test
    fun `cancelled task does not run`() =
        runTest {
            val scheduler = CoroutineScheduler(this)
            var executed = false
            val task = scheduler.schedule(100) { executed = true }
            task.cancel()
            advanceTimeBy(200)
            assertEquals(false, executed)
            assertTrue(task.isCancelled)
        }

    @Test
    fun `debouncer only runs last call`() =
        runTest {
            val debouncer = Debouncer(100, this)
            var value = ""
            debouncer.debounce { value = "first" }
            advanceTimeBy(50)
            debouncer.debounce { value = "second" }
            advanceTimeBy(101)
            assertEquals("second", value)
        }

    @Test
    fun `throttler respects interval with injected clock`() =
        runTest {
            // Start fakeTime at 1 000 ms so the first call passes the
            // `now - lastExecutionTime(0) >= intervalMs(1000)` guard.
            var fakeTime = 1000L
            val throttler = Throttler(1000, this, clock = { fakeTime })
            var count = 0

            // First call at t=1000 — should execute.
            throttler.throttle { count++ }
            advanceTimeBy(1)
            assertEquals(1, count)

            // Second call at t=1500 — only 500 ms since last execution, should be suppressed.
            fakeTime = 1500L
            throttler.throttle { count++ }
            advanceTimeBy(1)
            assertEquals(1, count)
        }

    @Test
    fun `throttler allows call after cooldown elapses`() =
        runTest {
            // Start fakeTime at 1 000 ms so the first call passes the guard.
            var fakeTime = 1000L
            val throttler = Throttler(1000, this, clock = { fakeTime })
            var count = 0

            // First call at t=1000 — executes.
            throttler.throttle { count++ }
            advanceTimeBy(1)
            assertEquals(1, count)

            // Call at t=2001 — 1 001 ms since last execution, interval has elapsed.
            fakeTime = 2001L
            throttler.throttle { count++ }
            advanceTimeBy(1)
            assertEquals(2, count)
        }
}
