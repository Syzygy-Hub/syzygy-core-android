package com.syzygyhub.core.validation

import com.syzygyhub.foundation.primitives.validation.ValidationResult
import com.syzygyhub.foundation.primitives.validation.ValidationRule

/*
 * NOTE — ValidationRule<T> invariance (Tier 2 / v1.1.0):
 *
 * Core previously defined `FieldValidator<in T>` with Kotlin contravariance (`in`),
 * allowing a single validator instance to be used where a supertype validator was expected.
 *
 * Foundation's [ValidationRule]<T> is invariant — it carries no variance modifier.
 * Callers that relied on cross-type variance (e.g. passing a `ValidationRule<String?>` where
 * `ValidationRule<String>` is expected) will encounter a type error and must provide an
 * explicit validator for the exact type.
 *
 * This will be revisited in v1.1.0 when Foundation's ValidationRule contract is finalised.
 */

/** Validates that a nullable string is not null or blank. */
class RequiredValidator : ValidationRule<String?> {
    override fun validate(value: String?): ValidationResult =
        if (value.isNullOrBlank()) {
            ValidationResult.Invalid(listOf("Field is required"))
        } else {
            ValidationResult.Valid
        }
}

/** Validates that a string has at least [minLength] characters. */
class MinLengthValidator(val minLength: Int) : ValidationRule<String> {
    override fun validate(value: String): ValidationResult =
        if (value.length < minLength) {
            ValidationResult.Invalid(listOf("Must be at least $minLength characters"))
        } else {
            ValidationResult.Valid
        }
}

/** Validates that a string has at most [maxLength] characters. */
class MaxLengthValidator(val maxLength: Int) : ValidationRule<String> {
    override fun validate(value: String): ValidationResult =
        if (value.length > maxLength) {
            ValidationResult.Invalid(listOf("Must be at most $maxLength characters"))
        } else {
            ValidationResult.Valid
        }
}

/** Validates that a string looks like an email address. */
class EmailValidator : ValidationRule<String> {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    override fun validate(value: String): ValidationResult =
        if (emailRegex.matches(value)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(listOf("Invalid email address"))
        }
}

/** Validates that a string matches the given [pattern]. */
class RegexValidator(val pattern: Regex, val message: String) : ValidationRule<String> {
    override fun validate(value: String): ValidationResult =
        if (pattern.matches(value)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(listOf(message))
        }
}

/**
 * Controls how a [ValidationPipeline] handles multiple failures.
 */
enum class ValidationMode {
    /** Stops at the first failure. */
    SHORT_CIRCUIT,

    /** Runs all validators and collects all failure messages. */
    COLLECT_ALL,
}

/**
 * Chains multiple [validators] into a pipeline that runs in the given [mode].
 *
 * In [ValidationMode.SHORT_CIRCUIT] mode the first [ValidationResult.Invalid]
 * is returned immediately (messages list intact).  In [ValidationMode.COLLECT_ALL]
 * mode the messages lists from every [ValidationResult.Invalid] are flattened into
 * a single [ValidationResult.Invalid].
 */
class ValidationPipeline<T>(
    private val validators: List<ValidationRule<T>>,
    private val mode: ValidationMode = ValidationMode.SHORT_CIRCUIT,
) : ValidationRule<T> {
    override fun validate(value: T): ValidationResult {
        val errors = mutableListOf<String>()
        for (validator in validators) {
            when (val result = validator.validate(value)) {
                is ValidationResult.Valid -> { /* continue */ }
                is ValidationResult.Invalid -> {
                    if (mode == ValidationMode.SHORT_CIRCUIT) return result
                    errors.addAll(result.messages)
                }
            }
        }
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
