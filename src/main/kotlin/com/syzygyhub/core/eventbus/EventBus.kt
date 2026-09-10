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
 */
class EventBus {
    private val events = MutableSharedFlow<Any>(extraBufferCapacity = 64)

    /**
     * Publishes an [event] to all matching subscribers.
     *
     * @param event the event instance to broadcast.
     * @return true if the event was emitted successfully.
     */
    fun <E : Any> publish(event: E): Boolean = events.tryEmit(event)

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
