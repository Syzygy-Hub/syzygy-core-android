package com.syzygyhub.core.eventbus

/**
 * A typed event bus for decoupled pub/sub communication.
 */
class EventBus {
    // TODO: typed channels, subscribe, publish, scoped subscriptions, async dispatch
}

/**
 * Token returned from a subscription; cancel to unsubscribe.
 */
class SubscriptionToken {
    // TODO: cancellation
    fun cancel() {}
}
