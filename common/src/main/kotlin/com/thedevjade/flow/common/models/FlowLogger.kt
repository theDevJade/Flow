package com.thedevjade.flow.common.models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

object FlowLogger {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val logLevels = ConcurrentHashMap<String, LogLevel>()

    enum class LogLevel(val value: Int) {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3)
    }

    var globalLogLevel: LogLevel = LogLevel.INFO

    /**
     * Strategy interface for logging outputs
     */
    fun interface LogHandler {
        fun log(level: LogLevel, component: String, message: String, throwable: Throwable?)
    }

    /**
     * Default logger just prints to stdout
     */
    var handler: LogHandler = LogHandler { level, component, message, throwable ->
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val levelStr = level.name.padEnd(5)
        val componentStr = component.padEnd(20)

        val output = buildString {
            append("[$timestamp] $levelStr [$componentStr] $message")
            if (throwable != null) {
                append(" - ${throwable.javaClass.simpleName}: ${throwable.message}")
            }
        }

        println(output)
    }

    fun setLogLevel(component: String, level: LogLevel) {
        logLevels[component] = level
    }

    private fun getLogLevel(component: String): LogLevel =
        logLevels[component] ?: globalLogLevel

    private fun isLevelEnabled(component: String, level: LogLevel): Boolean =
        level.value >= getLogLevel(component).value

    private fun log(component: String, level: LogLevel, message: String, throwable: Throwable? = null) {
        if (isLevelEnabled(component, level)) {
            handler.log(level, component, message, throwable)
        }
    }

    fun debug(message: String, component: String = "WEBSOCKET", throwable: Throwable? = null) =
        log(component, LogLevel.DEBUG, message, throwable)

    fun info(component: String, message: String, throwable: Throwable? = null) =
        log(component, LogLevel.INFO, message, throwable)

    fun warn(component: String, message: String, throwable: Throwable? = null) =
        log(component, LogLevel.WARN, message, throwable)

    fun error(component: String, message: String, throwable: Throwable? = null) =
        log(component, LogLevel.ERROR, message, throwable)


    // ===== Namespaced Loggers (unchanged usage) =====
    object Database {
        private const val COMPONENT = "DATABASE"
        fun debug(message: String, throwable: Throwable? = null) = FlowLogger.debug(COMPONENT, message, throwable)
        fun info(message: String, throwable: Throwable? = null) = FlowLogger.info(COMPONENT, message, throwable)
        fun warn(message: String, throwable: Throwable? = null) = FlowLogger.warn(COMPONENT, message, throwable)
        fun error(message: String, throwable: Throwable? = null) = FlowLogger.error(COMPONENT, message, throwable)
    }

    object WebSocket {
        private const val COMPONENT = "WEBSOCKET"
        fun debug(message: String, throwable: Throwable? = null) = FlowLogger.debug(COMPONENT, message, throwable)
        fun info(message: String, throwable: Throwable? = null) = FlowLogger.info(COMPONENT, message, throwable)
        fun warn(message: String, throwable: Throwable? = null) = FlowLogger.warn(COMPONENT, message, throwable)
        fun error(message: String, throwable: Throwable? = null) = FlowLogger.error(COMPONENT, message, throwable)
    }

    object API {
        private const val COMPONENT = "API"
        fun debug(message: String, throwable: Throwable? = null) = FlowLogger.debug(COMPONENT, message, throwable)
        fun info(message: String, throwable: Throwable? = null) = FlowLogger.info(COMPONENT, message, throwable)
        fun warn(message: String, throwable: Throwable? = null) = FlowLogger.warn(COMPONENT, message, throwable)
        fun error(message: String, throwable: Throwable? = null) = FlowLogger.error(COMPONENT, message, throwable)
    }

    object Auth {
        private const val COMPONENT = "AUTH"
        fun debug(message: String, throwable: Throwable? = null) = FlowLogger.debug(COMPONENT, message, throwable)
        fun info(message: String, throwable: Throwable? = null) = FlowLogger.info(COMPONENT, message, throwable)
        fun warn(message: String, throwable: Throwable? = null) = FlowLogger.warn(COMPONENT, message, throwable)
        fun error(message: String, throwable: Throwable? = null) = FlowLogger.error(COMPONENT, message, throwable)
    }

    object GraphSync {
        private const val COMPONENT = "GRAPH_SYNC"
        fun debug(message: String, throwable: Throwable? = null) = FlowLogger.debug(COMPONENT, message, throwable)
        fun info(message: String, throwable: Throwable? = null) = FlowLogger.info(COMPONENT, message, throwable)
        fun warn(message: String, throwable: Throwable? = null) = FlowLogger.warn(COMPONENT, message, throwable)
        fun error(message: String, throwable: Throwable? = null) = FlowLogger.error(COMPONENT, message, throwable)
    }
}
