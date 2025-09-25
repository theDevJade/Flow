package com.thedevjade.flow.webserver

import com.thedevjade.flow.common.config.FlowConfiguration
import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.webserver.database.DatabaseManager

fun main() {
    FlowLogger.info("MAIN", "Starting Flow WebSocket Server...")


    try {
        DatabaseManager.initialize()
        FlowLogger.info("MAIN", "Database initialized successfully")
    } catch (e: Exception) {
        FlowLogger.error("MAIN", "Failed to initialize database", e)
        return
    }


    FlowWebserver.runSockets()
    FlowLogger.info(
        "MAIN",
        "WebSocket server will be available at ws://${FlowConfiguration.webserverConfig.hostName}:${FlowConfiguration.webserverConfig.websocketPort}/ws"
    )
}

object WebSocketCaller {
    fun run() {
        main()
    }
}