package com.syzygyhub.core.configuration

import com.syzygyhub.foundation.sharedtypes.SyzygyEnvironment

/**
 * A typed configuration key with a [name] and a [defaultValue].
 */
data class ConfigKey<T>(val name: String, val defaultValue: T)

/**
 * In-memory configuration registry with typed access and per-environment overlays.
 *
 * Values set without an environment apply to all environments (global).
 * Values set with a specific environment override global values when that
 * environment is active.
 *
 * @param initialEnvironment the starting environment.
 */
class ConfigRegistry(initialEnvironment: SyzygyEnvironment = SyzygyEnvironment.PRODUCTION) {
    /** The currently active environment. */
    var environment: SyzygyEnvironment = initialEnvironment
        private set

    private val globalValues = mutableMapOf<String, Any?>()
    private val envValues = mutableMapOf<SyzygyEnvironment, MutableMap<String, Any?>>()

    /**
     * Returns the value for [key] in the current environment.
     * Resolution order: environment-specific value > global value > default.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: ConfigKey<T>): T {
        val envMap = envValues[environment]
        if (envMap != null && key.name in envMap) return envMap[key.name] as T
        if (key.name in globalValues) return globalValues[key.name] as T
        return key.defaultValue
    }

    /**
     * Sets a global [value] for [key] (applies to all environments unless overridden).
     */
    fun <T> set(
        key: ConfigKey<T>,
        value: T,
    ) {
        globalValues[key.name] = value
    }

    /**
     * Sets a [value] for [key] only in [forEnvironment].
     */
    fun <T> set(
        key: ConfigKey<T>,
        value: T,
        forEnvironment: SyzygyEnvironment,
    ) {
        envValues.getOrPut(forEnvironment) { mutableMapOf() }[key.name] = value
    }

    /**
     * Switches the active environment to [to].
     */
    fun switchEnvironment(to: SyzygyEnvironment) {
        environment = to
    }
}
