package com.thedevjade.flow.api

import com.thedevjade.flow.api.events.EventManager
import com.thedevjade.flow.api.graph.GraphManager
import com.thedevjade.flow.api.user.UserManager
import com.thedevjade.flow.api.websocket.WebSocketManager
import com.thedevjade.flow.extension.ExtensionManager
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap


class FlowCore private constructor() {

    companion object {
        @Volatile
        private var instance: FlowCore? = null


        fun getInstance(): FlowCore {
            return instance ?: synchronized(this) {
                instance ?: FlowCore().also { instance = it }
            }
        }


        fun initialize(config: FlowConfig = FlowConfig()): FlowCore {
            val core = getInstance()
            core.initialize(config)
            return core
        }
    }


    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    private lateinit var _userManager: UserManager
    private lateinit var _graphManager: GraphManager
    private lateinit var _eventManager: EventManager
    private lateinit var _webSocketManager: WebSocketManager
    private lateinit var _extensionManager: ExtensionManager


    private lateinit var config: FlowConfig


    private var isInitialized = false
    private val plugins = ConcurrentHashMap<String, FlowPlugin>()


    private fun initialize(config: FlowConfig) {
        if (isInitialized) return

        this.config = config


        _eventManager = EventManager(applicationScope)
        _userManager = UserManager(_eventManager, config)
        _graphManager = GraphManager(_eventManager, config)
        _webSocketManager = WebSocketManager(_eventManager, _userManager, _graphManager, config)
        _extensionManager = ExtensionManager(this, config.dataDirectory.let { File(it) })

        isInitialized = true


        applicationScope.launch {
            _extensionManager.initialize()
            _eventManager.emit(SystemEvent.SystemInitialized(System.currentTimeMillis()))
        }
    }


    val users: UserManager
        get() {
            checkInitialized()
            return _userManager
        }


    val graphs: GraphManager
        get() {
            checkInitialized()
            return _graphManager
        }


    val events: EventManager
        get() {
            checkInitialized()
            return _eventManager
        }


    val websockets: WebSocketManager
        get() {
            checkInitialized()
            return _webSocketManager
        }


    val extensions: ExtensionManager
        get() {
            checkInitialized()
            return _extensionManager
        }


    fun registerPlugin(plugin: FlowPlugin) {
        checkInitialized()
        plugins[plugin.name] = plugin
        plugin.initialize(this)
        applicationScope.launch {
            _eventManager.emit(SystemEvent.PluginRegistered(plugin.name, System.currentTimeMillis()))
        }
    }


    fun unregisterPlugin(pluginName: String) {
        checkInitialized()
        plugins[pluginName]?.let { plugin ->
            plugin.dispose()
            plugins.remove(pluginName)
            applicationScope.launch {
                _eventManager.emit(SystemEvent.PluginUnregistered(pluginName, System.currentTimeMillis()))
            }
        }
    }


    fun getPlugin(name: String): FlowPlugin? {
        checkInitialized()
        return plugins[name]
    }


    fun getAllPlugins(): Map<String, FlowPlugin> {
        checkInitialized()
        return plugins.toMap()
    }


    fun getSystemStatus(): SystemStatus {
        checkInitialized()
        return SystemStatus(
            isInitialized = isInitialized,
            uptime = System.currentTimeMillis() - config.startTime,
            activeUsers = _userManager.getActiveUserCount(),
            totalGraphs = _graphManager.getTotalGraphCount(),
            activeConnections = _webSocketManager.getActiveConnectionCount(),
            registeredPlugins = plugins.size,
            memoryUsage = Runtime.getRuntime().let {
                it.totalMemory() - it.freeMemory()
            }
        )
    }


    suspend fun shutdown() {
        if (!isInitialized) return

        applicationScope.launch {
            _eventManager.emit(SystemEvent.SystemShuttingDown(System.currentTimeMillis()))
        }.join()


        plugins.values.forEach { it.dispose() }
        plugins.clear()


        _extensionManager.dispose()
        _webSocketManager.dispose()
        _graphManager.dispose()
        _userManager.dispose()
        _eventManager.dispose()

        isInitialized = false


        applicationScope.cancel()
    }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("FlowCore must be initialized before use. Call FlowCore.initialize() first.")
        }
    }
}


data class FlowConfig(
    val dataDirectory: String = "data",
    val maxUsers: Int = 1000,
    val maxGraphsPerUser: Int = 100,
    val webSocketPort: Int = 9090,
    val enableMetrics: Boolean = true,
    val enableLogging: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val startTime: Long = System.currentTimeMillis()
) {
    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}


interface FlowPlugin {
    val name: String
    val version: String

    fun initialize(flowCore: FlowCore)
    fun dispose()
}


data class SystemStatus(
    val isInitialized: Boolean,
    val uptime: Long,
    val activeUsers: Int,
    val totalGraphs: Int,
    val activeConnections: Int,
    val registeredPlugins: Int,
    val memoryUsage: Long
)


sealed class SystemEvent {
    data class SystemInitialized(val timestamp: Long) : SystemEvent()
    data class SystemShuttingDown(val timestamp: Long) : SystemEvent()
    data class PluginRegistered(val pluginName: String, val timestamp: Long) : SystemEvent()
    data class PluginUnregistered(val pluginName: String, val timestamp: Long) : SystemEvent()
}