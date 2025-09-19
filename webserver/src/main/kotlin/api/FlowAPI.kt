package com.thedevjade.flow.webserver.api

import com.thedevjade.flow.common.models.FlowLogger
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import com.thedevjade.flow.webserver.websocket.WebSocketMessage
import com.thedevjade.flow.webserver.websocket.WebSocketMessageHandler
import com.thedevjade.flow.webserver.websocket.WebSocketSessionManager
import com.thedevjade.flow.webserver.websocket.GraphDataManager
import com.thedevjade.flow.webserver.websocket.GraphSyncHandler
import flow.api.implementation.FileSystemAccessImpl
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


class FlowAPI private constructor(
    private val sessionManager: WebSocketSessionManager,
    private val dataManager: GraphDataManager,
    private val graphSyncHandler: GraphSyncHandler,
    private val fileAccessAccessImpl: FileSystemAccessImpl
) {
    companion object {
        @Volatile
        private var instance: FlowAPI? = null

        fun getInstance(
            sessionManager: WebSocketSessionManager,
            dataManager: GraphDataManager,
            graphSyncHandler: GraphSyncHandler,
            fileAccessAccessImpl: FileSystemAccessImpl
        ): FlowAPI {
            return instance ?: synchronized(this) {
                instance ?: FlowAPI(sessionManager, dataManager, graphSyncHandler, fileAccessAccessImpl).also { instance = it }
            }
        }

        fun getInstance(): FlowAPI {
            return instance ?: throw IllegalStateException("FlowAPI must be initialized with parameters first")
        }
    }


    private val activeSessions = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val messageHandler = WebSocketMessageHandler(sessionManager, dataManager, graphSyncHandler)
    private val commandIdCounter = AtomicLong(0)


    private val allowedDirectories = mutableSetOf<String>()
    private var rootAccessDirectory: String? = null


    private val customCommands = ConcurrentHashMap<String, CustomCommandHandler>()


    private val userSessions = ConcurrentHashMap<String, UserSession>()
    private val sessionListeners = mutableListOf<SessionEventListener>()




    fun registerSession(sessionId: String, session: DefaultWebSocketSession) {
        activeSessions[sessionId] = session
        FlowLogger.debug("FlowAPI: Registered session $sessionId")
    }


    fun unregisterSession(sessionId: String) {
        activeSessions.remove(sessionId)
        FlowLogger.debug("FlowAPI: Unregistered session $sessionId")
    }

    fun getFileManager(): FileSystemAccessImpl {
        return fileAccessAccessImpl
    }


    @OptIn(DelicateCoroutinesApi::class)
    suspend fun sendMessageToSession(sessionId: String, message: WebSocketMessage): Boolean {
        val session = activeSessions[sessionId]
        return if (session != null && !session.outgoing.isClosedForSend) {
            try {
                session.send(Json.encodeToString(WebSocketMessage.serializer(), message))
                true
            } catch (e: Exception) {
                FlowLogger.debug("FlowAPI: Failed to send message to session $sessionId: ${e.message}")
                false
            }
        } else {
            false
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    suspend fun broadcastMessage(message: WebSocketMessage) {
        val messageJson = Json.encodeToString(WebSocketMessage.serializer(), message)
        activeSessions.values.forEach { session ->
            try {
                if (!session.outgoing.isClosedForSend) {
                    session.send(messageJson)
                }
            } catch (e: Exception) {
                FlowLogger.debug("FlowAPI: Failed to broadcast message: ${e.message}")
            }
        }
    }


    suspend fun sendCustomMessage(
        sessionId: String? = null,
        type: String,
        data: kotlinx.serialization.json.JsonObject,
        userId: String? = null
    ) {
        val message = WebSocketMessage(
            id = "cmd_${commandIdCounter.getAndIncrement()}",
            type = type,
            data = data,
            timestamp = java.time.Instant.now().toString(),
            userId = userId
        )

        if (sessionId != null) {
            sendMessageToSession(sessionId, message)
        } else {
            broadcastMessage(message)
        }
    }




    fun setFileExplorerRootDirectory(path: String): Boolean {
        val directory = File(path)
        return if (directory.exists() && directory.isDirectory) {
            rootAccessDirectory = directory.absolutePath
            FlowLogger.debug("FlowAPI: Set root access directory to: $path")
            true
        } else {
            FlowLogger.debug("FlowAPI: Invalid root directory: $path")
            false
        }
    }


    fun addAllowedDirectory(path: String): Boolean {
        val directory = File(path)
        return if (directory.exists() && directory.isDirectory) {
            val absolutePath = directory.absolutePath

            if (rootAccessDirectory != null && !absolutePath.startsWith(rootAccessDirectory!!)) {
                FlowLogger.debug("FlowAPI: Directory $path is outside root access directory")
                return false
            }
            allowedDirectories.add(absolutePath)
            FlowLogger.debug("FlowAPI: Added allowed directory: $path")
            true
        } else {
            FlowLogger.debug("FlowAPI: Invalid directory: $path")
            false
        }
    }


    fun removeAllowedDirectory(path: String) {
        val absolutePath = File(path).absolutePath
        allowedDirectories.remove(absolutePath)
        FlowLogger.debug("FlowAPI: Removed allowed directory: $path")
    }


    fun isFileAccessAllowed(filePath: String): Boolean {
        val absolutePath = File(filePath).absolutePath


        if (rootAccessDirectory != null && !absolutePath.startsWith(rootAccessDirectory!!)) {
            return false
        }


        return allowedDirectories.any { allowedDir ->
            absolutePath.startsWith(allowedDir)
        }
    }


    fun getAllowedDirectories(): List<String> = allowedDirectories.toList()


    fun getRootAccessDirectory(): String? = rootAccessDirectory




    fun registerCommand(handler: CustomCommandHandler): Boolean {
        return if (!customCommands.containsKey(handler.commandId)) {
            customCommands[handler.commandId] = handler
            FlowLogger.debug("FlowAPI: Registered command: ${handler.commandId} - ${handler.displayName}")
            true
        } else {
            FlowLogger.debug("FlowAPI: Command ${handler.commandId} already exists")
            false
        }
    }


    fun unregisterCommand(commandId: String): Boolean {
        return customCommands.remove(commandId) != null
    }


    suspend fun executeCommand(
        commandId: String,
        sessionId: String,
        userId: String?,
        data: JsonElement
    ): CommandResult {
        val handler = customCommands[commandId]
        return if (handler != null) {
            if (handler.requiresAuth && userId == null) {
                CommandResult.failure("Authentication required for command: $commandId", "AUTH_REQUIRED")
            } else {
                val validation = handler.validate(data)
                if (validation.isValid) {
                    try {
                        handler.execute(sessionId, userId, data)
                    } catch (e: Exception) {
                        FlowLogger.debug("FlowAPI: Error executing command $commandId: ${e.message}")
                        CommandResult.failure("Command execution failed: ${e.message}", "EXECUTION_ERROR")
                    }
                } else {
                    CommandResult.failure(
                        validation.errorMessage ?: "Validation failed",
                        "VALIDATION_ERROR",
                        kotlinx.serialization.json.buildJsonObject {
                            if (validation.missingFields.isNotEmpty()) {
                                put("missingFields", kotlinx.serialization.json.buildJsonArray {
                                    validation.missingFields.forEach { add(it) }
                                })
                            }
                        }
                    )
                }
            }
        } else {
            CommandResult.failure("Command not found: $commandId", "NOT_FOUND")
        }
    }


    fun getRegisteredCommands(): List<CustomCommandInfo> {
        return customCommands.values.map { handler ->
            CustomCommandInfo(
                commandId = handler.commandId,
                displayName = handler.displayName,
                description = handler.description,
                requiresAuth = handler.requiresAuth
            )
        }
    }




    suspend fun updateUserSession(
        sessionId: String,
        userId: String?,
        isAuthenticated: Boolean = false,
        metadata: Map<String, String> = emptyMap()
    ) {
        val now = System.currentTimeMillis()
        val existingSession = userSessions[sessionId]

        val session = if (existingSession != null) {
            existingSession.copy(
                userId = userId,
                isAuthenticated = isAuthenticated,
                lastActivity = now,
                metadata = metadata
            )
        } else {
            UserSession(
                sessionId = sessionId,
                userId = userId,
                isAuthenticated = isAuthenticated,
                connectedAt = now,
                lastActivity = now,
                metadata = metadata
            )
        }

        userSessions[sessionId] = session


        val event = if (existingSession == null) {
            SessionEvent.Connected(sessionId, userId)
        } else if (!existingSession.isAuthenticated && isAuthenticated) {
            SessionEvent.Authenticated(sessionId, userId!!)
        } else {
            SessionEvent.ActivityUpdate(sessionId, userId)
        }

        notifySessionEvent(event)
    }


    suspend fun removeUserSession(sessionId: String) {
        val session = userSessions.remove(sessionId)
        if (session != null) {
            notifySessionEvent(SessionEvent.Disconnected(sessionId, session.userId))
        }
    }


    fun getUserSession(sessionId: String): UserSession? = userSessions[sessionId]


    fun getAllUserSessions(): List<UserSession> = userSessions.values.toList()


    fun getUserSessions(userId: String): List<UserSession> {
        return userSessions.values.filter { it.userId == userId }
    }


    fun addSessionEventListener(listener: SessionEventListener) {
        sessionListeners.add(listener)
    }


    fun removeSessionEventListener(listener: SessionEventListener) {
        sessionListeners.remove(listener)
    }


    private suspend fun notifySessionEvent(event: SessionEvent) {
        sessionListeners.forEach { listener ->
            try {
                listener.onSessionEvent(event)
            } catch (e: Exception) {
                FlowLogger.debug("FlowAPI: Error notifying session event listener: ${e.message}")
            }
        }
    }




    fun getAPIStatus(): FlowAPIStatus {
        return FlowAPIStatus(
            activeSessions = activeSessions.size,
            registeredCommands = customCommands.size,
            allowedDirectories = allowedDirectories.size,
            rootAccessDirectory = rootAccessDirectory,
            activeUserSessions = userSessions.size,
            authenticatedSessions = userSessions.values.count { it.isAuthenticated }
        )
    }


    fun cleanup() {
        activeSessions.clear()
        customCommands.clear()
        userSessions.clear()
        sessionListeners.clear()
        allowedDirectories.clear()
        rootAccessDirectory = null
        FlowLogger.debug("FlowAPI: Cleaned up all resources")
    }
}