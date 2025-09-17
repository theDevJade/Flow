package com.thedevjade.flow.webserver

import com.thedevjade.flow.webserver.websocket.*
import flow.api.implementation.FlowApiImpl
import io.ktor.http.*
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets(authService: AuthService) {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // Initialize managers with new Flow API
    val flowCore = com.thedevjade.flow.api.FlowCore.initialize(
        com.thedevjade.flow.api.FlowConfig(
            dataDirectory = "data",
            maxUsers = 1000,
            maxGraphsPerUser = 50,
            webSocketPort = 9090,
            enableMetrics = true,
            enableLogging = true
        )
    )

    // Initialize file system access with project root directory
    val flowApi = flow.api.implementation.FlowApiImpl()
    val fileSystemAccess = flowApi.fileSystemAccess
    
    // Set the root directory to the project root (where data directory is located)
    val currentWorkingDir = java.io.File("").absolutePath
    val projectDataDir = java.io.File("data").absolutePath
    
    // Set root directory to current working directory (allowing access to all project files)
    if (fileSystemAccess.setRootDirectory(java.nio.file.Paths.get(currentWorkingDir))) {
        println("FlowAPI: File system root directory set to: $currentWorkingDir")
        println("FlowAPI: All files under root directory are now accessible")
    } else {
        println("FlowAPI: Warning - Failed to set root directory: $currentWorkingDir")
    }

    val sessionManager = WebSocketSessionManager()
    val dataManager = GraphDataManager()
    val graphSyncHandler = GraphSyncHandler(sessionManager, dataManager)
    val messageHandler = WebSocketMessageHandler(sessionManager, dataManager, graphSyncHandler)

    // Debug logging helper
    fun debugLog(message: String) {
        val timestamp = java.time.Instant.now().toString()
        println("[$timestamp] WS-DEBUG: $message")
    }

    // Get auth service from parameter
    // val authService = this.getAuthService()

    routing {
        webSocket("/ws") {
            // Extract token from query parameters or headers
            val token = call.request.queryParameters["token"]
                ?: call.request.headers["Authorization"]?.removePrefix("Bearer ")

            if (token == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Authentication token required"))
                return@webSocket
            }

            val authToken = authService.validateToken(token)
            if (authToken == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid or expired token"))
                return@webSocket
            }

            val sessionId = sessionManager.addSession(this, authToken.userId, authToken.username)

            try {
                debugLog("New WebSocket connection established - SessionID: $sessionId, User: ${authToken.username} (${authToken.userId})")
                debugLog("Active sessions count: ${sessionManager.getActiveSessions().size}")

                // Send welcome message
                val welcomeMessage = WebSocketMessage(
                    type = "connection_established",
                    data = buildJsonObject {
                        put("sessionId", sessionId)
                        put("userId", authToken.userId)
                        put("username", authToken.username)
                        put("server_version", "1.0.0")
                        put("timestamp", System.currentTimeMillis())
                        put("active_sessions", sessionManager.getActiveSessions().size)
                        put("authenticated", true)
                    }
                )
                send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(), welcomeMessage)))
                debugLog("Welcome message sent to session: $sessionId")

                // Handle incoming messages
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            try {
                                val messageText = frame.readText()
                                debugLog("Received message from $sessionId - Length: ${messageText.length} chars")

                                val message = Json.decodeFromString(WebSocketMessage.serializer(), messageText)
                                debugLog("Parsed message - Type: ${message.type}, ID: ${message.id}")

                                val startTime = System.currentTimeMillis()

                                // Process message using enhanced handler
                                messageHandler.handleMessage(sessionId, message)

                                // Also register with Flow API if it's a connection event
                                if (message.type == "auth" && message.data["userId"] != null) {
                                    val userId = message.data["userId"]?.jsonPrimitive?.content
                                    if (userId != null) {
                                        flowCore.websockets.registerConnection(
                                            sessionId,
                                            userId,
                                            mapOf("userAgent" to "Flow-WebSocket-Client")
                                        )
                                    }
                                }

                                val processingTime = System.currentTimeMillis() - startTime
                                debugLog("Message processed - Type: ${message.type}, Time: ${processingTime}ms")

                            } catch (e: JsonConvertException) {
                                debugLog("JSON decoding error for session $sessionId: ${e.message}")

                                // Send detailed error response for JSON parsing errors
                                val errorMessage = WebSocketMessage(
                                    type = "parse_error",
                                    data = buildJsonObject {
                                        put("error", "Invalid JSON format")
                                        put("details", e.message ?: "JSON parsing failed")
                                        put("timestamp", System.currentTimeMillis())
                                    }
                                )
                                send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(), errorMessage)))

                            } catch (e: Exception) {
                                debugLog("Error parsing message from $sessionId: ${e.message}")
                                debugLog("Stack trace: ${e.stackTraceToString()}")

                                val errorMessage = WebSocketMessage(
                                    type = "error",
                                    data = buildJsonObject {
                                        put("error", "Invalid message format")
                                        put("details", e.message ?: "Unknown error")
                                        put("timestamp", System.currentTimeMillis())
                                    }
                                )
                                send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(), errorMessage)))
                            }
                        }
                        is Frame.Binary -> {
                            debugLog("Received binary frame from $sessionId - Size: ${frame.data.size} bytes (not supported)")
                        }
                        is Frame.Close -> {
                            debugLog("Close frame received from session: $sessionId - Code: ${frame.readReason()?.code}, Reason: ${frame.readReason()?.message}")
                            break
                        }
                        else -> {
                            debugLog("Received unknown frame type from $sessionId: ${frame.frameType}")
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                debugLog("WebSocket connection closed normally - SessionID: $sessionId")
            } catch (e: Exception) {
                debugLog("WebSocket connection error - SessionID: $sessionId, Error: ${e.message}")
                debugLog("Stack trace: ${e.stackTraceToString()}")
            } finally {
                // Clean up session
                sessionManager.getSession(sessionId)?.let { sessionData ->
                    debugLog("Cleaning up session: $sessionId for user: ${sessionData.username} (${sessionData.userId})")

                    // Notify other users that this user left
                    val userLeftMessage = WebSocketMessage(
                        type = "user_left",
                        data = buildJsonObject {
                            put("userId", sessionData.userId)
                            put("username", sessionData.username)
                            put("sessionId", sessionId)
                            put("timestamp", System.currentTimeMillis())
                        }
                    )
                    val broadcastCount = sessionManager.broadcast(userLeftMessage, excludeSessionId = sessionId)
                    debugLog("User left notification sent to $broadcastCount sessions")

                    // Cleanup from Flow API
                    flowCore.websockets.unregisterConnection(sessionId)
                }

                sessionManager.removeSession(sessionId)
                debugLog("Session cleaned up: $sessionId. Active sessions: ${sessionManager.getActiveSessions().size}")
            }
        }

        // Health check endpoint
        get("/ws/health") {
            call.respond(HttpStatusCode.OK, mapOf<String, Any>(
                "status" to "healthy",
                "active_sessions" to sessionManager.getAllSessions().size,
                "users_online" to sessionManager.getUsersOnline().size,
                "timestamp" to System.currentTimeMillis()
            ))
        }

        // Get active users endpoint
        get("/ws/users") {
            call.respond(HttpStatusCode.OK, mapOf<String, Any>(
                "users" to sessionManager.getUsersOnline().toList(),
                "count" to sessionManager.getUsersOnline().size,
                "timestamp" to System.currentTimeMillis()
            ))
        }
    }
}
