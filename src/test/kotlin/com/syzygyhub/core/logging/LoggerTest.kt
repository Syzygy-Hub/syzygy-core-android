package com.syzygyhub.core.logging

import com.syzygyhub.foundation.contracts.logging.LoggerProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoggerTest {
    private class TestDestination : LogDestination {
        val messages = mutableListOf<Triple<String, LogLevel, Map<String, String>>>()

        override fun write(
            message: String,
            level: LogLevel,
            metadata: Map<String, String>,
        ) {
            messages.add(Triple(message, level, metadata))
        }
    }

    @Test
    fun `log level ordering`() {
        assertTrue(LogLevel.VERBOSE.ordinal < LogLevel.CRITICAL.ordinal)
        assertTrue(LogLevel.DEBUG.ordinal < LogLevel.ERROR.ordinal)
    }

    @Test
    fun `logger routes to destination`() {
        val dest = TestDestination()
        val logger = Logger()
        logger.addDestination(dest)
        logger.log(LogLevel.INFO, "hello")
        assertEquals(1, dest.messages.size)
        assertEquals("hello", dest.messages[0].first)
    }

    @Test
    fun `minimum level filters lower severity`() {
        val dest = TestDestination()
        val logger = Logger()
        logger.addDestination(dest, minLevel = LogLevel.WARNING)
        logger.log(LogLevel.DEBUG, "should be filtered")
        logger.log(LogLevel.WARNING, "should pass")
        logger.log(LogLevel.ERROR, "should also pass")
        assertEquals(2, dest.messages.size)
    }

    @Test
    fun `metadata is forwarded`() {
        val dest = TestDestination()
        val logger = Logger()
        logger.addDestination(dest)
        logger.log(LogLevel.INFO, "msg", mapOf("key" to "value"))
        assertEquals(mapOf("key" to "value"), dest.messages[0].third)
    }

    @Test
    fun `console destination does not throw`() {
        val dest = ConsoleLogDestination()
        dest.write("test", LogLevel.INFO, emptyMap())
    }

    @Test
    fun `Logger implements LoggerProtocol`() {
        // Compile-time conformance check: Logger must be assignable to LoggerProtocol
        val logger = Logger()
        assertIs<LoggerProtocol>(logger)
    }

    @Test
    fun `LoggerProtocol log entry is dispatched through destinations`() {
        val dest = TestDestination()
        val logger = Logger()
        logger.addDestination(dest)
        // Call via Foundation's LoggerProtocol convenience method
        (logger as LoggerProtocol).info("via protocol")
        assertEquals(1, dest.messages.size)
        assertEquals("via protocol", dest.messages[0].first)
        assertEquals(LogLevel.INFO, dest.messages[0].second)
    }
}
