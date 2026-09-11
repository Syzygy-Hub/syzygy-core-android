package com.syzygyhub.core.lifecycle

import java.util.concurrent.CopyOnWriteArrayList

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
    companion object {
        /**
         * Creates an [AppLifecycleTracker] pre-wired to a lifecycle owner.
         *
         * NOTE: This module is a pure Kotlin/JVM library; `androidx.lifecycle` classes
         * (ProcessLifecycleOwner, DefaultLifecycleObserver, LifecycleOwner) are part of the
         * Android framework and are not available here.  Callers in an Android module should
         * create an [AppLifecycleTracker] directly and wire lifecycle events themselves:
         *
         * ```kotlin
         * val tracker = AppLifecycleTracker()
         * lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
         *     override fun onStart(owner: LifecycleOwner)   = tracker.transition(AppLifecycleState.ACTIVE)
         *     override fun onPause(owner: LifecycleOwner)   = tracker.transition(AppLifecycleState.INACTIVE)
         *     override fun onStop(owner: LifecycleOwner)    = tracker.transition(AppLifecycleState.BACKGROUND)
         *     override fun onDestroy(owner: LifecycleOwner) = tracker.transition(AppLifecycleState.TERMINATED)
         * })
         * ```
         *
         * The lifecycle event mapping is:
         *  - `onStart` / `onResume` → [AppLifecycleState.ACTIVE]
         *  - `onPause`              → [AppLifecycleState.INACTIVE]
         *  - `onStop`               → [AppLifecycleState.BACKGROUND]
         *  - `onDestroy`            → [AppLifecycleState.TERMINATED]
         *
         * `onResume` is a no-op when the tracker is already in [AppLifecycleState.ACTIVE]
         * because [transition] only fires observers on a state change.
         *
         * @param onWire optional callback that receives the freshly-created tracker so the
         *               caller can attach Android lifecycle observers immediately.
         */
        fun fromProcessLifecycle(onWire: (AppLifecycleTracker) -> Unit = {}): AppLifecycleTracker {
            val tracker = AppLifecycleTracker()
            onWire(tracker)
            return tracker
        }
    }

    private val observers: MutableList<AppLifecycleObserver> = CopyOnWriteArrayList()

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
