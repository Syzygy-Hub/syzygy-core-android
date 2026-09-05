package com.syzygyhub.core.featureflags

/**
 * Defines a feature flag with a key and default value.
 */
data class FeatureFlag<T>(
    val key: String,
    val defaultValue: T,
)

/**
 * Provides feature flag evaluation with local overrides and variant selection.
 */
class FeatureFlagProvider {
    // TODO: evaluation rules, override storage, A/B variant selection
}
