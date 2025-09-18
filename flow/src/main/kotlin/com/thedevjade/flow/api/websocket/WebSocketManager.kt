package com.thedevjade.flow.api.websocket

import com.thedevjade.flow.api.FlowConfig
import com.thedevjade.flow.api.events.EventManager
import com.thedevjade.flow.api.events.WebSocketEvent
import com.thedevjade.flow.api.graph.GraphManager
import com.thedevjade.flow.api.user.UserManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap


class WebSocketManager(
    private val eventManager: EventManager,
    private val userManager: UserManager,
    private val graphManager: GraphManager,
    private val config: FlowConfig
) {


    private val connections = ConcurrentHashMap<String, WebSocketConnection>()
    private val userConnections = ConcurrentHashMap<String, MutableSet<String>>()
    private val connectionMutex = Mutex()


    private val messageHandlers = ConcurrentHashMap<String, MessageHandler>()


    private var totalMessagesReceived = 0L
    private var totalMessagesSent = 0L
    private var totalBytesReceived = 0L
    private var totalBytesSent = 0L

    init {
        registerDefaultHandlers()
    }


    suspend fun registerConnection(
        connectionId: String,
        userId: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): ConnectionRegistrationResult = connectionMutex.withLock {

        if (connections.containsKey(connectionId)) {
            return ConnectionRegistrationResult.Failure("Connection already exists")
        }

        val connection = WebSocketConnection(
            id = connectionId,
            userId = userId,
            connectedAt = System.currentTimeMillis(),
            lastActivityAt = System.currentTimeMillis(),
            metadata = metadata.toMutableMap()
        )

        connections[connectionId] = connection

        if (userId != null) {
            userConnections.getOrPut(userId) { mutableSetOf() }.add(connectionId)
        }

        eventManager.emit(WebSocketEvent.ConnectionOpened(connectionId, userId, System.currentTimeMillis()))

        ConnectionRegistrationResult.Success(connection)
    }


    suspend fun unregisterConnection(connectionId: String) = connectionMutex.withLock {
        val connection = connections.remove(connectionId)
        if (connection != null) {
            connection.userId?.let { userId ->
                userConnections[userId]?.remove(connectionId)
                if (userConnections[userId]?.isEmpty() == true) {
                    userConnections.remove(userId)
                }
                userManager.removeSession(connectionId)
            }

            eventManager.emit(WebSocketEvent.ConnectionClosed(connectionId, connection.userId, System.currentTimeMillis()))
        }
    }


    suspend fun processMessage(
        connectionId: String,
        messageType: String,
        messageData: Map<String, Any>,
        messageId: String? = null
    ): MessageProcessingResult {

        val connection = connections[connectionId]
            ?: return MessageProcessingResult.Failure("Connection not found")


        connection.lastActivityAt = System.currentTimeMillis()
        totalMessagesReceived++
        totalBytesReceived += messageData.toString().toByteArray().size

        eventManager.emit(WebSocketEvent.MessageReceived(
            connectionId, messageType, messageData.toString().length, System.currentTimeMillis()
        ))


        val handler = messageHandlers[messageType]
            ?: return MessageProcessingResult.Failure("Unknown message type: $messageType")

        return try {
            val context = MessageContext(
                connectionId = connectionId,
                userId = connection.userId,
                messageId = messageId,
                timestamp = System.currentTimeMillis()
            )

            handler.handle(context, messageData)
        } catch (e: Exception) {
            eventManager.emit(WebSocketEvent.ConnectionError(connectionId, e.message ?: "Unknown error", System.currentTimeMillis()))
            MessageProcessingResult.Failure("Error processing message: ${e.message}")
        }
    }


    suspend fun sendMessage(
        connectionId: String,
        messageType: String,
        data: Map<String, Any>,
        messageId: String? = null
    ): Boolean {
        val connection = connections[connectionId] ?: return false

        totalMessagesSent++
        totalBytesSent += data.toString().toByteArray().size

        eventManager.emit(WebSocketEvent.MessageSent(
            connectionId, messageType, data.toString().length, System.currentTimeMillis()
        ))

        // SEND VIA ACTUAL WEBSOCKET @TODO
        connection.lastActivityAt = System.currentTimeMillis()

        return true
    }


    suspend fun broadcastMessage(
        messageType: String,
        data: Map<String, Any>,
        excludeConnectionId: String? = null
    ): Int {
        var sentCount = 0

        connections.values.forEach { connection ->
            if (connection.id != excludeConnectionId) {
                if (sendMessage(connection.id, messageType, data)) {
                    sentCount++
                }
            }
        }

        return sentCount
    }


    suspend fun sendMessageToUser(
        userId: String,
        messageType: String,
        data: Map<String, Any>,
        messageId: String? = null
    ): Int {
        val connectionIds = userConnections[userId] ?: return 0
        var sentCount = 0

        connectionIds.forEach { connectionId ->
            if (sendMessage(connectionId, messageType, data, messageId)) {
                sentCount++
            }
        }

        return sentCount
    }


    suspend fun sendMessageToGraphUsers(
        graphId: String,
        messageType: String,
        data: Map<String, Any>,
        excludeUserId: String? = null
    ): Int {
        var sentCount = 0

        userConnections.keys.forEach { userId ->
            if (userId != excludeUserId && graphManager.hasReadAccess(graphId, userId)) {
                sentCount += sendMessageToUser(userId, messageType, data)
            }
        }

        return sentCount
    }


    fun registerMessageHandler(messageType: String, handler: MessageHandler) {
        messageHandlers[messageType] = handler
    }

    
    fun unregisterMessageHandler(messageType: String) {
        messageHandlers.remove(messageType)
    }

    
    fun getConnection(connectionId: String): WebSocketConnection? = connections[connectionId]

    
    fun getUserConnections(userId: String): List<WebSocketConnection> {
        val connectionIds = userConnections[userId] ?: return emptyList()
        return connectionIds.mapNotNull { connections[it] }
    }

    
    fun getAllConnections(): List<WebSocketConnection> = connections.values.toList()

    
    fun getActiveConnectionCount(): Int = connections.size

    
    fun getConnectionStatistics(): ConnectionStatistics {
        return ConnectionStatistics(
            totalConnections = connections.size,
            totalUsers = userConnections.size,
            totalMessagesReceived = totalMessagesReceived,
            totalMessagesSent = totalMessagesSent,
            totalBytesReceived = totalBytesReceived,
            totalBytesSent = totalBytesSent,
            registeredHandlers = messageHandlers.size
        )
    }

    
    suspend fun cleanupInactiveConnections(timeoutMs: Long = 300000) {
        val now = System.currentTimeMillis()
        val inactiveConnections = connections.values
            .filter { now - it.lastActivityAt > timeoutMs }
            .map { it.id }

        inactiveConnections.forEach { connectionId ->
            unregisterConnection(connectionId)
        }
    }

    
    private fun registerDefaultHandlers() {
        registerMessageHandler("auth") { context, data ->
            val userId = data["userId"]?.toString()?.removeSurrounding("\"")
            val username = data["username"]?.toString()?.removeSurrounding("\"")

            if (userId != null && username != null) {
                val authResult = userManager.authenticateUser(userId, context.connectionId, username)
                when (authResult) {
                    is com.thedevjade.flow.api.user.AuthenticationResult.Success -> {

                        connections[context.connectionId]?.userId = userId
                        userConnections.getOrPut(userId) { mutableSetOf() }.add(context.connectionId)

                        MessageProcessingResult.Success(mapOf(
                            "success" to true,
                            "userId" to userId,
                            "username" to username,
                            "token" to authResult.token
                        ))
                    }
                    is com.thedevjade.flow.api.user.AuthenticationResult.Failure -> {
                        MessageProcessingResult.Failure(authResult.error)
                    }
                }
            } else {
                MessageProcessingResult.Failure("Missing userId or username")
            }
        }


        registerMessageHandler("ping") { context, _ ->
            MessageProcessingResult.Success(mapOf(
                "type" to "pong",
                "timestamp" to System.currentTimeMillis()
            ))
        }


        registerMessageHandler("heartbeat") { context, _ ->
            connections[context.connectionId]?.let { connection ->
                connection.lastActivityAt = System.currentTimeMillis()
                connection.metadata["lastHeartbeat"] = System.currentTimeMillis()
                connection.metadata["heartbeatCount"] =
                    (connection.metadata["heartbeatCount"] as? Int ?: 0) + 1
            }

            MessageProcessingResult.Success(mapOf(
                "type" to "heartbeat_ack",
                "timestamp" to System.currentTimeMillis(),
                "status" to "alive"
            ))
        }



    }

    
    fun dispose() {
        connections.clear()
        userConnections.clear()
        messageHandlers.clear()
    }
}


data class WebSocketConnection(
    val id: String,
    var userId: String? = null,
    val connectedAt: Long,
    var lastActivityAt: Long,
    val metadata: MutableMap<String, Any> = mutableMapOf()
)


data class MessageContext(
    val connectionId: String,
    val userId: String?,
    val messageId: String?,
    val timestamp: Long
)


fun interface MessageHandler {
    suspend fun handle(context: MessageContext, data: Map<String, Any>): MessageProcessingResult
}


sealed class ConnectionRegistrationResult {
    data class Success(val connection: WebSocketConnection) : ConnectionRegistrationResult()
    data class Failure(val error: String) : ConnectionRegistrationResult()
}


sealed class MessageProcessingResult {
    data class Success(val data: Map<String, Any> = emptyMap()) : MessageProcessingResult()
    data class Failure(val error: String) : MessageProcessingResult()
}


data class ConnectionStatistics(
    val totalConnections: Int,
    val totalUsers: Int,
    val totalMessagesReceived: Long,
    val totalMessagesSent: Long,
    val totalBytesReceived: Long,
    val totalBytesSent: Long,
    val registeredHandlers: Int
)