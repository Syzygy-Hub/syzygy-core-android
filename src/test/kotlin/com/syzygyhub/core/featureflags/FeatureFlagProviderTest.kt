package com.syzygyhub.core.featureflags

import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagProviderTest {
    private val darkMode = FeatureFlag("dark_mode", false, "Enable dark mode")
    private val maxRetries = FeatureFlag("max_retries", 3)

    @Test
    fun `returns default value when nothing set`() {
        val provider = InMemoryFeatureFlagProvider()
        assertEquals(false, provider.value(darkMode))
        assertEquals(3, provider.value(maxRetries))
    }

    @Test
    fun `setValue overrides default`() {
        val provider = InMemoryFeatureFlagProvider()
        provider.setValue(darkMode, true)
        assertEquals(true, provider.value(darkMode))
    }

    @Test
    fun `override takes precedence over base value`() {
        val provider = InMemoryFeatureFlagProvider()
        provider.setValue(darkMode, false)
        provider.setOverride(darkMode, true)
        assertEquals(true, provider.value(darkMode))
    }

    @Test
    fun `clearOverride reverts to base value`() {
        val provider = InMemoryFeatureFlagProvider()
        provider.setValue(maxRetries, 5)
        provider.setOverride(maxRetries, 10)
        assertEquals(10, provider.value(maxRetries))
        provider.clearOverride(maxRetries)
        assertEquals(5, provider.value(maxRetries))
    }

    @Test
    fun `provider interface contract`() {
        val provider: FeatureFlagProvider = InMemoryFeatureFlagProvider()
        assertEquals(false, provider.value(darkMode))
    }
}
