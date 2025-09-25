package com.thedevjade.flow.webserver.api

import kotlinx.serialization.json.JsonElement


interface CustomCommandHandler {

    val commandId: String


    val displayName: String


    val description: String


    val requiresAuth: Boolean get() = true

    /**
     * Execute the command with the provided data
     * @param sessionId The WebSocket session that triggered the command
     * @param userId The ID of the user executing the command (if authenticated)
     * @param data The command data/parameters
     * @return CommandResult indicating success/failure and any response data
     */
    suspend fun execute(sessionId: String, userId: String?, data: JsonElement): CommandResult

    /**
     * Validate if the command can be executed with the given parameters
     * @param data The command data/parameters to validate
     * @return ValidationResult indicating if the command is valid and any error messages
     */
    fun validate(data: JsonElement): ValidationResult = ValidationResult.success()
}


data class CommandResult(
    val success: Boolean,
    val message: String? = null,
    val data: JsonElement? = null,
    val errorCode: String? = null
) {
    companion object {
        fun success(message: String? = null, data: JsonElement? = null) =
            CommandResult(true, message, data)

        fun failure(message: String, errorCode: String? = null, data: JsonElement? = null) =
            CommandResult(false, message, data, errorCode)
    }
}


data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val missingFields: List<String> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(message: String, missingFields: List<String> = emptyList()) =
            ValidationResult(false, message, missingFields)
    }
}


data class UserSession(
    val sessionId: String,
    val userId: String?,
    val isAuthenticated: Boolean,
    val connectedAt: Long,
    val lastActivity: Long,
    val metadata: Map<String, String> = emptyMap()
)


sealed class SessionEvent {
    abstract val sessionId: String
    abstract val timestamp: Long

    data class Connected(
        override val sessionId: String,
        val userId: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : SessionEvent()

    data class Disconnected(
        override val sessionId: String,
        val userId: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : SessionEvent()

    data class Authenticated(
        override val sessionId: String,
        val userId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : SessionEvent()

    data class ActivityUpdate(
        override val sessionId: String,
        val userId: String?,
        override val timestamp: Long = System.currentTimeMillis()
    ) : SessionEvent()
}


interface SessionEventListener {
    suspend fun onSessionEvent(event: SessionEvent)
}


data class CustomCommandInfo(
    val commandId: String,
    val displayName: String,
    val description: String,
    val requiresAuth: Boolean
)


data class FlowAPIStatus(
    val activeSessions: Int,
    val registeredCommands: Int,
    val allowedDirectories: Int,
    val rootAccessDirectory: String?,
    val activeUserSessions: Int,
    val authenticatedSessions: Int
)