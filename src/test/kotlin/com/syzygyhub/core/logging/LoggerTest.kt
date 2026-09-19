package com.syzygyhub.core.logging

import com.syzygyhub.foundation.contracts.logging.LogEntry
import com.syzygyhub.foundation.contracts.logging.LogLevel
import com.syzygyhub.foundation.contracts.logging.LoggerProtocol
import com.syzygyhub.foundation.primitives.time.SyzygyTimestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoggerTest {
    private data class CapturedWrite(
        val level: LogLevel,
        val message: String,
        val metadata: Map<String, String>,
        val timestamp: SyzygyTimestamp?,
        val error: Throwable?,
    )

    private class TestDestination : LogDestination {
        val writes = mutableListOf<CapturedWrite>()

        // Keep legacy accessor for existing tests that reference by position.
        val messages: List<Triple<String, LogLevel, Map<String, String>>>
            get() = writes.map { Triple(it.message, it.level, it.metadata) }

        override fun write(
            level: LogLevel,
            message: String,
            metadata: Map<String, String>,
            timestamp: SyzygyTimestamp?,
            error: Throwable?,
        ) {
            writes.add(CapturedWrite(level, message, metadata, timestamp, error))
        }
    }

    @Test
    fun `log level ordering`() {
        // CoreLogLevel ordering includes the Core-only VERBOSE tier
        assertTrue(CoreLogLevel.VERBOSE.ordinal < CoreLogLevel.CRITICAL.ordinal)
        // Foundation LogLevel ordering (public type, no VERBOSE)
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
        dest.write(LogLevel.INFO, "test", emptyMap())
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

    // -------------------------------------------------------------------------
    // FIX 8 — Foundation LogEntry bridge tests
    // -------------------------------------------------------------------------

    private fun makeEntry(
        level: LogLevel,
        message: String = "msg",
        metadata: Map<String, String> = emptyMap(),
        timestamp: SyzygyTimestamp = SyzygyTimestamp(1_000L),
        error: Throwable? = null,
    ) = LogEntry(level, message, timestamp, metadata, error)

    @Test
    fun `Foundation DEBUG maps to Core DEBUG`() {
        val dest = TestDestination()
        val logger = Logger().also { it.addDestination(dest) }
        logger.log(makeEntry(LogLevel.DEBUG))
        assertEquals(LogLevel.DEBUG, dest.writes.single().level)
    }

    @Test
    fun `Foundation INFO maps to Core INFO`() {
        val dest = TestDestination()
        val logger = Logger().also { it.addDestination(dest) }
        logger.log(makeEntry(LogLevel.INFO))
        assertEquals(LogLevel.INFO, dest.writes.single().level)
    }

    @Test
    fun `Foundation WARNING maps to Core WARNING`() {
        val dest = TestDestination()
        val logger = Logger().also { it.addDestination(dest) }
        logger.log(makeEntry(LogLevel.WARNING))
        assertEquals(LogLevel.WARNING, dest.writes.single().level)
    }

    @Test
    fun `Foundation ERROR maps to Core ERROR`() {
        val dest = TestDestination()
        val logger = Logger().also { it.addDestination(dest) }
        logger.log(makeEntry(LogLevel.ERROR))
        assertEquals(LogLevel.ERROR, dest.writes.single().level)
    }

    @Test
    fun `Foundation CRITICAL maps to Core CRITICAL`() {
        val dest = TestDestination()
        val logger = Logger().also { it.addDestination(dest) }
        logger.log(makeEntry(LogLevel.CRITICAL))
        assertEquals(LogLevel.CRITICAL, dest.writes.single().level)
    }

    @Test
    fun `metadata forwarded via LogEntry`() {
        val dest = TestDestination()
        val logger = Logger().also { it.addDestination(dest) }
        logger.log(makeEntry(LogLevel.INFO, metadata = mapOf("k" to "v")))
        assertEquals(mapOf("k" to "v"), dest.writes.single().metadata)
    }

    @Test
    fun `timestamp forwarded via LogEntry`() {
        val dest = TestDestination()
        val logger = Logger().also { it.addDestination(dest) }
        val ts = SyzygyTimestamp(42_000L)
        logger.log(makeEntry(LogLevel.INFO, timestamp = ts))
        assertEquals(ts, dest.writes.single().timestamp)
    }

    @Test
    fun `error forwarded via LogEntry`() {
        val dest = TestDestination()
        val logger = Logger().also { it.addDestination(dest) }
        val ex = RuntimeException("boom")
        logger.log(makeEntry(LogLevel.ERROR, error = ex))
        assertNotNull(dest.writes.single().error)
        assertEquals("boom", dest.writes.single().error!!.message)
    }
}
