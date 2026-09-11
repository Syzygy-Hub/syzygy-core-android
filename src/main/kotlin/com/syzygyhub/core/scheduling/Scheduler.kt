package com.syzygyhub.core.scheduling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A handle to a scheduled operation that can be cancelled.
 */
interface CancellableTask {
    /** Cancels the scheduled task. */
    fun cancel()

    /** Whether this task has been cancelled. */
    val isCancelled: Boolean
}

/**
 * Schedules tasks for delayed execution.
 */
interface Scheduler {
    /**
     * Schedules [action] to run after [delayMs] milliseconds.
     * @return a [CancellableTask] handle.
     */
    fun schedule(
        delayMs: Long,
        action: suspend () -> Unit,
    ): CancellableTask
}

/**
 * [Scheduler] implementation backed by Kotlin coroutines.
 *
 * @param scope the [CoroutineScope] in which tasks are launched.
 */
class CoroutineScheduler(private val scope: CoroutineScope) : Scheduler {
    override fun schedule(
        delayMs: Long,
        action: suspend () -> Unit,
    ): CancellableTask {
        val job =
            scope.launch {
                delay(delayMs)
                action()
            }
        return JobCancellableTask(job)
    }

    private class JobCancellableTask(private val job: Job) : CancellableTask {
        override fun cancel() = job.cancel()

        override val isCancelled: Boolean get() = job.isCancelled
    }
}

/**
 * Debounces calls so that only the last invocation within a [delayMs] window executes.
 *
 * @param delayMs the debounce window in milliseconds.
 * @param scope the [CoroutineScope] for launching the debounced action.
 */
class Debouncer(
    private val delayMs: Long,
    private val scope: CoroutineScope,
) {
    @Volatile private var job: Job? = null

    /**
     * Schedules [action] to run after [delayMs], cancelling any previously scheduled action.
     */
    fun debounce(action: suspend () -> Unit) {
        job?.cancel()
        job =
            scope.launch {
                delay(delayMs)
                action()
            }
    }
}

/**
 * Throttles calls so that at most one invocation runs per [intervalMs] window.
 *
 * @param intervalMs the minimum interval between executions in milliseconds.
 * @param scope the [CoroutineScope] for launching throttled actions.
 * @param clock a function that returns the current time in milliseconds since the Unix epoch.
 *   Defaults to [System.currentTimeMillis]. Inject a fake clock in tests to achieve
 *   deterministic time-based assertions without real wall-clock delays.
 */
class Throttler(
    private val intervalMs: Long,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var lastExecutionTime = 0L

    /**
     * Executes [action] only if at least [intervalMs] has elapsed since the last execution.
     */
    fun throttle(action: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                val now = clock()
                if (now - lastExecutionTime >= intervalMs) {
                    lastExecutionTime = now
                    action()
                }
            }
        }
    }
}
