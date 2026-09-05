package com.syzygyhub.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A reactive store that holds state and notifies observers on change.
 */
class StateStore<State>(initial: State) {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<State> = _state.asStateFlow()

    // TODO: reduce, select
}

/**
 * Reduces the current state with an action to produce a new state.
 */
interface StateReducer<State, Action> {
    fun reduce(
        state: State,
        action: Action,
    ): State
}
