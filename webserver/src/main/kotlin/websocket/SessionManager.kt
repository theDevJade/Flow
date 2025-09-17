package com.thedevjade.flow.webserver.websocket

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.UUID

class WebSocketSessionManager {
    private val sessions = ConcurrentHashMap<String, WebSocketSessionData>()
    private val userSessions = ConcurrentHashMap<String, MutableSet<String>>()
    private val nextUserId = AtomicInteger(1)
    
    fun addSession(session: DefaultWebSocketSession, userId: String? = null, username: String? = null): String {
        val sessionId = UUID.randomUUID().toString()
        val actualUserId = userId ?: "user_${nextUserId.getAndIncrement()}"
        val actualUsername = username ?: "Anonymous User ${actualUserId}"
        
        val sessionData = WebSocketSessionData(
            sessionId = sessionId,
            userId = actualUserId,
            username = actualUsername,
            session = session,
            connectedAt = System.currentTimeMillis(),
            isAuthenticated = userId != null && username != null
        )
        
        sessions[sessionId] = sessionData
        userSessions.computeIfAbsent(actualUserId) { mutableSetOf() }.add(sessionId)
        
        println("Added WebSocket session: $sessionId for user: $actualUsername ($actualUserId), authenticated: ${sessionData.isAuthenticated}")
        return sessionId
    }
    
    fun removeSession(sessionId: String) {
        sessions[sessionId]?.let { sessionData ->
            sessions.remove(sessionId)
            userSessions[sessionData.userId]?.remove(sessionId)
            if (userSessions[sessionData.userId]?.isEmpty() == true) {
                userSessions.remove(sessionData.userId)
            }
            println("Removed WebSocket session: $sessionId for user: ${sessionData.username} (${sessionData.userId})")
        }
    }
    
    fun getSession(sessionId: String): WebSocketSessionData? = sessions[sessionId]
    
    fun getAllSessions(): Collection<WebSocketSessionData> = sessions.values
    
    fun getSessionsForUser(userId: String): List<WebSocketSessionData> {
        return userSessions[userId]?.mapNotNull { sessions[it] } ?: emptyList()
    }
    
    fun getUsersOnline(): Set<String> = userSessions.keys.toSet()
    
    fun getActiveSessions(): Collection<WebSocketSessionData> = sessions.values
    
    suspend fun broadcast(message: WebSocketMessage, excludeSessionId: String? = null): Int {
        val jsonMessage = Json.encodeToString(WebSocketMessage.serializer(), message)
        var sentCount = 0
        sessions.values.forEach { sessionData ->
            if (sessionData.sessionId != excludeSessionId) {
                try {
                    sessionData.session.send(Frame.Text(jsonMessage))
                    sentCount++
                } catch (e: Exception) {
                    println("Failed to send message to session ${sessionData.sessionId}: ${e.message}")
                    removeSession(sessionData.sessionId)
                }
            }
        }
        return sentCount
    }
    
    suspend fun broadcast(message: WebSocketMessage, excludeSession: DefaultWebSocketSession? = null): Int {
        val jsonMessage = Json.encodeToString(WebSocketMessage.serializer(), message)
        var sentCount = 0
        sessions.values.forEach { sessionData ->
            if (sessionData.session != excludeSession) {
                try {
                    sessionData.session.send(Frame.Text(jsonMessage))
                    sentCount++
                } catch (e: Exception) {
                    println("Failed to send message to session ${sessionData.sessionId}: ${e.message}")
                    removeSession(sessionData.sessionId)
                }
            }
        }
        return sentCount
    }
    
    suspend fun sendToUser(userId: String, message: WebSocketMessage) {
        val jsonMessage = Json.encodeToString(WebSocketMessage.serializer(), message)
        getSessionsForUser(userId).forEach { sessionData ->
            try {
                sessionData.session.send(Frame.Text(jsonMessage))
            } catch (e: Exception) {
                println("Failed to send message to user $userId session ${sessionData.sessionId}: ${e.message}")
                removeSession(sessionData.sessionId)
            }
        }
    }
    
    suspend fun sendToSession(sessionId: String, message: WebSocketMessage) {
        val sessionData = getSession(sessionId) ?: return
        val jsonMessage = Json.encodeToString(WebSocketMessage.serializer(), message)
        try {
            sessionData.session.send(Frame.Text(jsonMessage))
        } catch (e: Exception) {
            println("Failed to send message to session $sessionId: ${e.message}")
            removeSession(sessionId)
        }
    }
}

data class WebSocketSessionData(
    val sessionId: String,
    val userId: String,
    val username: String,
    val session: DefaultWebSocketSession,
    val connectedAt: Long,
    var lastActivity: Long = System.currentTimeMillis(),
    var isAuthenticated: Boolean = false,
    var viewportState: ViewportState? = null,
    var metadata: MutableMap<String, Any> = mutableMapOf()
)