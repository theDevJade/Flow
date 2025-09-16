package com.thedevjade.flow.api

import com.thedevjade.flow.api.events.EventManager
import com.thedevjade.flow.api.graph.GraphManager
import com.thedevjade.flow.api.user.UserManager
import com.thedevjade.flow.api.websocket.WebSocketManager
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Core Flow API that provides comprehensive access to all system components.
 * This is the main entry point for all Flow operations including user management,
 * graph operations, event handling, and WebSocket communication.
 */
class FlowCore private constructor() {
    
    companion object {
        @Volatile
        private var instance: FlowCore? = null
        
        /**
         * Get the singleton instance of FlowCore
         */
        fun getInstance(): FlowCore {
            return instance ?: synchronized(this) {
                instance ?: FlowCore().also { instance = it }
            }
        }
        
        /**
         * Initialize the Flow system with configuration
         */
        fun initialize(config: FlowConfig = FlowConfig()): FlowCore {
            val core = getInstance()
            core.initialize(config)
            return core
        }
    }
    
    // Core application scope for all Flow operations
    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Core managers
    private lateinit var _userManager: UserManager
    private lateinit var _graphManager: GraphManager
    private lateinit var _eventManager: EventManager
    private lateinit var _webSocketManager: WebSocketManager
    
    // Configuration
    private lateinit var config: FlowConfig
    
    // State tracking
    private var isInitialized = false
    private val plugins = ConcurrentHashMap<String, FlowPlugin>()
    
    /**
     * Initialize the Flow system
     */
    private fun initialize(config: FlowConfig) {
        if (isInitialized) return
        
        this.config = config
        
        // Initialize core managers
        _eventManager = EventManager(applicationScope)
        _userManager = UserManager(_eventManager, config)
        _graphManager = GraphManager(_eventManager, config)
        _webSocketManager = WebSocketManager(_eventManager, _userManager, _graphManager, config)
        
        isInitialized = true
        
        // Emit initialization event
        applicationScope.launch {
            _eventManager.emit(SystemEvent.SystemInitialized(System.currentTimeMillis()))
        }
    }
    
    /**
     * User management operations
     */
    val users: UserManager
        get() {
            checkInitialized()
            return _userManager
        }
    
    /**
     * Graph management operations
     */
    val graphs: GraphManager
        get() {
            checkInitialized()
            return _graphManager
        }
    
    /**
     * Event management operations
     */
    val events: EventManager
        get() {
            checkInitialized()
            return _eventManager
        }
    
    /**
     * WebSocket communication operations
     */
    val websockets: WebSocketManager
        get() {
            checkInitialized()
            return _webSocketManager
        }
    
    /**
     * Register a plugin with the Flow system
     */
    fun registerPlugin(plugin: FlowPlugin) {
        checkInitialized()
        plugins[plugin.name] = plugin
        plugin.initialize(this)
        applicationScope.launch {
            _eventManager.emit(SystemEvent.PluginRegistered(plugin.name, System.currentTimeMillis()))
        }
    }
    
    /**
     * Unregister a plugin
     */
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
    
    /**
     * Get registered plugin by name
     */
    fun getPlugin(name: String): FlowPlugin? {
        checkInitialized()
        return plugins[name]
    }
    
    /**
     * Get all registered plugins
     */
    fun getAllPlugins(): Map<String, FlowPlugin> {
        checkInitialized()
        return plugins.toMap()
    }
    
    /**
     * Get system status and metrics
     */
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
    
    /**
     * Shutdown the Flow system gracefully
     */
    suspend fun shutdown() {
        if (!isInitialized) return
        
        applicationScope.launch {
            _eventManager.emit(SystemEvent.SystemShuttingDown(System.currentTimeMillis()))
        }.join()
        
        // Shutdown plugins
        plugins.values.forEach { it.dispose() }
        plugins.clear()
        
        // Shutdown managers in reverse order
        _webSocketManager.dispose()
        _graphManager.dispose()
        _userManager.dispose()
        _eventManager.dispose()
        
        isInitialized = false
        
        // Cancel application scope
        applicationScope.cancel()
    }
    
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("FlowCore must be initialized before use. Call FlowCore.initialize() first.")
        }
    }
}

/**
 * Configuration for the Flow system
 */
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

/**
 * Base interface for Flow plugins
 */
interface FlowPlugin {
    val name: String
    val version: String
    
    fun initialize(flowCore: FlowCore)
    fun dispose()
}

/**
 * System status information
 */
data class SystemStatus(
    val isInitialized: Boolean,
    val uptime: Long,
    val activeUsers: Int,
    val totalGraphs: Int,
    val activeConnections: Int,
    val registeredPlugins: Int,
    val memoryUsage: Long
)

/**
 * System events
 */
sealed class SystemEvent {
    data class SystemInitialized(val timestamp: Long) : SystemEvent()
    data class SystemShuttingDown(val timestamp: Long) : SystemEvent()
    data class PluginRegistered(val pluginName: String, val timestamp: Long) : SystemEvent()
    data class PluginUnregistered(val pluginName: String, val timestamp: Long) : SystemEvent()
}