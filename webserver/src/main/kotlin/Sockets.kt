package com.thedevjade.flow.webserver

import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.webserver.websocket.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets(authService: AuthService) {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }


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


    val flowApi = flow.api.implementation.FlowApiImpl()
    val fileSystemAccess = flowApi.fileSystemAccess


    val currentWorkingDir = java.io.File("").absolutePath
    val projectDataDir = java.io.File("data").absolutePath


    val projectRoot = java.io.File("../").absolutePath
    val rootDir = if (java.io.File(projectRoot).exists()) projectRoot else currentWorkingDir

    if (fileSystemAccess.setRootDirectory(java.nio.file.Paths.get(rootDir))) {
        FlowLogger.debug("FlowAPI: File system root directory set to: $rootDir")
        FlowLogger.debug("FlowAPI: All files under root directory are now accessible")
    } else {
        FlowLogger.debug("FlowAPI: Warning - Failed to set root directory: $rootDir")
    }

    val sessionManager = WebSocketSessionManager()
    val dataManager = GraphDataManager()
    val graphSyncHandler = GraphSyncHandler(sessionManager, dataManager)
    val messageHandler = WebSocketMessageHandler(sessionManager, dataManager, graphSyncHandler, flowCore)


    val flowAPIInstance = com.thedevjade.flow.webserver.api.FlowAPI.getInstance(
        sessionManager,
        dataManager,
        graphSyncHandler,
        fileSystemAccess as flow.api.implementation.FileSystemAccessImpl,
        flowCore
    )


    fun debugLog(message: String) {
        val timestamp = java.time.Instant.now().toString()
        FlowLogger.debug("[$timestamp] WS-DEBUG: $message")
    }




    routing {
        webSocket("/ws") {

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


                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            try {
                                val messageText = frame.readText()
                                debugLog("Received message from $sessionId - Length: ${messageText.length} chars")

                                val message = Json.decodeFromString(WebSocketMessage.serializer(), messageText)
                                debugLog("Parsed message - Type: ${message.type}, ID: ${message.id}")

                                val startTime = System.currentTimeMillis()


                                messageHandler.handleMessage(sessionId, message)


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

                sessionManager.getSession(sessionId)?.let { sessionData ->
                    debugLog("Cleaning up session: $sessionId for user: ${sessionData.username} (${sessionData.userId})")


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


                    flowCore.websockets.unregisterConnection(sessionId)
                }

                sessionManager.removeSession(sessionId)
                debugLog("Session cleaned up: $sessionId. Active sessions: ${sessionManager.getActiveSessions().size}")
            }
        }


        get("/ws/health") {
            call.respond(
                HttpStatusCode.OK, mapOf<String, Any>(
                    "status" to "healthy",
                    "active_sessions" to sessionManager.getAllSessions().size,
                    "users_online" to sessionManager.getUsersOnline().size,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }


        get("/ws/users") {
            call.respond(
                HttpStatusCode.OK, mapOf<String, Any>(
                    "users" to sessionManager.getUsersOnline().toList(),
                    "count" to sessionManager.getUsersOnline().size,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
}
