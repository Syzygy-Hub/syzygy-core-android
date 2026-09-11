package com.syzygyhub.core.state

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StateStoreTest {
    sealed class Action {
        data class Increment(val amount: Int) : Action()

        data object Reset : Action()
    }

    private val reducer =
        StateReducer<Int, Action> { state, action ->
            when (action) {
                is Action.Increment -> state + action.amount
                is Action.Reset -> 0
            }
        }

    @Test
    fun `store holds initial state`() {
        val store = StateStore(0, reducer)
        assertEquals(0, store.state.value)
    }

    @Test
    fun `dispatch updates state through reducer`() {
        val store = StateStore(0, reducer)
        store.dispatch(Action.Increment(5))
        assertEquals(5, store.state.value)
    }

    @Test
    fun `multiple dispatches accumulate`() {
        val store = StateStore(0, reducer)
        store.dispatch(Action.Increment(3))
        store.dispatch(Action.Increment(7))
        assertEquals(10, store.state.value)
    }

    @Test
    fun `select emits derived values`() =
        runTest {
            data class AppState(val count: Int, val name: String)
            val appReducer =
                StateReducer<AppState, String> { state, action ->
                    state.copy(name = action)
                }
            val store = StateStore(AppState(0, "init"), appReducer)
            store.dispatch("updated")
            val name = store.select { it.name }.first()
            assertEquals("updated", name)
        }

    @Test
    fun `reset action works`() {
        val store = StateStore(10, reducer)
        store.dispatch(Action.Reset)
        assertEquals(0, store.state.value)
    }

    @Test
    fun `concurrent dispatches do not lose updates`() =
        runTest {
            val store = StateStore(0, reducer)
            val coroutineCount = 100
            val jobs =
                (1..coroutineCount).map {
                    launch { store.dispatch(Action.Increment(1)) }
                }
            jobs.forEach { it.join() }
            assertEquals(coroutineCount, store.state.value)
        }
}
