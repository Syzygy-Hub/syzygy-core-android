package com.syzygyhub.core.validation

import kotlin.test.Test
import kotlin.test.assertIs

class ValidatorTest {
    @Test
    fun `valid result is Valid`() {
        val result: ValidationResult = ValidationResult.Valid
        assertIs<ValidationResult.Valid>(result)
    }
}
