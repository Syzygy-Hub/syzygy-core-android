package com.syzygyhub.core.configuration

import com.syzygyhub.foundation.sharedtypes.SyzygyEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigRegistryTest {
    private val apiUrl = ConfigKey("api_url", "https://default.api")
    private val timeout = ConfigKey("timeout", 30)

    @Test
    fun `returns default when nothing set`() {
        val registry = ConfigRegistry()
        assertEquals("https://default.api", registry.get(apiUrl))
    }

    @Test
    fun `global set overrides default`() {
        val registry = ConfigRegistry()
        registry.set(apiUrl, "https://custom.api")
        assertEquals("https://custom.api", registry.get(apiUrl))
    }

    @Test
    fun `environment specific value overrides global`() {
        // Core's DEVELOPMENT is represented by SyzygyEnvironment.DEBUG in Foundation
        val registry = ConfigRegistry(SyzygyEnvironment.DEBUG)
        registry.set(apiUrl, "https://global.api")
        registry.set(apiUrl, "https://dev.api", SyzygyEnvironment.DEBUG)
        assertEquals("https://dev.api", registry.get(apiUrl))
    }

    @Test
    fun `switchEnvironment changes active config`() {
        val registry = ConfigRegistry(SyzygyEnvironment.PRODUCTION)
        registry.set(timeout, 60, SyzygyEnvironment.PRODUCTION)
        registry.set(timeout, 5, SyzygyEnvironment.DEBUG)
        assertEquals(60, registry.get(timeout))
        registry.switchEnvironment(SyzygyEnvironment.DEBUG)
        assertEquals(5, registry.get(timeout))
    }

    @Test
    fun `environment property reflects current`() {
        val registry = ConfigRegistry(SyzygyEnvironment.STAGING)
        assertEquals(SyzygyEnvironment.STAGING, registry.environment)
    }
}
