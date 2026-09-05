package com.syzygyhub.core.logging

import kotlin.test.Test
import kotlin.test.assertTrue

class LoggerTest {
    @Test
    fun `log level ordering`() {
        assertTrue(LogLevel.DEBUG.ordinal < LogLevel.ERROR.ordinal)
    }

    @Test
    fun `console destination writes`() {
        val dest = ConsoleLogDestination()
        dest.write("test", LogLevel.INFO)
    }
}
