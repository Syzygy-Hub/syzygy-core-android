package com.syzygyhub.core.navigation

import kotlin.test.Test

class RouterTest {
    @Test
    fun `router initialises`() {
        val router = Router()
        assert(router != null)
    }
}
