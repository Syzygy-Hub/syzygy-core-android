package com.syzygyhub.core.featureflags

import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagProviderTest {
    @Test
    fun `flag holds default value`() {
        val flag = FeatureFlag(key = "dark_mode", defaultValue = false)
        assertEquals(false, flag.defaultValue)
    }
}
