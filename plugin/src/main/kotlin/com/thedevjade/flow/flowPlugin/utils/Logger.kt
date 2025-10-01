package com.thedevjade.flow.flowPlugin.utils

import com.thedevjade.flow.common.config.FlowConfiguration
import com.thedevjade.flow.common.models.FlowLogger

class Logger : FlowLogger.LogHandler {
    override fun log(
        level: FlowLogger.LogLevel,
        component: String,
        message: String,
        throwable: Throwable?
    ) {
        val logMessage = "[$level][$component] $message"

        when (level) {
            FlowLogger.LogLevel.DEBUG -> {
                if (FlowConfiguration.webserverConfig.debugLog == true) {
                    if (throwable != null) {
                        info("$logMessage - ${throwable.javaClass.simpleName}: ${throwable.message}")
                    } else {
                        info(logMessage)
                    }
                }
            }

            FlowLogger.LogLevel.INFO -> {
                if (throwable != null) {
                    info("$logMessage - ${throwable.javaClass.simpleName}: ${throwable.message}")
                } else {
                    info(logMessage)
                }
            }

            FlowLogger.LogLevel.WARN -> {
                if (throwable != null) {
                    warning("$logMessage - ${throwable.javaClass.simpleName}: ${throwable.message}")
                } else {
                    warning(logMessage)
                }
            }

            FlowLogger.LogLevel.ERROR -> {
                if (throwable != null) {
                    error("$logMessage - ${throwable.javaClass.simpleName}: ${throwable.message}")
                } else {
                    error(logMessage)
                }
            }
        }
    }
}