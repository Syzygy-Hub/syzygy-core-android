package com.syzygyhub.core.di

import kotlin.test.Test

class ContainerTest {
    @Test
    fun `container initialises`() {
        val container = Container()
        assert(container != null)
    }
}
