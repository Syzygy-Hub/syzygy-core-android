package com.syzygyhub.core.state

import kotlin.test.Test
import kotlin.test.assertEquals

class StateStoreTest {
    @Test
    fun `store holds initial state`() {
        val store = StateStore(initial = 0)
        assertEquals(0, store.state.value)
    }
}
