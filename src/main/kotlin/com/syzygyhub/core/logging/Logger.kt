package com.syzygyhub.core.logging

import com.syzygyhub.foundation.contracts.logging.LogEntry
import com.syzygyhub.foundation.contracts.logging.LoggerProtocol
import com.syzygyhub.foundation.contracts.logging.LogLevel as FoundationLogLevel

/**
 * Severity level for log messages, ordered from least to most severe.
 *
 * [VERBOSE] is a Core-only extension; it maps to [FoundationLogLevel.DEBUG]
 * when satisfying the [LoggerProtocol] contract.
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL,
}

/**
 * A destination that receives formatted log messages with metadata.
 */
interface LogDestination {
    /**
     * Writes a log [message] at the given [level] with optional [metadata].
     */
    fun write(
        message: String,
        level: LogLevel,
        metadata: Map<String, String>,
    )
}

/**
 * Console log destination that prints to stdout in a human-readable format.
 */
class ConsoleLogDestination : LogDestination {
    override fun write(
        message: String,
        level: LogLevel,
        metadata: Map<String, String>,
    ) {
        val meta = if (metadata.isEmpty()) "" else " $metadata"
        println("[${level.name}] $message$meta")
    }
}

/**
 * Logger that routes messages through registered [LogDestination] instances,
 * each gated by a minimum log level.
 *
 * Implements [LoggerProtocol] from Foundation so that Core's logger can be
 * supplied wherever a Foundation-typed logger is expected.  Foundation's
 * [LogLevel] has no [LogLevel.VERBOSE] entry; calls arriving via
 * [LoggerProtocol.log] with [FoundationLogLevel.DEBUG] are dispatched as
 * [LogLevel.DEBUG] internally.
 */
class Logger : LoggerProtocol {
    private data class DestinationEntry(
        val destination: LogDestination,
        val minLevel: LogLevel,
    )

    private val destinations = mutableListOf<DestinationEntry>()

    /**
     * Adds a [destination] that will receive messages at or above [minLevel].
     */
    fun addDestination(
        destination: LogDestination,
        minLevel: LogLevel = LogLevel.VERBOSE,
    ) {
        destinations.add(DestinationEntry(destination, minLevel))
    }

    /**
     * Logs a [message] at the given [level] with optional [metadata].
     * The message is forwarded to all destinations whose minimum level is met.
     */
    fun log(
        level: LogLevel,
        message: String,
        metadata: Map<String, String> = emptyMap(),
    ) {
        for (entry in destinations) {
            if (level.ordinal >= entry.minLevel.ordinal) {
                entry.destination.write(message, level, metadata)
            }
        }
    }

    // -------------------------------------------------------------------------
    // LoggerProtocol implementation
    // -------------------------------------------------------------------------

    /**
     * Satisfies [LoggerProtocol.log] by mapping Foundation [LogLevel] values
     * to Core [LogLevel] values.  [FoundationLogLevel.DEBUG] maps to
     * [LogLevel.DEBUG] (VERBOSE is a Core-only concept with no Foundation
     * equivalent).
     */
    override fun log(entry: LogEntry) {
        val coreLevel =
            when (entry.level) {
                FoundationLogLevel.DEBUG -> LogLevel.DEBUG
                FoundationLogLevel.INFO -> LogLevel.INFO
                FoundationLogLevel.WARNING -> LogLevel.WARNING
                FoundationLogLevel.ERROR -> LogLevel.ERROR
                FoundationLogLevel.CRITICAL -> LogLevel.CRITICAL
            }
        log(coreLevel, entry.message, entry.metadata)
    }
}
