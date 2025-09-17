package com.thedevjade.flow.webserver.api

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import com.thedevjade.flow.webserver.websocket.WebSocketMessage
import com.thedevjade.flow.webserver.websocket.WebSocketSessionManager
import flow.api.implementation.FileSystemAccessImpl


class FlowAPIIntegration(
    private val sessionManager: WebSocketSessionManager,
    private val dataManager: com.thedevjade.flow.webserver.websocket.GraphDataManager,
    private val graphSyncHandler: com.thedevjade.flow.webserver.websocket.GraphSyncHandler,
    private val fileSystemAccessImpl: FileSystemAccessImpl
) {
    private val flowAPI = FlowAPI.getInstance(sessionManager, dataManager, graphSyncHandler, fileSystemAccessImpl)


    fun initialize() {

        flowAPI.setFileExplorerRootDirectory(".")


        registerBuiltInCommands()


        flowAPI.addSessionEventListener(LoggingSessionListener())

        println("FlowAPI Integration initialized")
    }


    suspend fun handleSessionConnect(sessionId: String, session: DefaultWebSocketSession, userId: String?) {

        flowAPI.registerSession(sessionId, session)


        flowAPI.updateUserSession(sessionId, userId, userId != null)


        sendAvailableCommands(sessionId)
    }


    suspend fun handleSessionDisconnect(sessionId: String) {
        flowAPI.unregisterSession(sessionId)
        flowAPI.removeUserSession(sessionId)
    }


    suspend fun handleCommandExecution(
        sessionId: String,
        userId: String?,
        message: WebSocketMessage
    ): WebSocketMessage {
        val messageData = message.data as? JsonObject
        val commandId = messageData?.get("commandId")?.jsonPrimitive?.content
        val commandData = messageData?.get("data") ?: JsonNull

        if (commandId == null) {
            return WebSocketMessage(
                id = "cmd_response_${System.currentTimeMillis()}",
                type = "command_error",
                data = buildJsonObject {
                    put("error", "Missing commandId in request")
                    put("originalMessageId", message.id ?: "unknown")
                },
                timestamp = java.time.Instant.now().toString(),
                userId = userId
            )
        }

        val result = flowAPI.executeCommand(commandId, sessionId, userId, commandData)

        return WebSocketMessage(
            id = "cmd_response_${System.currentTimeMillis()}",
            type = "command_response",
            data = buildJsonObject {
                put("commandId", commandId)
                put("success", result.success)
                result.message?.let { put("message", it) }
                result.errorCode?.let { put("errorCode", it) }
                result.data?.let { put("data", it) }
                put("originalMessageId", message.id ?: "unknown")
            },
            timestamp = java.time.Instant.now().toString(),
            userId = userId
        )
    }


    private suspend fun sendAvailableCommands(sessionId: String) {
        val commands = flowAPI.getRegisteredCommands()
        val message = WebSocketMessage(
            id = "available_commands_${System.currentTimeMillis()}",
            type = "available_commands",
            data = buildJsonObject {
                put("commands", buildJsonArray {
                    commands.forEach { command ->
                        add(buildJsonObject {
                            put("commandId", command.commandId)
                            put("displayName", command.displayName)
                            put("description", command.description)
                            put("requiresAuth", command.requiresAuth)
                        })
                    }
                })
            },
            timestamp = java.time.Instant.now().toString()
        )

        flowAPI.sendMessageToSession(sessionId, message)
    }


    private fun registerBuiltInCommands() {

        flowAPI.registerCommand(ListDirectoriesCommand())
        flowAPI.registerCommand(GetFileInfoCommand())


        flowAPI.registerCommand(GetAPIStatusCommand())
        flowAPI.registerCommand(ListActiveSessionsCommand())

        println("FlowAPI: Registered built-in commands")
    }


    private class LoggingSessionListener : SessionEventListener {
        override suspend fun onSessionEvent(event: SessionEvent) {
            when (event) {
                is SessionEvent.Connected -> {
                    println("FlowAPI Session: User ${event.userId ?: "anonymous"} connected (${event.sessionId})")
                }
                is SessionEvent.Disconnected -> {
                    println("FlowAPI Session: User ${event.userId ?: "anonymous"} disconnected (${event.sessionId})")
                }
                is SessionEvent.Authenticated -> {
                    println("FlowAPI Session: User ${event.userId} authenticated (${event.sessionId})")
                }
                is SessionEvent.ActivityUpdate -> {

                }
            }
        }
    }
}




