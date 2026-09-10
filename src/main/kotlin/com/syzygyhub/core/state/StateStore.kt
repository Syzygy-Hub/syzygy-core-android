package com.syzygyhub.core.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Reduces the current state with an action to produce a new state.
 *
 * @param S the state type.
 * @param A the action type.
 */
fun interface StateReducer<S, A> {
    /** Returns a new state by applying [action] to [state]. */
    fun reduce(
        state: S,
        action: A,
    ): S
}

/**
 * A reactive store that holds immutable state, dispatches actions through a reducer,
 * and exposes the state as a [StateFlow].
 *
 * @param initialState the initial state value.
 * @param reducer the pure function that computes the next state.
 */
class StateStore<S, A>(
    initialState: S,
    private val reducer: StateReducer<S, A>,
) {
    private val _state = MutableStateFlow(initialState)

    /** The current state as a [StateFlow]. */
    val state: StateFlow<S> = _state.asStateFlow()

    /**
     * Dispatches an [action] through the reducer, updating the state synchronously.
     */
    fun dispatch(action: A) {
        _state.value = reducer.reduce(_state.value, action)
    }

    /**
     * Creates a derived [Flow] by applying [selector] to each state emission,
     * emitting only when the selected value changes.
     *
     * @param selector a function that extracts a sub-value from the state.
     * @return a [Flow] of the selected values, deduplicated.
     */
    fun <T> select(selector: (S) -> T): Flow<T> = _state.map(selector).distinctUntilChanged()
}
