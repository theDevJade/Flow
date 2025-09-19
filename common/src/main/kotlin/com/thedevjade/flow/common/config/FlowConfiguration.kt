package com.thedevjade.flow.common.config

object FlowConfiguration {
    var webserverConfig = WebserverConfig()
    var databaseConfig = DatabaseConfig()

    class WebserverConfig {
        var websocketPort: Int? = 9090
        var webserverPort: Int? = 8080
        var enableWebserver: Boolean? = true
        var enableWebsocket: Boolean? = true
        var hostName: String? = "0.0.0.0"
        var debugLog: Boolean? = true
    }

    class DatabaseConfig {
        var databasePath: String? = "./data/flow.db"
        var enableWAL: Boolean? = true
        var connectionPoolSize: Int? = 10
    }
}