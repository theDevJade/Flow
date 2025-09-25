package com.thedevjade.flow.webserver.websocket

import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.webserver.database.WorkspaceRepository
import com.thedevjade.flow.common.models.FileTreeNode
import com.thedevjade.flow.webserver.api.FlowAPI
import com.thedevjade.flow.webserver.terminal.TerminalInterpreter
import com.thedevjade.flow.webserver.terminal.TerminalContext
import com.thedevjade.flow.webserver.terminal.TerminalResult
import flow.api.FileSystemAccess
import flow.api.implementation.FileSystemAccessImpl
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds


class WebSocketMessageHandler(
    private val sessionManager: WebSocketSessionManager,
    private val dataManager: GraphDataManager,
    private val graphSyncHandler: GraphSyncHandler,
    private val flowCore: com.thedevjade.flow.api.FlowCore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val terminalInterpreter = TerminalInterpreter()

    companion object {
        private const val MAX_MESSAGE_SIZE = 1024 * 1024
        private const val RATE_LIMIT_WINDOW_SECONDS = 60
        private const val MAX_MESSAGES_PER_WINDOW = 1000
        private val REQUIRED_AUTH_MESSAGE_TYPES = setOf("graph_save", "graph_delete", "workspace_create")
    }


    private val rateLimitData = ConcurrentHashMap<String, RateLimitInfo>()
    private val rateLimitMutex = Mutex()


    private val messageValidators = mapOf<String, MessageValidator>(
        "auth" to AuthMessageValidator(),
        "ping" to PingMessageValidator(),
        "heartbeat" to HeartbeatMessageValidator(),
        "graph_update" to GraphUpdateMessageValidator(),
        "graph_save" to GraphSaveMessageValidator(),
        "graph_load" to GraphLoadMessageValidator(),
        "graph_list" to GraphListMessageValidator(),
        "node_update" to NodeUpdateMessageValidator(),
        "connection_update" to ConnectionUpdateMessageValidator(),
        "user_cursor" to UserCursorMessageValidator(),

        "get_file_tree" to FileTreeMessageValidator(),
        "read_file" to ReadFileMessageValidator(),
        "write_file" to WriteFileMessageValidator(),
        "create_file" to CreateFileMessageValidator(),
        "create_directory" to CreateDirectoryMessageValidator(),
        "delete_file" to DeleteFileMessageValidator(),
        "delete_directory" to DeleteDirectoryMessageValidator(),

        "node_templates" to NodeTemplatesMessageValidator()
    )


    suspend fun handleMessage(
        sessionId: String,
        message: WebSocketMessage
    ) {
        val correlationId = generateCorrelationId()

        FlowLogger.debug("WebSocketMessageHandler",
            "Processing message - SessionId: $sessionId, Type: ${message.type}, CorrelationId: $correlationId")

        try {

            val sessionData = sessionManager.getSession(sessionId)
            if (sessionData?.session == null) {
                FlowLogger.error("WebSocketMessageHandler",
                    "Session not found: $sessionId, CorrelationId: $correlationId")
                return
            }


            if (!checkRateLimit(sessionId)) {
                FlowLogger.warn("WebSocketMessageHandler",
                    "Rate limit exceeded for session: $sessionId, CorrelationId: $correlationId")
                sendErrorResponse(sessionId, message.id, "Rate limit exceeded", correlationId)
                return
            }


            if (!validateMessageSize(message)) {
                FlowLogger.warn("WebSocketMessageHandler",
                    "Message too large for session: $sessionId, CorrelationId: $correlationId")
                sendErrorResponse(sessionId, message.id, "Message size exceeds limit", correlationId)
                return
            }


            if (REQUIRED_AUTH_MESSAGE_TYPES.contains(message.type) && !sessionData.isAuthenticated) {
                FlowLogger.warn("WebSocketMessageHandler",
                    "Unauthenticated access attempt for ${message.type}: $sessionId, CorrelationId: $correlationId")
                sendErrorResponse(sessionId, message.id, "Authentication required", correlationId)
                return
            }


            val validator = messageValidators[message.type]
            if (validator != null) {
                val validationResult = validator.validate(message)
                if (!validationResult.isValid) {
                    FlowLogger.warn("WebSocketMessageHandler",
                        "Message validation failed: ${validationResult.error}, CorrelationId: $correlationId")
                    sendErrorResponse(sessionId, message.id, validationResult.error ?: "Invalid message format", correlationId)
                    return
                }
            }


            withTimeout(30.seconds) {
                processMessage(sessionId, message, sessionData, correlationId)
            }

        } catch (e: TimeoutCancellationException) {
            FlowLogger.error("WebSocketMessageHandler",
                "Message processing timeout - SessionId: $sessionId, Type: ${message.type}, CorrelationId: $correlationId", e)
            sendErrorResponse(sessionId, message.id, "Request timeout", correlationId)
        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling message - SessionId: $sessionId, Type: ${message.type}, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Internal server error", correlationId)
        }
    }


    private suspend fun processMessage(
        sessionId: String,
        message: WebSocketMessage,
        sessionData: WebSocketSessionData,
        correlationId: String
    ) {
        when (message.type) {
            "auth" -> handleAuthMessage(sessionId, message, sessionData.session, correlationId)
            "ping" -> handlePingMessage(sessionId, message, correlationId)
            "heartbeat" -> handleHeartbeatMessage(sessionId, message, correlationId)
            "graph_update" -> handleGraphUpdate(sessionId, message, correlationId)
            "graph_save" -> handleGraphSave(sessionId, message, correlationId)
            "graph_load" -> handleGraphLoad(sessionId, message, correlationId)
            "graph_list" -> handleGraphList(sessionId, message, correlationId)
            "node_update" -> handleNodeUpdate(sessionId, message, correlationId)
            "connection_update" -> handleConnectionUpdate(sessionId, message, correlationId)
            "user_cursor" -> handleUserCursor(sessionId, message, correlationId)

            "get_file_tree" -> handleGetFileTree(sessionId, message, correlationId)
            "read_file" -> handleReadFile(sessionId, message, correlationId)
            "write_file" -> handleWriteFile(sessionId, message, correlationId)
            "create_file" -> handleCreateFile(sessionId, message, correlationId)
            "create_directory" -> handleCreateDirectory(sessionId, message, correlationId)
            "delete_file" -> handleDeleteFile(sessionId, message, correlationId)
            "delete_directory" -> handleDeleteDirectory(sessionId, message, correlationId)
            "terminal_command" -> handleTerminalCommand(sessionId, message, correlationId)
            "terminal_autocomplete" -> handleTerminalAutocomplete(sessionId, message, correlationId)
            "viewport_update" -> handleViewportUpdate(sessionId, message, correlationId)

            "node_templates" -> handleNodeTemplates(sessionId, message, correlationId)

            "workspace_list" -> handleWorkspaceList(sessionId, message, correlationId)
            "create_workspace" -> handleCreateWorkspace(sessionId, message, correlationId)
            "update_workspace" -> handleUpdateWorkspace(sessionId, message, correlationId)
            "delete_workspace" -> handleDeleteWorkspace(sessionId, message, correlationId)
            else -> {
                FlowLogger.warn("WebSocketMessageHandler",
                    "Unknown message type: ${message.type}, CorrelationId: $correlationId")
                sendErrorResponse(sessionId, message.id, "Unknown message type: ${message.type}", correlationId)
            }
        }
    }



    private suspend fun handleAuthMessage(
        sessionId: String,
        message: WebSocketMessage,
        session: DefaultWebSocketSession,
        correlationId: String
    ) {
        val userId = message.data["userId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        val username = message.data["username"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        val token = message.data["token"]?.jsonPrimitive?.content

        if (userId == null || username == null) {
            FlowLogger.warn("WebSocketMessageHandler",
                "Auth message missing required fields - SessionId: $sessionId, CorrelationId: $correlationId")
            sendErrorResponse(sessionId, message.id, "Missing userId or username in auth message", correlationId)
            return
        }


        if (token != null && !validateAuthToken(token, userId)) {
            FlowLogger.warn("WebSocketMessageHandler",
                "Invalid auth token - UserId: $userId, SessionId: $sessionId, CorrelationId: $correlationId")
            sendErrorResponse(sessionId, message.id, "Invalid authentication token", correlationId)
            return
        }


        val sessionData = sessionManager.getSession(sessionId)
        if (sessionData != null) {
            sessionData.isAuthenticated = true
            sessionData.metadata["authenticatedAt"] = System.currentTimeMillis()

            FlowLogger.info("WebSocketMessageHandler",
                "User authenticated - Username: $username, UserId: $userId, SessionId: $sessionId, CorrelationId: $correlationId")


            val response = WebSocketMessage(
                type = "auth_response",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(true))
                    put("userId", JsonPrimitive(userId))
                    put("username", JsonPrimitive(username))
                    put("sessionId", JsonPrimitive(sessionId))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)
        } else {
            FlowLogger.error("WebSocketMessageHandler",
                "Session data not found during auth - SessionId: $sessionId, CorrelationId: $correlationId")
            sendErrorResponse(sessionId, message.id, "Session not found", correlationId)
        }
    }


    private suspend fun handlePingMessage(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        val sessionData = sessionManager.getSession(sessionId)
        val userId = sessionData?.userId

        FlowLogger.debug("WebSocketMessageHandler",
            "Received ping - UserId: $userId, SessionId: $sessionId, CorrelationId: $correlationId")


        sessionData?.lastActivity = System.currentTimeMillis()

        val pongResponse = WebSocketMessage(
            type = "pong",
            id = message.id,
            data = buildJsonObject {
                put("timestamp", JsonPrimitive(System.currentTimeMillis()))
                put("correlationId", JsonPrimitive(correlationId))
                put("sessionId", JsonPrimitive(sessionId))
            },
            userId = userId,
            sessionId = sessionId
        )

        sessionManager.sendToSession(sessionId, pongResponse)
    }


    private suspend fun handleHeartbeatMessage(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        val sessionData = sessionManager.getSession(sessionId)
        val userId = sessionData?.userId

        FlowLogger.debug("WebSocketMessageHandler",
            "Received heartbeat - UserId: $userId, SessionId: $sessionId, CorrelationId: $correlationId")


        sessionData?.let {
            it.lastActivity = System.currentTimeMillis()
            it.metadata["lastHeartbeat"] = System.currentTimeMillis()
            it.metadata["heartbeatCount"] = (it.metadata["heartbeatCount"] as? Int ?: 0) + 1
        }


        val heartbeatResponse = WebSocketMessage(
            type = "heartbeat_ack",
            id = message.id,
            data = buildJsonObject {
                put("timestamp", JsonPrimitive(System.currentTimeMillis()))
                put("correlationId", JsonPrimitive(correlationId))
                put("sessionId", JsonPrimitive(sessionId))
                put("status", JsonPrimitive("alive"))
            },
            userId = userId,
            sessionId = sessionId
        )

        sessionManager.sendToSession(sessionId, heartbeatResponse)
    }


    private suspend fun handleGraphUpdate(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val updateType = message.data["updateType"]?.jsonPrimitive?.content

        if (graphId.isNullOrBlank()) {
            FlowLogger.warn("WebSocketMessageHandler",
                "Graph update missing graphId - SessionId: $sessionId, CorrelationId: $correlationId")
            sendErrorResponse(sessionId, message.id, "Missing graphId", correlationId)
            return
        }

        FlowLogger.info("WebSocketMessageHandler",
            "Processing graph update - GraphId: $graphId, UpdateType: $updateType, SessionId: $sessionId, CorrelationId: $correlationId")

        try {

            graphSyncHandler.handleGraphUpdate(sessionManager.getSession(sessionId)!!.session, message)


            val broadcastMessage = WebSocketMessage(
                type = "graph_updated",
                data = buildJsonObject {
                    put("graphId", JsonPrimitive(graphId))
                    put("updateType", JsonPrimitive(updateType ?: "generic"))
                    put("updatedBy", JsonPrimitive(sessionManager.getSession(sessionId)?.userId ?: "unknown"))
                    put("correlationId", JsonPrimitive(correlationId))
                }
            )

            sessionManager.broadcast(broadcastMessage, excludeSessionId = sessionId)


            sendSuccessResponse(sessionId, message.id, "Graph updated successfully", correlationId)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Failed to process graph update - GraphId: $graphId, SessionId: $sessionId, CorrelationId: $correlationId", e)
            sendErrorResponse(sessionId, message.id, "Failed to update graph: ${e.message}", correlationId)
        }
    }


    private suspend fun handleGraphSave(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val graphData = message.data["graphData"]?.jsonObject

        if (graphId.isNullOrBlank()) {
            sendErrorResponse(sessionId, message.id, "Missing graphId", correlationId)
            return
        }

        if (graphData == null) {
            sendErrorResponse(sessionId, message.id, "Missing graphData", correlationId)
            return
        }

        FlowLogger.info("WebSocketMessageHandler",
            "Saving graph - GraphId: $graphId, SessionId: $sessionId, CorrelationId: $correlationId")

        try {

            val graph = Json.decodeFromJsonElement<GraphData>(graphData)
            val success = dataManager.saveGraph(graphId, graph)

            if (success) {
                sendSuccessResponse(sessionId, message.id, "Graph saved successfully", correlationId,
                    mapOf("graphId" to JsonPrimitive(graphId)))


                val notificationMessage = WebSocketMessage(
                    type = "graph_saved",
                    data = buildJsonObject {
                        put("graphId", JsonPrimitive(graphId))
                        put("savedBy", JsonPrimitive(sessionManager.getSession(sessionId)?.userId ?: "unknown"))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                )
                sessionManager.broadcast(notificationMessage, excludeSessionId = sessionId)

            } else {
                sendErrorResponse(sessionId, message.id, "Failed to save graph", correlationId)
            }

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error saving graph - GraphId: $graphId, SessionId: $sessionId, CorrelationId: $correlationId", e)
            sendErrorResponse(sessionId, message.id, "Error saving graph: ${e.message}", correlationId)
        }
    }


    private suspend fun handleGraphLoad(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content

        if (graphId.isNullOrBlank()) {
            sendErrorResponse(sessionId, message.id, "Missing graphId", correlationId)
            return
        }

        FlowLogger.info("WebSocketMessageHandler",
            "Loading graph - GraphId: $graphId, SessionId: $sessionId, CorrelationId: $correlationId")

        try {
            val graph = dataManager.loadGraph(graphId)

            if (graph != null) {
                val responseData = buildJsonObject {
                    put("success", JsonPrimitive(true))
                    put("graphId", JsonPrimitive(graphId))
                    put("graphData", Json.encodeToJsonElement(graph))
                    put("correlationId", JsonPrimitive(correlationId))
                }

                val response = WebSocketMessage(
                    type = "graph_load_response",
                    id = message.id,
                    data = responseData,
                    sessionId = sessionId
                )

                sessionManager.sendToSession(sessionId, response)

            } else {
                sendErrorResponse(sessionId, message.id, "Graph not found", correlationId)
            }

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error loading graph - GraphId: $graphId, SessionId: $sessionId, CorrelationId: $correlationId", e)
            sendErrorResponse(sessionId, message.id, "Error loading graph: ${e.message}", correlationId)
        }
    }

    private suspend fun handleGraphList(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        FlowLogger.info("WebSocketMessageHandler",
            "Listing graphs - SessionId: $sessionId, CorrelationId: $correlationId")

        try {
            val graphIds = dataManager.listGraphs()

            val responseData = buildJsonObject {
                put("success", JsonPrimitive(true))
                put("graphs", buildJsonArray {
                    graphIds.forEach { graphId ->
                        add(buildJsonObject {
                            put("id", JsonPrimitive(graphId))
                            put("name", JsonPrimitive(graphId))
                            put("description", JsonPrimitive("Graph: $graphId"))
                            put("createdAt", JsonPrimitive(java.time.Instant.now().toString()))
                            put("updatedAt", JsonPrimitive(java.time.Instant.now().toString()))
                        })
                    }
                })
                put("correlationId", JsonPrimitive(correlationId))
            }

            val response = WebSocketMessage(
                type = "graph_list_response",
                id = message.id,
                data = responseData,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error listing graphs - SessionId: $sessionId, CorrelationId: $correlationId", e)
            sendErrorResponse(sessionId, message.id, "Error listing graphs: ${e.message}", correlationId)
        }
    }


    private suspend fun handleNodeUpdate(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val nodeId = message.data["nodeId"]?.jsonPrimitive?.content
        val nodeData = message.data["nodeData"]?.jsonObject

        if (graphId.isNullOrBlank() || nodeId.isNullOrBlank() || nodeData == null) {
            sendErrorResponse(sessionId, message.id, "Missing required fields for node update", correlationId)
            return
        }

        FlowLogger.debug("WebSocketMessageHandler",
            "Node update - GraphId: $graphId, NodeId: $nodeId, SessionId: $sessionId, CorrelationId: $correlationId")


        val broadcastMessage = WebSocketMessage(
            type = "node_updated",
            data = buildJsonObject {
                put("graphId", JsonPrimitive(graphId))
                put("nodeId", JsonPrimitive(nodeId))
                put("nodeData", nodeData)
                put("updatedBy", JsonPrimitive(sessionManager.getSession(sessionId)?.userId ?: "unknown"))
                put("correlationId", JsonPrimitive(correlationId))
            }
        )

        sessionManager.broadcast(broadcastMessage, excludeSessionId = sessionId)
        sendSuccessResponse(sessionId, message.id, "Node updated successfully", correlationId)
    }


    private suspend fun handleConnectionUpdate(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val connectionId = message.data["connectionId"]?.jsonPrimitive?.content
        val connectionData = message.data["connectionData"]?.jsonObject

        if (graphId.isNullOrBlank() || connectionId.isNullOrBlank() || connectionData == null) {
            sendErrorResponse(sessionId, message.id, "Missing required fields for connection update", correlationId)
            return
        }

        FlowLogger.debug("WebSocketMessageHandler",
            "Connection update - GraphId: $graphId, ConnectionId: $connectionId, SessionId: $sessionId, CorrelationId: $correlationId")


        val broadcastMessage = WebSocketMessage(
            type = "connection_updated",
            data = buildJsonObject {
                put("graphId", JsonPrimitive(graphId))
                put("connectionId", JsonPrimitive(connectionId))
                put("connectionData", connectionData)
                put("updatedBy", JsonPrimitive(sessionManager.getSession(sessionId)?.userId ?: "unknown"))
                put("correlationId", JsonPrimitive(correlationId))
            }
        )

        sessionManager.broadcast(broadcastMessage, excludeSessionId = sessionId)
        sendSuccessResponse(sessionId, message.id, "Connection updated successfully", correlationId)
    }


    private suspend fun handleUserCursor(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val position = message.data["position"]?.jsonObject

        if (graphId.isNullOrBlank() || position == null) {

            FlowLogger.debug("WebSocketMessageHandler",
                "Invalid cursor update - GraphId: $graphId, SessionId: $sessionId, CorrelationId: $correlationId")
            return
        }


        val broadcastMessage = WebSocketMessage(
            type = "user_cursor_update",
            data = buildJsonObject {
                put("graphId", JsonPrimitive(graphId))
                put("userId", JsonPrimitive(sessionManager.getSession(sessionId)?.userId ?: "unknown"))
                put("position", position)
                put("correlationId", JsonPrimitive(correlationId))
            }
        )

        sessionManager.broadcast(broadcastMessage, excludeSessionId = sessionId)
    }





    private fun generateCorrelationId(): String {
        return "req_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }


    private suspend fun checkRateLimit(sessionId: String): Boolean = rateLimitMutex.withLock {
        val now = System.currentTimeMillis()
        val windowStart = now - (RATE_LIMIT_WINDOW_SECONDS * 1000)

        val rateLimitInfo = rateLimitData.computeIfAbsent(sessionId) {
            RateLimitInfo(mutableListOf(), 0)
        }


        rateLimitInfo.timestamps.removeIf { it < windowStart }

        if (rateLimitInfo.timestamps.size >= MAX_MESSAGES_PER_WINDOW) {
            rateLimitInfo.violationCount++
            return@withLock false
        }

        rateLimitInfo.timestamps.add(now)
        return@withLock true
    }


    private fun validateMessageSize(message: WebSocketMessage): Boolean {
        return try {
            val messageSize = Json.encodeToString(WebSocketMessage.serializer(), message).toByteArray().size
            messageSize <= MAX_MESSAGE_SIZE
        } catch (e: Exception) {
            false
        }
    }


    private fun validateAuthToken(token: String, userId: String): Boolean {
        // TODO: Implement proper token validation
        return token.isNotBlank() && token.length >= 8
    }


    private suspend fun sendErrorResponse(
        sessionId: String,
        messageId: String?,
        error: String,
        correlationId: String
    ) {
        val errorResponse = WebSocketMessage(
            type = "error",
            id = messageId,
            data = buildJsonObject {
                put("error", JsonPrimitive(error))
                put("correlationId", JsonPrimitive(correlationId))
                put("timestamp", JsonPrimitive(System.currentTimeMillis()))
            },
            sessionId = sessionId
        )

        try {
            sessionManager.sendToSession(sessionId, errorResponse)
        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Failed to send error response - SessionId: $sessionId, CorrelationId: $correlationId", e)
        }
    }


    private suspend fun sendSuccessResponse(
        sessionId: String,
        messageId: String?,
        message: String,
        correlationId: String,
        additionalData: Map<String, JsonElement> = emptyMap()
    ) {
        val dataMap = mutableMapOf<String, JsonElement>(
            "success" to JsonPrimitive(true),
            "message" to JsonPrimitive(message),
            "correlationId" to JsonPrimitive(correlationId),
            "timestamp" to JsonPrimitive(System.currentTimeMillis())
        )
        dataMap.putAll(additionalData)

        val successResponse = WebSocketMessage(
            type = "success",
            id = messageId,
            data = JsonObject(dataMap),
            sessionId = sessionId
        )

        try {
            sessionManager.sendToSession(sessionId, successResponse)
        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Failed to send success response - SessionId: $sessionId, CorrelationId: $correlationId", e)
        }
    }




    private suspend fun handleWorkspaceList(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            FlowLogger.info("WebSocketMessageHandler",
                "Getting workspace list - SessionId: $sessionId, CorrelationId: $correlationId")

            val session = sessionManager.getSession(sessionId)
            if (session == null) {
                sendErrorResponse(sessionId, message.id, "Session not found", correlationId)
                return
            }


            val userId = session.userId.toIntOrNull() ?: 1
            val workspaces = WorkspaceRepository.loadWorkspacesByUser(userId)
            val workspaceData = workspaces.map { workspace ->
                mapOf(
                    "workspaceId" to workspace.workspaceId,
                    "name" to workspace.name,
                    "data" to workspace.data,
                    "currentPage" to workspace.currentPage,
                    "settings" to workspace.settings,
                    "createdAt" to workspace.createdAt.toString(),
                    "updatedAt" to workspace.updatedAt.toString()
                )
            }

            val response = WebSocketMessage(
                type = "workspace_list_response",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(true))
                    put("workspaces", Json.encodeToJsonElement(workspaceData))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = session.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling workspace_list - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to get workspace list: ${e.message}", correlationId)
        }
    }

    private suspend fun handleCreateWorkspace(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val name = message.data["name"]?.jsonPrimitive?.content
            val data = message.data["data"]?.jsonObject ?: buildJsonObject { }
            val settings = message.data["settings"]?.jsonObject ?: buildJsonObject { }

            FlowLogger.info("WebSocketMessageHandler",
                "Creating workspace - Name: $name, SessionId: $sessionId, CorrelationId: $correlationId")

            val session = sessionManager.getSession(sessionId)
            if (session == null) {
                sendErrorResponse(sessionId, message.id, "Session not found", correlationId)
                return
            }

            if (name.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Workspace name is required", correlationId)
                return
            }

            val workspaceId = "workspace_${System.currentTimeMillis()}"
            val userId = session.userId.toIntOrNull() ?: 1
            val success = WorkspaceRepository.saveWorkspace(
                workspaceId = workspaceId,
                userId = userId,
                name = name,
                data = data.toString(),
                currentPage = null,
                settings = settings.toString()
            )

            val response = WebSocketMessage(
                type = "workspace_created",
                id = message.id,
                data = if (success) {
                    buildJsonObject {
                        put("success", JsonPrimitive(true))
                        put("workspace", Json.encodeToJsonElement(mapOf(
                            "workspaceId" to workspaceId,
                            "name" to name,
                            "data" to data,
                            "currentPage" to null,
                            "settings" to settings,
                            "createdAt" to java.time.Instant.now().toString(),
                            "updatedAt" to java.time.Instant.now().toString()
                        )))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                } else {
                    buildJsonObject {
                        put("success", JsonPrimitive(false))
                        put("error", JsonPrimitive("Failed to create workspace"))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                },
                userId = session.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling create_workspace - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to create workspace: ${e.message}", correlationId)
        }
    }

    private suspend fun handleUpdateWorkspace(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val workspaceId = message.data["workspaceId"]?.jsonPrimitive?.content
            val name = message.data["name"]?.jsonPrimitive?.content
            val data = message.data["data"]?.jsonObject
            val settings = message.data["settings"]?.jsonObject

            FlowLogger.info("WebSocketMessageHandler",
                "Updating workspace - ID: $workspaceId, SessionId: $sessionId, CorrelationId: $correlationId")

            val session = sessionManager.getSession(sessionId)
            if (session == null) {
                sendErrorResponse(sessionId, message.id, "Session not found", correlationId)
                return
            }

            if (workspaceId.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Workspace ID is required", correlationId)
                return
            }

            val existingWorkspace = WorkspaceRepository.loadWorkspace(workspaceId)
            if (existingWorkspace == null) {
                sendErrorResponse(sessionId, message.id, "Workspace not found", correlationId)
                return
            }

            val userId = session.userId.toIntOrNull() ?: 1
            val success = WorkspaceRepository.saveWorkspace(
                workspaceId = workspaceId,
                userId = userId,
                name = name ?: existingWorkspace.name,
                data = data?.toString() ?: existingWorkspace.data,
                currentPage = existingWorkspace.currentPage,
                settings = settings?.toString() ?: existingWorkspace.settings
            )

            val response = WebSocketMessage(
                type = "workspace_updated",
                id = message.id,
                data = if (success) {
                    buildJsonObject {
                        put("success", JsonPrimitive(true))
                        put("workspace", Json.encodeToJsonElement(mapOf(
                            "workspaceId" to workspaceId,
                            "name" to (name ?: existingWorkspace.name),
                            "data" to (data?.toString() ?: existingWorkspace.data),
                            "currentPage" to existingWorkspace.currentPage,
                            "settings" to (settings?.toString() ?: existingWorkspace.settings),
                            "createdAt" to existingWorkspace.createdAt.toString(),
                            "updatedAt" to java.time.Instant.now().toString()
                        )))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                } else {
                    buildJsonObject {
                        put("success", JsonPrimitive(false))
                        put("error", JsonPrimitive("Failed to update workspace"))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                },
                userId = session.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling update_workspace - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to update workspace: ${e.message}", correlationId)
        }
    }

    private suspend fun handleDeleteWorkspace(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val workspaceId = message.data["workspaceId"]?.jsonPrimitive?.content

            FlowLogger.info("WebSocketMessageHandler",
                "Deleting workspace - ID: $workspaceId, SessionId: $sessionId, CorrelationId: $correlationId")

            val session = sessionManager.getSession(sessionId)
            if (session == null) {
                sendErrorResponse(sessionId, message.id, "Session not found", correlationId)
                return
            }

            if (workspaceId.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Workspace ID is required", correlationId)
                return
            }

            val success = WorkspaceRepository.deleteWorkspace(workspaceId)

            val response = WebSocketMessage(
                type = "workspace_deleted",
                id = message.id,
                data = if (success) {
                    buildJsonObject {
                        put("success", JsonPrimitive(true))
                        put("workspaceId", JsonPrimitive(workspaceId))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                } else {
                    buildJsonObject {
                        put("success", JsonPrimitive(false))
                        put("error", JsonPrimitive("Failed to delete workspace"))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                },
                userId = session.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling delete_workspace - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to delete workspace: ${e.message}", correlationId)
        }
    }

    fun dispose() {
        scope.cancel("WebSocketMessageHandler disposed")
        rateLimitData.clear()
        FlowLogger.info("WebSocketMessageHandler", "Message handler disposed")
    }



    private fun fileTreeNodeToJson(node: FileTreeNode): JsonObject {
        return buildJsonObject {
            put("name", JsonPrimitive(node.name))
            put("fullPath", JsonPrimitive(node.path))
            put("isDirectory", JsonPrimitive(node.type == "directory"))
            put("lastModified", JsonPrimitive(
                node.lastModified?.let {
                    java.time.Instant.ofEpochMilli(it).toString()
                } ?: java.time.Instant.now().toString()
            ))
            put("size", JsonPrimitive(node.size ?: 0))
            put("children", buildJsonArray {
                node.children.forEach { child ->
                    add(fileTreeNodeToJson(child))
                }
            })
        }
    }

    private suspend fun handleGetFileTree(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val rootPath = message.data["rootPath"]?.jsonPrimitive?.content
            FlowLogger.info("WebSocketMessageHandler",
                "Getting file tree - RootPath: $rootPath, SessionId: $sessionId, CorrelationId: $correlationId")


            val flowApi = flow.api.implementation.FlowApiImpl()
            val fileSystemAccess = flowApi.fileSystemAccess


            if (fileSystemAccess.getRootDirectory() == null) {
                val currentWorkingDir = java.io.File("").absolutePath
                if (!fileSystemAccess.setRootDirectory(java.nio.file.Paths.get(currentWorkingDir))) {
                    FlowLogger.error("WebSocketMessageHandler", "Failed to initialize file system access")
                    sendErrorResponse(sessionId, message.id, "File system not initialized", correlationId)
                    return
                }
            }

            val pathToUse = if (rootPath != null) java.nio.file.Paths.get(rootPath) else null
            val fileTree = fileSystemAccess.getFileTree(pathToUse)

            val response = WebSocketMessage(
                type = "file_tree",
                id = message.id,
                data = if (fileTree != null) {
                    val fileTreeJson = fileTreeNodeToJson(fileTree)
                    buildJsonObject {
                        put("success", JsonPrimitive(true))
                        fileTreeJson.forEach { (key, value) ->
                            put(key, value)
                        }
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                } else {
                    buildJsonObject {
                        put("success", JsonPrimitive(false))
                        put("error", JsonPrimitive("Failed to access file tree"))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling get_file_tree - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to get file tree: ${e.message}", correlationId)
        }
    }

    private suspend fun handleReadFile(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val filePath = message.data["path"]?.jsonPrimitive?.content
            val requestId = message.data["requestId"]?.jsonPrimitive?.content

            if (filePath.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing file path", correlationId)
                return
            }

            FlowLogger.info("WebSocketMessageHandler",
                "Reading file - Path: $filePath, RequestId: $requestId, SessionId: $sessionId, CorrelationId: $correlationId")

            val flowApi = FlowAPI.getInstance();
            val fileSystemAccess = flowApi.getFileManager()


            if (fileSystemAccess.getRootDirectory() == null) {
                val currentWorkingDir = java.io.File("").absolutePath
                if (!fileSystemAccess.setRootDirectory(java.nio.file.Paths.get(currentWorkingDir))) {
                    FlowLogger.error("WebSocketMessageHandler", "Failed to initialize file system access")
                    sendErrorResponse(sessionId, message.id, "File system not initialized", correlationId)
                    return
                }
            }

            val path = java.nio.file.Paths.get(filePath)
            val content = fileSystemAccess.readFile(path)

            val response = WebSocketMessage(
                type = "file_content",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(content != null))
                    put("path", JsonPrimitive(filePath))
                    put("content", JsonPrimitive(content ?: ""))
                    if (requestId != null) put("requestId", JsonPrimitive(requestId))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling read_file - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to read file: ${e.message}", correlationId)
        }
    }

    private suspend fun handleWriteFile(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val filePath = message.data["path"]?.jsonPrimitive?.content
            val content = message.data["content"]?.jsonPrimitive?.content

            if (filePath.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing file path", correlationId)
                return
            }

            if (content == null) {
                sendErrorResponse(sessionId, message.id, "Missing file content", correlationId)
                return
            }

            FlowLogger.info("WebSocketMessageHandler",
                "Writing file - Path: $filePath, SessionId: $sessionId, CorrelationId: $correlationId")

            val flowApi = FlowAPI.getInstance();
            val fileSystemAccess = flowApi.getFileManager()
            val path = java.nio.file.Paths.get(filePath)
            val success = fileSystemAccess.writeFile(path, content)

            val response = WebSocketMessage(
                type = "file_saved",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(success))
                    put("path", JsonPrimitive(filePath))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling write_file - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to write file: ${e.message}", correlationId)
        }
    }

    private suspend fun handleCreateFile(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val dirPath = message.data["dirPath"]?.jsonPrimitive?.content
            val fileName = message.data["fileName"]?.jsonPrimitive?.content

            if (dirPath.isNullOrBlank() || fileName.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing dirPath or fileName", correlationId)
                return
            }

            FlowLogger.info("WebSocketMessageHandler",
                "Creating file - DirPath: $dirPath, FileName: $fileName, SessionId: $sessionId, CorrelationId: $correlationId")

            val flowApi = flow.api.implementation.FlowApiImpl()
            val fileSystemAccess = flowApi.fileSystemAccess
            val fullPath = java.nio.file.Paths.get(dirPath).resolve(fileName)
            val success = fileSystemAccess.createFile(fullPath)

            val response = WebSocketMessage(
                type = "file_created",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(success))
                    put("path", JsonPrimitive(fullPath.toString()))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling create_file - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to create file: ${e.message}", correlationId)
        }
    }

    private suspend fun handleCreateDirectory(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val parentPath = message.data["parentPath"]?.jsonPrimitive?.content
            val dirName = message.data["dirName"]?.jsonPrimitive?.content

            if (parentPath.isNullOrBlank() || dirName.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing parentPath or dirName", correlationId)
                return
            }

            FlowLogger.info("WebSocketMessageHandler",
                "Creating directory - ParentPath: $parentPath, DirName: $dirName, SessionId: $sessionId, CorrelationId: $correlationId")

            val flowApi = flow.api.implementation.FlowApiImpl()
            val fileSystemAccess = flowApi.fileSystemAccess
            val fullPath = java.nio.file.Paths.get(parentPath).resolve(dirName)
            val success = fileSystemAccess.createDirectory(fullPath)

            val response = WebSocketMessage(
                type = "directory_created",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(success))
                    put("path", JsonPrimitive(fullPath.toString()))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling create_directory - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to create directory: ${e.message}", correlationId)
        }
    }

    private suspend fun handleDeleteFile(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val filePath = message.data["path"]?.jsonPrimitive?.content

            if (filePath.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing file path", correlationId)
                return
            }

            FlowLogger.info("WebSocketMessageHandler",
                "Deleting file - Path: $filePath, SessionId: $sessionId, CorrelationId: $correlationId")

            val flowApi = flow.api.implementation.FlowApiImpl()
            val fileSystemAccess = flowApi.fileSystemAccess
            val path = java.nio.file.Paths.get(filePath)
            val success = fileSystemAccess.deleteFile(path)

            val response = WebSocketMessage(
                type = "file_deleted",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(success))
                    put("path", JsonPrimitive(filePath))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling delete_file - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to delete file: ${e.message}", correlationId)
        }
    }

    private suspend fun handleDeleteDirectory(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val dirPath = message.data["path"]?.jsonPrimitive?.content
            val recursive = message.data["recursive"]?.jsonPrimitive?.booleanOrNull ?: false

            if (dirPath.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing directory path", correlationId)
                return
            }

            FlowLogger.info("WebSocketMessageHandler",
                "Deleting directory - Path: $dirPath, Recursive: $recursive, SessionId: $sessionId, CorrelationId: $correlationId")

            val flowApi = flow.api.implementation.FlowApiImpl()
            val fileSystemAccess = flowApi.fileSystemAccess
            val path = java.nio.file.Paths.get(dirPath)
            val success = fileSystemAccess.deleteDirectory(path, recursive)

            val response = WebSocketMessage(
                type = "directory_deleted",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(success))
                    put("path", JsonPrimitive(dirPath))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling delete_directory - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to delete directory: ${e.message}", correlationId)
        }
    }

    private suspend fun handleNodeTemplates(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            FlowLogger.info("WebSocketMessageHandler",
                "Requesting node templates - SessionId: $sessionId, CorrelationId: $correlationId")


            val templates = flowCore.getNodeTemplates()


            val nodeTemplatesArray = JsonArray(
                templates.map { (templateId, template) ->
                    buildJsonObject {
                        put("id", templateId)
                        put("name", template.name)
                        put("description", template.description)
                        put("category", template.category)
                        put("version", "1.0")
                        put("color", buildJsonObject {

                            val color = template.color.removePrefix("#")
                            val r = color.substring(0, 2).toInt(16) / 255.0
                            val g = color.substring(2, 4).toInt(16) / 255.0
                            val b = color.substring(4, 6).toInt(16) / 255.0
                            put("r", r)
                            put("g", g)
                            put("b", b)
                            put("a", 1.0)
                        })
                        put("size", buildJsonObject {
                            put("width", 150)
                            put("height", 80)
                        })
                        put("inputs", JsonArray(
                            template.inputs.map { port ->
                                buildJsonObject {
                                    put("id", port.id)
                                    put("name", port.name)
                                    put("type", "any")
                                    put("color", buildJsonObject {
                                        val r = ((port.color shr 16) and 0xFF) / 255.0
                                        val g = ((port.color shr 8) and 0xFF) / 255.0
                                        val b = (port.color and 0xFF) / 255.0
                                        put("r", r)
                                        put("g", g)
                                        put("b", b)
                                        put("a", 1.0)
                                    })
                                }
                            }
                        ))
                        put("outputs", JsonArray(
                            template.outputs.map { port ->
                                buildJsonObject {
                                    put("id", port.id)
                                    put("name", port.name)
                                    put("type", "any")
                                    put("color", buildJsonObject {
                                        val r = ((port.color shr 16) and 0xFF) / 255.0
                                        val g = ((port.color shr 8) and 0xFF) / 255.0
                                        val b = (port.color and 0xFF) / 255.0
                                        put("r", r)
                                        put("g", g)
                                        put("b", b)
                                        put("a", 1.0)
                                    })
                                }
                            }
                        ))
                        put("properties", JsonArray(
                            template.properties.map { prop ->
                                buildJsonObject {
                                    put("name", prop.name)
                                    put("type", prop.type)
                                    put("label", prop.name.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } })
                                    put("default", prop.defaultValue?.toString() ?: "")
                                    put("description", prop.description)
                                    if (prop.options != null) {
                                        put("options", JsonArray(prop.options!!.map { option -> JsonPrimitive(option.toString()) }))
                                    }
                                }
                            }
                        ))
                    }
                }
            )

            val templatesJson = buildJsonObject {
                put("version", "1.0")
                put("node_templates", nodeTemplatesArray)
            }

            val response = WebSocketMessage(
                type = "node_templates_response",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(true))
                    put("templates", templatesJson)
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling node_templates - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to get node templates: ${e.message}", correlationId)
        }
    }

    private suspend fun handleTerminalCommand(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val command = message.data["command"]?.jsonPrimitive?.content
            val cwd = message.data["cwd"]?.jsonPrimitive?.content ?: "/"
            val pageId = message.data["pageId"]?.jsonPrimitive?.content
            val session = sessionManager.getSession(sessionId)

            if (command.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing command", correlationId)
                return
            }

            FlowLogger.info("WebSocketMessageHandler",
                "Executing custom terminal command - Command: '$command', CWD: $cwd, PageId: $pageId, SessionId: $sessionId, CorrelationId: $correlationId")


            val startResponse = WebSocketMessage(
                type = "terminal_response",
                id = message.id,
                data = buildJsonObject {
                    put("type", JsonPrimitive("start"))
                    put("success", JsonPrimitive(true))
                    put("cwd", JsonPrimitive(cwd))
                    if (pageId != null) put("pageId", JsonPrimitive(pageId))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = session?.userId,
                sessionId = sessionId
            )
            sessionManager.sendToSession(sessionId, startResponse)


            val context = TerminalContext(
                sessionId = sessionId,
                userId = session?.userId,
                currentDirectory = cwd,
                pageId = pageId
            )


            val result = terminalInterpreter.executeCommand(command, context)

            when (result) {
                is TerminalResult.Success -> {

                    result.output.forEach { line ->
                        val streamResponse = WebSocketMessage(
                            type = "terminal_response",
                            id = message.id,
                            data = buildJsonObject {
                                put("type", JsonPrimitive("stream"))
                                put("stream", JsonPrimitive("stdout"))
                                put("data", JsonPrimitive(line))
                                put("cwd", JsonPrimitive(result.newDirectory ?: cwd))
                                if (pageId != null) put("pageId", JsonPrimitive(pageId))
                                put("correlationId", JsonPrimitive(correlationId))
                            },
                            userId = session?.userId,
                            sessionId = sessionId
                        )
                        sessionManager.sendToSession(sessionId, streamResponse)
                    }


                    val endResponse = WebSocketMessage(
                        type = "terminal_response",
                        id = message.id,
                        data = buildJsonObject {
                            put("type", JsonPrimitive("end"))
                            put("success", JsonPrimitive(true))
                            put("exitCode", JsonPrimitive(0))
                            put("cwd", JsonPrimitive(result.newDirectory ?: cwd))
                            if (pageId != null) put("pageId", JsonPrimitive(pageId))
                            put("correlationId", JsonPrimitive(correlationId))


                            result.data.forEach { (key, value) ->
                                put(key, value)
                            }
                        },
                        userId = session?.userId,
                        sessionId = sessionId
                    )
                    sessionManager.sendToSession(sessionId, endResponse)
                }

                is TerminalResult.Error -> {

                    val errorStreamResponse = WebSocketMessage(
                        type = "terminal_response",
                        id = message.id,
                        data = buildJsonObject {
                            put("type", JsonPrimitive("stream"))
                            put("stream", JsonPrimitive("stderr"))
                            put("data", JsonPrimitive(result.message))
                            put("cwd", JsonPrimitive(cwd))
                            if (pageId != null) put("pageId", JsonPrimitive(pageId))
                            put("correlationId", JsonPrimitive(correlationId))
                        },
                        userId = session?.userId,
                        sessionId = sessionId
                    )
                    sessionManager.sendToSession(sessionId, errorStreamResponse)


                    val endResponse = WebSocketMessage(
                        type = "terminal_response",
                        id = message.id,
                        data = buildJsonObject {
                            put("type", JsonPrimitive("end"))
                            put("success", JsonPrimitive(false))
                            put("exitCode", JsonPrimitive(result.code))
                            put("cwd", JsonPrimitive(cwd))
                            if (pageId != null) put("pageId", JsonPrimitive(pageId))
                            put("correlationId", JsonPrimitive(correlationId))
                        },
                        userId = session?.userId,
                        sessionId = sessionId
                    )
                    sessionManager.sendToSession(sessionId, endResponse)
                }

                is TerminalResult.Stream -> {

                    val streamResponse = WebSocketMessage(
                        type = "terminal_response",
                        id = message.id,
                        data = buildJsonObject {
                            put("type", JsonPrimitive("stream"))
                            put("stream", JsonPrimitive(result.stream))
                            put("data", JsonPrimitive(result.output))
                            put("cwd", JsonPrimitive(cwd))
                            if (pageId != null) put("pageId", JsonPrimitive(pageId))
                            put("correlationId", JsonPrimitive(correlationId))
                        },
                        userId = session?.userId,
                        sessionId = sessionId
                    )
                    sessionManager.sendToSession(sessionId, streamResponse)
                }
            }

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling terminal_command - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)

            val errorResponse = WebSocketMessage(
                type = "terminal_response",
                id = message.id,
                data = buildJsonObject {
                    put("type", JsonPrimitive("error"))
                    put("success", JsonPrimitive(false))
                    put("error", JsonPrimitive("Internal error: ${e.message}"))
                    put("cwd", JsonPrimitive("/"))
                    put("exitCode", JsonPrimitive(-1))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, errorResponse)
        }
    }

    private suspend fun handleTerminalAutocomplete(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val input = message.data["input"]?.jsonPrimitive?.content
            val cwd = message.data["cwd"]?.jsonPrimitive?.content ?: "/"
            val pageId = message.data["pageId"]?.jsonPrimitive?.content
            val cursorPosition = message.data["cursorPosition"]?.jsonPrimitive?.intOrNull ?: 0
            val session = sessionManager.getSession(sessionId)

            if (input.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing input", correlationId)
                return
            }

            FlowLogger.info("WebSocketMessageHandler",
                "Generating autocomplete - Input: '$input', CWD: $cwd, PageId: $pageId, SessionId: $sessionId, CorrelationId: $correlationId")


            val context = TerminalContext(
                sessionId = sessionId,
                userId = session?.userId,
                currentDirectory = cwd,
                pageId = pageId
            )


            val suggestions = terminalInterpreter.getAutocompleteSuggestions(input, cursorPosition, context)


            val suggestionData = suggestions.map { suggestion ->
                mapOf(
                    "text" to suggestion.text,
                    "type" to suggestion.type,
                    "description" to suggestion.description,
                    "insertText" to suggestion.insertText
                )
            }

            val response = WebSocketMessage(
                type = "terminal_autocomplete_response",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(true))
                    put("suggestions", Json.encodeToJsonElement(suggestionData))
                    put("input", JsonPrimitive(input))
                    put("cursorPosition", JsonPrimitive(cursorPosition))
                    if (pageId != null) put("pageId", JsonPrimitive(pageId))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = session?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, response)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling terminal_autocomplete - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)

            val errorResponse = WebSocketMessage(
                type = "terminal_autocomplete_response",
                id = message.id,
                data = buildJsonObject {
                    put("success", JsonPrimitive(false))
                    put("suggestions", Json.encodeToJsonElement(emptyList<String>()))
                    put("error", JsonPrimitive("Error: ${e.message}"))
                    put("correlationId", JsonPrimitive(correlationId))
                },
                userId = sessionManager.getSession(sessionId)?.userId,
                sessionId = sessionId
            )

            sessionManager.sendToSession(sessionId, errorResponse)
        }
    }

    private suspend fun handleViewportUpdate(
        sessionId: String,
        message: WebSocketMessage,
        correlationId: String
    ) {
        try {
            val graphId = message.data["graphId"]?.jsonPrimitive?.content
            val scale = message.data["scale"]?.jsonPrimitive?.doubleOrNull
            val panOffsetData = message.data["panOffset"]?.jsonObject

            if (graphId.isNullOrBlank()) {
                sendErrorResponse(sessionId, message.id, "Missing graphId", correlationId)
                return
            }

            FlowLogger.debug("WebSocketMessageHandler",
                "Viewport update - GraphId: $graphId, Scale: $scale, SessionId: $sessionId, CorrelationId: $correlationId")


            val sessionData = sessionManager.getSession(sessionId)
            if (sessionData != null && scale != null && panOffsetData != null) {
                val panOffsetX = panOffsetData["dx"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val panOffsetY = panOffsetData["dy"]?.jsonPrimitive?.doubleOrNull ?: 0.0

                sessionData.viewportState = ViewportState(
                    scale = scale,
                    panOffsetX = panOffsetX,
                    panOffsetY = panOffsetY,
                    graphId = graphId
                )


                val broadcastMessage = WebSocketMessage(
                    type = "viewport_updated",
                    data = buildJsonObject {
                        put("graphId", JsonPrimitive(graphId))
                        put("scale", JsonPrimitive(scale))
                        put("panOffset", panOffsetData)
                        put("updatedBy", JsonPrimitive(sessionData.userId))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                )

                sessionManager.broadcast(broadcastMessage, excludeSessionId = sessionId)
            }

            sendSuccessResponse(sessionId, message.id, "Viewport updated successfully", correlationId)

        } catch (e: Exception) {
            FlowLogger.error("WebSocketMessageHandler",
                "Error handling viewport_update - SessionId: $sessionId, CorrelationId: $correlationId: ${e.message}", e)
            sendErrorResponse(sessionId, message.id, "Failed to update viewport: ${e.message}", correlationId)
        }
    }
}




private data class RateLimitInfo(
    val timestamps: MutableList<Long>,
    var violationCount: Int
)


data class ValidationResult(
    val isValid: Boolean,
    val error: String? = null
)


interface MessageValidator {
    fun validate(message: WebSocketMessage): ValidationResult
}


class AuthMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val userId = message.data["userId"]?.jsonPrimitive?.content
        val username = message.data["username"]?.jsonPrimitive?.content

        return when {
            userId.isNullOrBlank() -> ValidationResult(false, "Missing userId")
            username.isNullOrBlank() -> ValidationResult(false, "Missing username")
            userId.length > 100 -> ValidationResult(false, "UserId too long")
            username.length > 100 -> ValidationResult(false, "Username too long")
            else -> ValidationResult(true)
        }
    }
}


class PingMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {

        return ValidationResult(true)
    }
}


class HeartbeatMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {

        return ValidationResult(true)
    }
}


class GraphUpdateMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content

        return when {
            graphId.isNullOrBlank() -> ValidationResult(false, "Missing graphId")
            graphId.length > 255 -> ValidationResult(false, "GraphId too long")
            else -> ValidationResult(true)
        }
    }
}


class GraphSaveMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val graphData = message.data["graphData"]?.jsonObject

        return when {
            graphId.isNullOrBlank() -> ValidationResult(false, "Missing graphId")
            graphData == null -> ValidationResult(false, "Missing graphData")
            graphId.length > 255 -> ValidationResult(false, "GraphId too long")
            else -> ValidationResult(true)
        }
    }
}


class GraphLoadMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content

        return when {
            graphId.isNullOrBlank() -> ValidationResult(false, "Missing graphId")
            graphId.length > 255 -> ValidationResult(false, "GraphId too long")
            else -> ValidationResult(true)
        }
    }
}


class NodeUpdateMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val nodeId = message.data["nodeId"]?.jsonPrimitive?.content
        val nodeData = message.data["nodeData"]?.jsonObject

        return when {
            graphId.isNullOrBlank() -> ValidationResult(false, "Missing graphId")
            nodeId.isNullOrBlank() -> ValidationResult(false, "Missing nodeId")
            nodeData == null -> ValidationResult(false, "Missing nodeData")
            graphId.length > 255 -> ValidationResult(false, "GraphId too long")
            nodeId.length > 255 -> ValidationResult(false, "NodeId too long")
            else -> ValidationResult(true)
        }
    }
}


class ConnectionUpdateMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val connectionId = message.data["connectionId"]?.jsonPrimitive?.content
        val connectionData = message.data["connectionData"]?.jsonObject

        return when {
            graphId.isNullOrBlank() -> ValidationResult(false, "Missing graphId")
            connectionId.isNullOrBlank() -> ValidationResult(false, "Missing connectionId")
            connectionData == null -> ValidationResult(false, "Missing connectionData")
            graphId.length > 255 -> ValidationResult(false, "GraphId too long")
            connectionId.length > 255 -> ValidationResult(false, "ConnectionId too long")
            else -> ValidationResult(true)
        }
    }
}


class UserCursorMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val graphId = message.data["graphId"]?.jsonPrimitive?.content
        val position = message.data["position"]?.jsonObject

        return when {
            graphId.isNullOrBlank() -> ValidationResult(false, "Missing graphId")
            position == null -> ValidationResult(false, "Missing position")
            graphId.length > 255 -> ValidationResult(false, "GraphId too long")
            else -> ValidationResult(true)
        }
    }
}



class FileTreeMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val rootPath = message.data["rootPath"]?.jsonPrimitive?.content
        return when {
            rootPath != null && rootPath.length > 500 -> ValidationResult(false, "Root path too long")
            else -> ValidationResult(true)
        }
    }
}

class ReadFileMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val path = message.data["path"]?.jsonPrimitive?.content
        return when {
            path.isNullOrBlank() -> ValidationResult(false, "Missing file path")
            path.length > 500 -> ValidationResult(false, "File path too long")
            else -> ValidationResult(true)
        }
    }
}

class WriteFileMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val path = message.data["path"]?.jsonPrimitive?.content
        val content = message.data["content"]?.jsonPrimitive?.content
        return when {
            path.isNullOrBlank() -> ValidationResult(false, "Missing file path")
            content == null -> ValidationResult(false, "Missing file content")
            path.length > 500 -> ValidationResult(false, "File path too long")
            content.length > 10 * 1024 * 1024 -> ValidationResult(false, "File content too large (max 10MB)")
            else -> ValidationResult(true)
        }
    }
}

class CreateFileMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val dirPath = message.data["dirPath"]?.jsonPrimitive?.content
        val fileName = message.data["fileName"]?.jsonPrimitive?.content
        return when {
            dirPath.isNullOrBlank() -> ValidationResult(false, "Missing directory path")
            fileName.isNullOrBlank() -> ValidationResult(false, "Missing file name")
            dirPath.length > 500 -> ValidationResult(false, "Directory path too long")
            fileName.length > 255 -> ValidationResult(false, "File name too long")
            fileName.contains("/") || fileName.contains("\\") -> ValidationResult(false, "Invalid characters in file name")
            else -> ValidationResult(true)
        }
    }
}

class CreateDirectoryMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val parentPath = message.data["parentPath"]?.jsonPrimitive?.content
        val dirName = message.data["dirName"]?.jsonPrimitive?.content
        return when {
            parentPath.isNullOrBlank() -> ValidationResult(false, "Missing parent path")
            dirName.isNullOrBlank() -> ValidationResult(false, "Missing directory name")
            parentPath.length > 500 -> ValidationResult(false, "Parent path too long")
            dirName.length > 255 -> ValidationResult(false, "Directory name too long")
            dirName.contains("/") || dirName.contains("\\") -> ValidationResult(false, "Invalid characters in directory name")
            else -> ValidationResult(true)
        }
    }
}

class DeleteFileMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val path = message.data["path"]?.jsonPrimitive?.content
        return when {
            path.isNullOrBlank() -> ValidationResult(false, "Missing file path")
            path.length > 500 -> ValidationResult(false, "File path too long")
            else -> ValidationResult(true)
        }
    }
}

class DeleteDirectoryMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {
        val path = message.data["path"]?.jsonPrimitive?.content
        return when {
            path.isNullOrBlank() -> ValidationResult(false, "Missing directory path")
            path.length > 500 -> ValidationResult(false, "Directory path too long")
            else -> ValidationResult(true)
        }
    }
}

class NodeTemplatesMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {

        return ValidationResult(true)
    }
}

class GraphListMessageValidator : MessageValidator {
    override fun validate(message: WebSocketMessage): ValidationResult {

        return ValidationResult(true)
    }
}

