package com.thedevjade.flow.flowPlugin.utils

import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.common.config.FlowConfiguration

class Logger : FlowLogger.LogHandler{
    override fun log(
        level: FlowLogger.LogLevel,
        component: String,
        message: String,
        throwable: Throwable?
    ) {
        when (level) {
            FlowLogger.LogLevel.DEBUG -> {
                if (FlowConfiguration.webserverConfig.debugLog == true) {
                    info("[$level][$component]")
                }
            }
            FlowLogger.LogLevel.INFO -> {
                info("[$level][$component]")

            }
            FlowLogger.LogLevel.WARN -> {
                warning("[$level][$component]")

            }
            FlowLogger.LogLevel.ERROR -> {
                error("[$level][$component]")

            }
        }
    }
}