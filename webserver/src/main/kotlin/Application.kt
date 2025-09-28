package com.thedevjade.flow.webserver

import com.thedevjade.flow.common.config.FlowConfiguration
import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.webserver.database.DatabaseManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

object FlowWebserver {
    var mainServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    var socketServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    fun killAll() {
        try {
            mainServer?.stop(2000, 3000)
            mainServer = null
        } catch (e: Exception) {

        }

        try {
            socketServer?.stop(2000, 3000)
            socketServer = null
        } catch (e: Exception) {

        }
    }

    fun run() {

        DatabaseManager.initialize()

        mainServer = embeddedServer(Netty, configure = {
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

        DatabaseManager.initialize()

        socketServer = embeddedServer(Netty, configure = {
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


    FlowLogger.debug("Initializing database...")
    DatabaseManager.initialize()


    val authService = AuthService()
    attributes.put(authServiceKey, authService)


    configureAuthRoutes(authService)


    configureSockets(authService)
}
