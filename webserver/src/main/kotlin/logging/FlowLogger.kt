package com.thedevjade.flow.webserver.logging

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


    fun setLogLevel(component: String, level: LogLevel) {
        logLevels[component] = level
    }


    private fun getLogLevel(component: String): LogLevel {
        return logLevels[component] ?: globalLogLevel
    }


    private fun isLevelEnabled(component: String, level: LogLevel): Boolean {
        return level.value >= getLogLevel(component).value
    }


    private fun formatMessage(component: String, level: LogLevel, message: String, throwable: Throwable? = null): String {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val levelStr = level.name.padEnd(5)
        val componentStr = component.padEnd(20)

        return buildString {
            append("[$timestamp] $levelStr [$componentStr] $message")
            if (throwable != null) {
                append(" - ${throwable.javaClass.simpleName}: ${throwable.message}")
            }
        }
    }


    fun debug(component: String, message: String, throwable: Throwable? = null) {
        if (isLevelEnabled(component, LogLevel.DEBUG)) {
            println(formatMessage(component, LogLevel.DEBUG, message, throwable))
        }
    }


    fun info(component: String, message: String, throwable: Throwable? = null) {
        if (isLevelEnabled(component, LogLevel.INFO)) {
            println(formatMessage(component, LogLevel.INFO, message, throwable))
        }
    }


    fun warn(component: String, message: String, throwable: Throwable? = null) {
        if (isLevelEnabled(component, LogLevel.WARN)) {
            println(formatMessage(component, LogLevel.WARN, message, throwable))
        }
    }


    fun error(component: String, message: String, throwable: Throwable? = null) {
        if (isLevelEnabled(component, LogLevel.ERROR)) {
            println(formatMessage(component, LogLevel.ERROR, message, throwable))
        }
    }



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