private class ListDirectoriesCommand : CustomCommandHandler {
    override val commandId = "list_directories"
    override val displayName = "List Directories"
    override val description = "Get list of directories accessible through the Flow API"
    override val requiresAuth = false

    override suspend fun execute(sessionId: String, userId: String?, data: JsonElement): CommandResult {
        val flowAPI = FlowAPI.getInstance()
        val directories = flowAPI.getAllowedDirectories()
        val rootDir = flowAPI.getRootAccessDirectory()

        return CommandResult.success(
            "Retrieved directory list",
            buildJsonObject {
                put("rootDirectory", rootDir)
                put("allowedDirectories", buildJsonArray {
                    directories.forEach { add(it) }
                })
            }
        )
    }
}


private class GetFileInfoCommand : CustomCommandHandler {
    override val commandId = "get_file_info"
    override val displayName = "Get File Info"
    override val description = "Get information about a specific file or directory"

    override fun validate(data: JsonElement): ValidationResult {
        val jsonObject = data as? JsonObject
        val filePath = jsonObject?.get("filePath")?.jsonPrimitive?.content

        return if (filePath.isNullOrBlank()) {
            ValidationResult.failure("Missing required field: filePath", listOf("filePath"))
        } else {
            ValidationResult.success()
        }
    }

    override suspend fun execute(sessionId: String, userId: String?, data: JsonElement): CommandResult {
        val jsonObject = data as JsonObject
        val filePath = jsonObject["filePath"]!!.jsonPrimitive.content

        val flowAPI = FlowAPI.getInstance()
        if (!flowAPI.isFileAccessAllowed(filePath)) {
            return CommandResult.failure("Access denied to file: $filePath", "ACCESS_DENIED")
        }

        val file = java.io.File(filePath)
        if (!file.exists()) {
            return CommandResult.failure("File not found: $filePath", "FILE_NOT_FOUND")
        }

        return CommandResult.success(
            "Retrieved file information",
            buildJsonObject {
                put("path", file.absolutePath)
                put("name", file.name)
                put("isDirectory", file.isDirectory)
                put("isFile", file.isFile)
                put("size", if (file.isFile) file.length() else null)
                put("lastModified", file.lastModified())
                put("canRead", file.canRead())
                put("canWrite", file.canWrite())
            }
        )
    }
}


private class GetAPIStatusCommand : CustomCommandHandler {
    override val commandId = "get_api_status"
    override val displayName = "Get API Status"
    override val description = "Get current status and statistics of the Flow API"
    override val requiresAuth = false

    override suspend fun execute(sessionId: String, userId: String?, data: JsonElement): CommandResult {
        val flowAPI = FlowAPI.getInstance()
        val status = flowAPI.getAPIStatus()

        return CommandResult.success(
            "Retrieved API status",
            buildJsonObject {
                put("activeSessions", status.activeSessions)
                put("registeredCommands", status.registeredCommands)
                put("allowedDirectories", status.allowedDirectories)
                put("rootAccessDirectory", status.rootAccessDirectory)
                put("activeUserSessions", status.activeUserSessions)
                put("authenticatedSessions", status.authenticatedSessions)
                put("timestamp", System.currentTimeMillis())
            }
        )
    }
}

/**
 * Command to list active sessions
 */
private class ListActiveSessionsCommand : CustomCommandHandler {
    override val commandId = "list_active_sessions"
    override val displayName = "List Active Sessions"
    override val description = "Get list of all active user sessions"
    override val requiresAuth = true

    override suspend fun execute(sessionId: String, userId: String?, data: JsonElement): CommandResult {
        val flowAPI = FlowAPI.getInstance()
        val sessions = flowAPI.getAllUserSessions()

        return CommandResult.success(
            "Retrieved active sessions",
            buildJsonObject {
                put("sessions", buildJsonArray {
                    sessions.forEach { session ->
                        add(buildJsonObject {
                            put("sessionId", session.sessionId)
                            put("userId", session.userId)
                            put("isAuthenticated", session.isAuthenticated)
                            put("connectedAt", session.connectedAt)
                            put("lastActivity", session.lastActivity)
                        })
                    }
                })
                put("totalSessions", sessions.size)
            }
        )
    }
}