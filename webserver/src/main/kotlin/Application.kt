package com.thedevjade.flow.webserver

import com.thedevjade.flow.webserver.database.DatabaseManager
import config.FlowConfiguration
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

object FlowWebserver {
    fun run() {
        // Initialize database first
        DatabaseManager.initialize()
        
        embeddedServer(Netty, configure = {
            connectors.add(EngineConnectorBuilder().apply {
                host = FlowConfiguration.webserverConfig.hostName!!
                port = FlowConfiguration.webserverConfig.webserverPort!!
            })
            connectionGroupSize = 2
            workerGroupSize = 5
            callGroupSize = 10
            shutdownGracePeriod = 2000
            shutdownTimeout = 3000
        }) {
            module()
        }.start(wait = true)
    }

    fun runSockets() {
        // Initialize database first
        DatabaseManager.initialize()
        
        embeddedServer(Netty, configure = {
            connectors.add(EngineConnectorBuilder().apply {
                host = FlowConfiguration.webserverConfig.hostName!!
                port = FlowConfiguration.webserverConfig.websocketPort!!
            })
            connectionGroupSize = 2
            workerGroupSize = 5
            callGroupSize = 10
            shutdownGracePeriod = 2000
            shutdownTimeout = 3000
        }) {
            moduleSocket()
        }.start(wait = true)
    }
}

fun Application.module() {

    configureRouting()
}

fun Application.moduleSocket() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    
    // Initialize database first
    println("Initializing database...")
    com.thedevjade.flow.webserver.database.DatabaseManager.initialize()
    
    // Initialize auth service after database is ready
    val authService = AuthService()
    attributes.put(authServiceKey, authService)
    
    // Configure auth routes
    configureAuthRoutes(authService)
    
    // Configure sockets with auth service
    configureSockets(authService)
}
