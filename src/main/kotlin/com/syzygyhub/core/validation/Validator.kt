package com.syzygyhub.core.validation

/**
 * Result of a single validation check.
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * A composable validator for a single field value.
 */
interface FieldValidator<T> {
    fun validate(value: T): ValidationResult
}

/**
 * Chains multiple validators into a pipeline.
 */
class ValidationPipeline<T>(
    private val validators: List<FieldValidator<T>>,
) : FieldValidator<T> {
    // TODO: short-circuit / collect-all modes
    override fun validate(value: T): ValidationResult = ValidationResult.Valid
}
