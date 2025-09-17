package com.thedevjade.flow.webserver

import com.thedevjade.flow.webserver.database.DatabaseManager
import com.thedevjade.flow.webserver.logging.FlowLogger
import config.FlowConfiguration
import io.ktor.server.application.*
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    FlowLogger.info("MAIN", "Starting Flow WebSocket Server...")

    // Initialize database
    try {
        DatabaseManager.initialize()
        FlowLogger.info("MAIN", "Database initialized successfully")
    } catch (e: Exception) {
        FlowLogger.error("MAIN", "Failed to initialize database", e)
        return
    }

    // Start server using Application.kt configuration
    FlowWebserver.runSockets()
    FlowLogger.info("MAIN", "WebSocket server will be available at ws://${FlowConfiguration.webserverConfig.hostName}:${FlowConfiguration.webserverConfig.websocketPort}/ws")
}