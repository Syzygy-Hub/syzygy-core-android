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

    // -------------------------------------------------------------------------
    // FIX 18 — MaxLengthValidator standalone boundary tests
    // -------------------------------------------------------------------------

    @Test
    fun `MaxLengthValidator passes string under max`() {
        val validator = MaxLengthValidator(5)
        assertIs<ValidationResult.Valid>(validator.validate("abc"))
    }

    @Test
    fun `MaxLengthValidator passes string at exact max`() {
        val validator = MaxLengthValidator(5)
        assertIs<ValidationResult.Valid>(validator.validate("abcde"))
    }

    // -------------------------------------------------------------------------
    // ITEM 9 — EmailValidator strict mode (RFC 5321)
    // -------------------------------------------------------------------------

    @Test
    fun `strictModeAcceptsValidEmail`() {
        val validator = EmailValidator(strict = true)
        assertIs<ValidationResult.Valid>(validator.validate("user@example.com"))
    }

    @Test
    fun `strictModeRejectsLongLocalPart`() {
        // 65-character local part (exceeds RFC 5321 limit of 64)
        val localPart = "a".repeat(65)
        val validator = EmailValidator(strict = true)
        assertIs<ValidationResult.Invalid>(validator.validate("$localPart@example.com"))
    }

    @Test
    fun `strictModeRejectsTotalOver255`() {
        // Build an address whose total length exceeds 255 characters.
        // localPart(64) + '@'(1) + subdomain(190) + ".com"(4) = 259 chars
        val localPart = "a".repeat(64)
        val domain = "b".repeat(190) + ".com"
        val address = "$localPart@$domain"
        assertTrue(address.length > 255, "Expected address length > 255 but was ${address.length}")
        val validator = EmailValidator(strict = true)
        assertIs<ValidationResult.Invalid>(validator.validate(address))
    }

    @Test
    fun `strictModeRejectsConsecutiveDots`() {
        val validator = EmailValidator(strict = true)
        assertIs<ValidationResult.Invalid>(validator.validate("user..name@example.com"))
    }

    @Test
    fun `MaxLengthValidator fails string over max with correct message`() {
        val validator = MaxLengthValidator(5)
        val result = validator.validate("abcdef")
        assertIs<ValidationResult.Invalid>(result)
        assertTrue(result.messages.isNotEmpty())
        assertTrue(result.messages[0].contains("5"))
    }
}
