package com.syzygyhub.core.logging

/**
 * Severity level for log messages.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    FATAL,
}

/**
 * A destination that receives formatted log messages.
 */
interface LogDestination {
    fun write(message: String, level: LogLevel)
}

/**
 * Console log destination that prints to stdout.
 */
class ConsoleLogDestination : LogDestination {
    override fun write(message: String, level: LogLevel) {
        println("[${level.name}] $message")
    }
}

/**
 * Logger that routes messages through a pipeline of destinations.
 */
class Logger {
    // TODO: destinations, formatters, pipeline routing, minimum level filtering
}
