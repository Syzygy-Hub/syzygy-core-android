package com.syzygyhub.core.validation

import com.syzygyhub.foundation.primitives.validation.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ValidatorTest {
    @Test
    fun `RequiredValidator rejects blank`() {
        val validator = RequiredValidator()
        assertIs<ValidationResult.Invalid>(validator.validate(""))
        assertIs<ValidationResult.Invalid>(validator.validate(null))
        assertIs<ValidationResult.Valid>(validator.validate("ok"))
    }

    @Test
    fun `MinLengthValidator checks length`() {
        val validator = MinLengthValidator(3)
        assertIs<ValidationResult.Invalid>(validator.validate("ab"))
        assertIs<ValidationResult.Valid>(validator.validate("abc"))
    }

    @Test
    fun `EmailValidator validates format`() {
        val validator = EmailValidator()
        assertIs<ValidationResult.Valid>(validator.validate("user@example.com"))
        assertIs<ValidationResult.Invalid>(validator.validate("not-email"))
    }

    @Test
    fun `pipeline short circuits on first error`() {
        val pipeline =
            ValidationPipeline(
                listOf(MinLengthValidator(5), MaxLengthValidator(3)),
                ValidationMode.SHORT_CIRCUIT,
            )
        val result = pipeline.validate("ab")
        assertIs<ValidationResult.Invalid>(result)
        // Short-circuit: only the first validator's single message is present
        assertEquals(1, result.messages.size)
    }

    @Test
    fun `pipeline collects all errors`() {
        val pipeline =
            ValidationPipeline(
                listOf(MinLengthValidator(5), RegexValidator(Regex("^[A-Z]+$"), "Must be uppercase")),
                ValidationMode.COLLECT_ALL,
            )
        val result = pipeline.validate("ab")
        assertIs<ValidationResult.Invalid>(result)
        // Both validators fail — messages list must contain both messages
        assertTrue(result.messages.size >= 2)
        assertTrue(result.messages.any { it.contains("least") })
        assertTrue(result.messages.any { it.contains("uppercase") })
    }

    @Test
    fun `Invalid carries messages list`() {
        val result = MinLengthValidator(10).validate("hi")
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.messages.isNotEmpty())
        assertTrue(result.messages[0].isNotBlank())
    }
}
