package com.syzygyhub.core.featureflags

import java.util.concurrent.ConcurrentHashMap

/**
 * Defines a feature flag with a [key], a [defaultValue], and an optional [description].
 */
data class FeatureFlag<T>(
    val key: String,
    val defaultValue: T,
    val description: String = "",
)

/**
 * Provides read-only evaluation of feature flags.
 */
interface FeatureFlagProvider {
    /** Returns the current value for the given [flag]. */
    fun <T> value(flag: FeatureFlag<T>): T
}

/**
 * In-memory feature flag provider that supports base values and overrides.
 * Overrides take precedence over base values, which take precedence over defaults.
 */
class InMemoryFeatureFlagProvider : FeatureFlagProvider {
    private val values: MutableMap<String, Any?> = ConcurrentHashMap()
    private val overrides: MutableMap<String, Any?> = ConcurrentHashMap()

    /**
     * Sets the base [value] for a [flag].
     */
    fun <T> setValue(
        flag: FeatureFlag<T>,
        value: T,
    ) {
        values[flag.key] = value
    }

    /**
     * Sets an override [value] for a [flag]. Overrides take precedence over base values.
     */
    fun <T> setOverride(
        flag: FeatureFlag<T>,
        value: T,
    ) {
        overrides[flag.key] = value
    }

    /**
     * Clears the override for a [flag], reverting to the base value or default.
     */
    fun <T> clearOverride(flag: FeatureFlag<T>) {
        overrides.remove(flag.key)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> value(flag: FeatureFlag<T>): T {
        if (flag.key in overrides) {
            try {
                return overrides[flag.key] as T
            } catch (e: ClassCastException) {
                throw ClassCastException(
                    "Override value for flag '${flag.key}' cannot be cast to the expected type. " +
                        "Ensure the stored type matches the FeatureFlag type parameter.",
                )
            }
        }
        if (flag.key in values) {
            try {
                return values[flag.key] as T
            } catch (e: ClassCastException) {
                throw ClassCastException(
                    "Base value for flag '${flag.key}' cannot be cast to the expected type. " +
                        "Ensure the stored type matches the FeatureFlag type parameter.",
                )
            }
        }
        return flag.defaultValue
    }
}
