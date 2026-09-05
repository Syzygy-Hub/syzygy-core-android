package com.syzygyhub.core.configuration

import kotlin.test.Test

class ConfigRegistryTest {
    @Test
    fun `registry initialises with environment`() {
        val registry = ConfigRegistry(environment = Environment.DEVELOPMENT)
        assert(registry != null)
    }
}
