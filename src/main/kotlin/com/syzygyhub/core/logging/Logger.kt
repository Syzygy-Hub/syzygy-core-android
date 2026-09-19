package com.syzygyhub.core.logging

import com.syzygyhub.foundation.contracts.logging.LogEntry
import com.syzygyhub.foundation.contracts.logging.LogLevel
import com.syzygyhub.foundation.contracts.logging.LoggerProtocol
import com.syzygyhub.foundation.primitives.time.SyzygyTimestamp

// TODO(v1.2.0): align verbose case with Foundation — pending Foundation 1.2.0

/**
 * Core-internal severity level that extends Foundation with [VERBOSE].
 *
 * Not part of the public API; consumers use Foundation's [LogLevel]
 * (imported above).  Retained here so internal code that distinguishes
 * the verbose tier can remain until Foundation 1.2.0 adds parity.
 */
internal enum class CoreLogLevel {
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
     * Writes a log [message] at the given [level] with optional [metadata],
     * [timestamp], and [error].
     *
     * The [timestamp] and [error] parameters are optional and default to null for
     * backward compatibility with existing implementations that do not use them.
     */
    fun write(
        level: LogLevel,
        message: String,
        metadata: Map<String, String>,
        timestamp: SyzygyTimestamp? = null,
        error: Throwable? = null,
    )
}

/**
 * Console log destination that prints to stdout in a human-readable format.
 */
class ConsoleLogDestination : LogDestination {
    override fun write(
        level: LogLevel,
        message: String,
        metadata: Map<String, String>,
        timestamp: SyzygyTimestamp?,
        error: Throwable?,
    ) {
        val ts = if (timestamp != null) " @${timestamp.millisecondsSinceEpoch}" else ""
        val meta = if (metadata.isEmpty()) "" else " $metadata"
        println("[${level.name}]$ts $message$meta")
        if (error != null) {
            println("[${level.name}] Exception: ${error.message}")
            error.printStackTrace(System.out)
        }
    }
}

/**
 * Logger that routes messages through registered [LogDestination] instances,
 * each gated by a minimum log level.
 *
 * Implements [LoggerProtocol] from Foundation so that Core's logger can be
 * supplied wherever a Foundation-typed logger is expected.  Because [LogLevel]
 * is now Foundation's type directly, no level translation is required when
 * receiving entries via [LoggerProtocol.log].
 */
class Logger : LoggerProtocol {
    private data class DestinationEntry(
        val destination: LogDestination,
        val minLevel: LogLevel,
    )

    private val destinations = mutableListOf<DestinationEntry>()

    /**
     * Adds a [destination] that will receive messages at or above [minLevel].
     * Defaults to [LogLevel.DEBUG], the most permissive Foundation level.
     */
    fun addDestination(
        destination: LogDestination,
        minLevel: LogLevel = LogLevel.DEBUG,
    ) {
        destinations.add(DestinationEntry(destination, minLevel))
    }

    /**
     * Logs a [message] at the given [level] with optional [metadata], [timestamp],
     * and [error]. The message is forwarded to all destinations whose minimum level is met.
     */
    fun log(
        level: LogLevel,
        message: String,
        metadata: Map<String, String> = emptyMap(),
        timestamp: SyzygyTimestamp? = null,
        error: Throwable? = null,
    ) {
        for (entry in destinations) {
            if (level >= entry.minLevel) {
                entry.destination.write(level, message, metadata, timestamp, error)
            }
        }
    }

    // -------------------------------------------------------------------------
    // LoggerProtocol implementation
    // -------------------------------------------------------------------------

    /**
     * Satisfies [LoggerProtocol.log] by forwarding the entry directly.
     * [LogLevel] is Foundation's type so no mapping is required.
     */
    override fun log(entry: LogEntry) {
        log(entry.level, entry.message, entry.metadata, entry.timestamp, entry.error)
    }
}
