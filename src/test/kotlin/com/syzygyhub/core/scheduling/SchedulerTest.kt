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
    fun `throttler respects interval`() =
        runTest {
            val throttler = Throttler(1000, this)
            var count = 0
            throttler.throttle { count++ }
            advanceTimeBy(1)
            throttler.throttle { count++ }
            advanceTimeBy(1)
            assertEquals(1, count)
        }
}
