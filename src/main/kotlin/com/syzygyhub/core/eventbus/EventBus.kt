package com.syzygyhub.core.eventbus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

/**
 * A typed event bus that uses [kotlinx.coroutines.flow.SharedFlow] for
 * decoupled publish/subscribe communication.
 *
 * Events are dispatched by type; subscribers receive only events matching
 * the requested class.
 *
 * **Buffer limit**: the internal [MutableSharedFlow] is configured with an
 * `extraBufferCapacity` of **64 events**. If the buffer is full when [publish]
 * is called (i.e. no subscriber is actively consuming), [tryEmit] returns false
 * and the event is dropped. A warning is printed to stderr in this case.
 * Subscribers should consume events promptly to avoid buffer exhaustion.
 */
class EventBus {
    private val events = MutableSharedFlow<Any>(extraBufferCapacity = 64)

    /**
     * Publishes an [event] to all matching subscribers.
     *
     * Returns true if the event was accepted into the buffer. Returns false — and
     * emits a warning to stderr — when the 64-event buffer is full and the event
     * must be dropped.
     *
     * @param event the event instance to broadcast.
     * @return true if the event was emitted successfully, false if the buffer was full.
     */
    fun <E : Any> publish(event: E): Boolean {
        val emitted = events.tryEmit(event)
        if (!emitted) {
            System.err.println(
                "[EventBus] WARNING: event buffer full (capacity=64); dropped event of type " +
                    "${event::class.simpleName}. Ensure subscribers are consuming events promptly.",
            )
        }
        return emitted
    }

    /**
     * Subscribes to events of type [E] using a reified type parameter.
     *
     * @return a [Flow] that emits only events of type [E].
     */
    inline fun <reified E : Any> subscribe(): Flow<E> = subscribe(E::class)

    /**
     * Subscribes to events of the given [eventType].
     *
     * @param eventType the [KClass] of events to receive.
     * @return a [Flow] that emits only events matching [eventType].
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : Any> subscribe(eventType: KClass<E>): Flow<E> =
        events.asSharedFlow()
            .filter { eventType.isInstance(it) }
            .map { it as E }
}
