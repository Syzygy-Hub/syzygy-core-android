package com.syzygyhub.core.lifecycle

/**
 * Application lifecycle state.
 */
enum class AppLifecycleState {
    ACTIVE,
    INACTIVE,
    BACKGROUND,
}

/**
 * Observes app lifecycle transitions.
 */
interface AppLifecycleObserver {
    fun onStateChange(state: AppLifecycleState)
}

/**
 * Tracks the app's current lifecycle state and notifies registered observers.
 */
class AppLifecycleTracker {
    // TODO: observer registry, state tracking, lifecycle-aware scoping
}
