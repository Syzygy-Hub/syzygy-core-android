package com.syzygyhub.core.lifecycle

/**
 * Application lifecycle state.
 */
enum class AppLifecycleState {
    ACTIVE,
    INACTIVE,
    BACKGROUND,
    TERMINATED,
}

/**
 * Observes app lifecycle transitions.
 */
interface AppLifecycleObserver {
    /** Called when the app lifecycle changes to [state]. */
    fun onLifecycleChange(state: AppLifecycleState)
}

/**
 * Tracks the app's current lifecycle state and notifies registered observers
 * on transitions.
 */
class AppLifecycleTracker {
    private val observers = mutableListOf<AppLifecycleObserver>()

    /** The current lifecycle state. */
    var currentState: AppLifecycleState = AppLifecycleState.ACTIVE
        private set

    /** Registers an [observer] for lifecycle change notifications. */
    fun addObserver(observer: AppLifecycleObserver) {
        observers.add(observer)
    }

    /** Removes a previously registered [observer]. */
    fun removeObserver(observer: AppLifecycleObserver) {
        observers.remove(observer)
    }

    /**
     * Transitions the lifecycle to [to] and notifies all observers.
     */
    fun transition(to: AppLifecycleState) {
        if (currentState == to) return
        currentState = to
        for (observer in observers.toList()) {
            observer.onLifecycleChange(to)
        }
    }
}
