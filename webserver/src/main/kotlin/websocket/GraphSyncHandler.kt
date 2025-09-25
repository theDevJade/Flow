package com.thedevjade.flow.webserver.websocket

import com.thedevjade.flow.common.models.FlowLogger
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*


class GraphSyncHandler(
    private val sessionManager: WebSocketSessionManager,
    private val dataManager: GraphDataManager
) {

    companion object {
        private const val MAX_BATCH_SIZE = 100
        private const val SYNC_TIMEOUT_SECONDS = 30L
    }


    suspend fun handleGraphUpdate(session: DefaultWebSocketSession, message: WebSocketMessage) {
        val correlationId = "sync_${System.currentTimeMillis()}_${(1000..9999).random()}"

        try {
            FlowLogger.debug("GraphSyncHandler",
                "Processing graph update - MessageId: ${message.id}, CorrelationId: $correlationId")

            val graphId = message.data["graphId"]?.jsonPrimitive?.content
            val updateType = message.data["updateType"]?.jsonPrimitive?.content
            val nodeJson = message.data["node"]?.jsonObject
            val connectionJson = message.data["connection"]?.jsonObject
            val batchUpdates = message.data["batchUpdates"]?.jsonArray

            if (graphId.isNullOrBlank()) {
                FlowLogger.warn("GraphSyncHandler",
                    "Graph update missing graphId - CorrelationId: $correlationId")
                sendSyncErrorResponse(session, message.id, "Missing graphId", correlationId)
                return
            }

            when (updateType) {
                "node_add", "node_update" -> {
                    if (nodeJson != null) {
                        handleNodeUpdate(graphId, nodeJson, updateType, session, message.id, correlationId)
                    } else {
                        sendSyncErrorResponse(session, message.id, "Missing node data for $updateType", correlationId)
                    }
                }
                "connection_add", "connection_update" -> {
                    if (connectionJson != null) {
                        handleConnectionUpdate(graphId, connectionJson, updateType, session, message.id, correlationId)
                    } else {
                        sendSyncErrorResponse(session, message.id, "Missing connection data for $updateType", correlationId)
                    }
                }
                "batch_update" -> {
                    if (batchUpdates != null && batchUpdates.size <= MAX_BATCH_SIZE) {
                        handleBatchUpdate(graphId, batchUpdates, session, message.id, correlationId)
                    } else {
                        sendSyncErrorResponse(session, message.id, "Invalid batch update data or size exceeded", correlationId)
                    }
                }
                "graph_sync" -> {
                    handleFullGraphSync(graphId, message.data, session, message.id, correlationId)
                }
                else -> {
                    FlowLogger.warn("GraphSyncHandler",
                        "Unknown update type: $updateType - CorrelationId: $correlationId")
                    sendSyncErrorResponse(session, message.id, "Unknown update type: $updateType", correlationId)
                }
            }

        } catch (e: Exception) {
            FlowLogger.error("GraphSyncHandler",
                "Error processing graph update - CorrelationId: $correlationId", e)
            sendSyncErrorResponse(session, message.id, "Error processing graph update: ${e.message}", correlationId)
        }
    }


    private suspend fun handleNodeUpdate(
        graphId: String,
        nodeJson: JsonObject,
        updateType: String,
        session: DefaultWebSocketSession,
        messageId: String?,
        correlationId: String
    ) = withTimeout(SYNC_TIMEOUT_SECONDS * 1000L) {
        try {
            val node = Json.decodeFromJsonElement<GraphNode>(nodeJson)

            FlowLogger.info("GraphSyncHandler",
                "Processing node update - GraphId: $graphId, NodeId: ${node.id}, Type: $updateType, CorrelationId: $correlationId")


            if (node.id.isBlank() || node.name.isBlank()) {
                sendSyncErrorResponse(session, messageId, "Invalid node data: missing id or name", correlationId)
                return@withTimeout
            }


            val currentGraph = dataManager.loadGraph(graphId) ?: GraphData(
                nodes = emptyList(),
                connections = emptyList()
            )

            val updatedNodes = if (updateType == "node_add") {
                currentGraph.nodes + node
            } else {
                currentGraph.nodes.map { if (it.id == node.id) node else it }
            }

            val updatedGraph = currentGraph.copy(nodes = updatedNodes)
            val saveSuccess = dataManager.saveGraph(graphId, updatedGraph)

            if (saveSuccess) {

                sendSyncSuccessResponse(session, messageId, "Node updated successfully", correlationId)


                broadcastGraphUpdate(graphId, updateType, nodeJson, null, correlationId, excludeSession = session)

                FlowLogger.debug("GraphSyncHandler",
                    "Node update completed successfully - GraphId: $graphId, NodeId: ${node.id}, CorrelationId: $correlationId")
            } else {
                sendSyncErrorResponse(session, messageId, "Failed to persist node update", correlationId)
            }

        } catch (e: Exception) {
            FlowLogger.error("GraphSyncHandler",
                "Error in node update - GraphId: $graphId, CorrelationId: $correlationId", e)
            sendSyncErrorResponse(session, messageId, "Node update failed: ${e.message}", correlationId)
        }
    }


    private suspend fun handleConnectionUpdate(
        graphId: String,
        connectionJson: JsonObject,
        updateType: String,
        session: DefaultWebSocketSession,
        messageId: String?,
        correlationId: String
    ) = withTimeout(SYNC_TIMEOUT_SECONDS * 1000L) {
        try {
            val connection = Json.decodeFromJsonElement<GraphConnection>(connectionJson)

            FlowLogger.info("GraphSyncHandler",
                "Processing connection update - GraphId: $graphId, ConnectionId: ${connection.id}, Type: $updateType, CorrelationId: $correlationId")


            if (connection.id.isBlank() || connection.fromNodeId.isBlank() || connection.toNodeId.isBlank()) {
                sendSyncErrorResponse(session, messageId, "Invalid connection data", correlationId)
                return@withTimeout
            }


            val currentGraph = dataManager.loadGraph(graphId) ?: GraphData(
                nodes = emptyList(),
                connections = emptyList()
            )

            val updatedConnections = if (updateType == "connection_add") {
                currentGraph.connections + connection
            } else {
                currentGraph.connections.map { if (it.id == connection.id) connection else it }
            }

            val updatedGraph = currentGraph.copy(connections = updatedConnections)
            val saveSuccess = dataManager.saveGraph(graphId, updatedGraph)

            if (saveSuccess) {
                sendSyncSuccessResponse(session, messageId, "Connection updated successfully", correlationId)
                broadcastGraphUpdate(graphId, updateType, null, connectionJson, correlationId, excludeSession = session)

                FlowLogger.debug("GraphSyncHandler",
                    "Connection update completed successfully - GraphId: $graphId, ConnectionId: ${connection.id}, CorrelationId: $correlationId")
            } else {
                sendSyncErrorResponse(session, messageId, "Failed to persist connection update", correlationId)
            }

        } catch (e: Exception) {
            FlowLogger.error("GraphSyncHandler",
                "Error in connection update - GraphId: $graphId, CorrelationId: $correlationId", e)
            sendSyncErrorResponse(session, messageId, "Connection update failed: ${e.message}", correlationId)
        }
    }


    private suspend fun handleBatchUpdate(
        graphId: String,
        batchUpdates: JsonArray,
        session: DefaultWebSocketSession,
        messageId: String?,
        correlationId: String
    ) = withTimeout(SYNC_TIMEOUT_SECONDS * 1000L) {
        try {
            FlowLogger.info("GraphSyncHandler",
                "Processing batch update - GraphId: $graphId, BatchSize: ${batchUpdates.size}, CorrelationId: $correlationId")

            val currentGraph = dataManager.loadGraph(graphId) ?: GraphData(
                nodes = emptyList(),
                connections = emptyList()
            )

            var updatedNodes = currentGraph.nodes.toMutableList()
            var updatedConnections = currentGraph.connections.toMutableList()
            val processedUpdates = mutableListOf<JsonObject>()

            for (updateElement in batchUpdates) {
                val update = updateElement.jsonObject
                val updateType = update["updateType"]?.jsonPrimitive?.content
                val nodeJson = update["node"]?.jsonObject
                val connectionJson = update["connection"]?.jsonObject

                when (updateType) {
                    "node_add", "node_update" -> {
                        if (nodeJson != null) {
                            val node = Json.decodeFromJsonElement<GraphNode>(nodeJson)
                            if (updateType == "node_add") {
                                updatedNodes.add(node)
                            } else {
                                val index = updatedNodes.indexOfFirst { it.id == node.id }
                                if (index >= 0) {
                                    updatedNodes[index] = node
                                }
                            }
                            processedUpdates.add(update)
                        }
                    }
                    "connection_add", "connection_update" -> {
                        if (connectionJson != null) {
                            val connection = Json.decodeFromJsonElement<GraphConnection>(connectionJson)
                            if (updateType == "connection_add") {
                                updatedConnections.add(connection)
                            } else {
                                val index = updatedConnections.indexOfFirst { it.id == connection.id }
                                if (index >= 0) {
                                    updatedConnections[index] = connection
                                }
                            }
                            processedUpdates.add(update)
                        }
                    }
                }
            }

            val updatedGraph = currentGraph.copy(
                nodes = updatedNodes,
                connections = updatedConnections
            )

            val saveSuccess = dataManager.saveGraph(graphId, updatedGraph)

            if (saveSuccess) {
                sendSyncSuccessResponse(session, messageId,
                    "Batch update completed: ${processedUpdates.size} updates processed", correlationId)


                val broadcastMessage = WebSocketMessage(
                    type = "graph_batch_updated",
                    data = buildJsonObject {
                        put("graphId", JsonPrimitive(graphId))
                        put("updates", JsonArray(processedUpdates))
                        put("correlationId", JsonPrimitive(correlationId))
                    }
                )

                sessionManager.broadcast(broadcastMessage, excludeSession = session)

            } else {
                sendSyncErrorResponse(session, messageId, "Failed to persist batch update", correlationId)
            }

        } catch (e: Exception) {
            FlowLogger.error("GraphSyncHandler",
                "Error in batch update - GraphId: $graphId, CorrelationId: $correlationId", e)
            sendSyncErrorResponse(session, messageId, "Batch update failed: ${e.message}", correlationId)
        }
    }


    private suspend fun handleFullGraphSync(
        graphId: String,
        syncData: JsonObject,
        session: DefaultWebSocketSession,
        messageId: String?,
        correlationId: String
    ) {
        try {
            FlowLogger.info("GraphSyncHandler",
                "Processing full graph sync - GraphId: $graphId, CorrelationId: $correlationId")

            val graphData = syncData["graphData"]?.jsonObject
            if (graphData != null) {
                val graph = Json.decodeFromJsonElement<GraphData>(graphData)
                val saveSuccess = dataManager.saveGraph(graphId, graph)

                if (saveSuccess) {
                    sendSyncSuccessResponse(session, messageId, "Full graph sync completed", correlationId)


                    val broadcastMessage = WebSocketMessage(
                        type = "graph_full_sync",
                        data = buildJsonObject {
                            put("graphId", JsonPrimitive(graphId))
                            put("correlationId", JsonPrimitive(correlationId))
                        }
                    )
                    sessionManager.broadcast(broadcastMessage, excludeSession = session)

                } else {
                    sendSyncErrorResponse(session, messageId, "Failed to persist full graph sync", correlationId)
                }
            } else {
                sendSyncErrorResponse(session, messageId, "Missing graph data for full sync", correlationId)
            }

        } catch (e: Exception) {
            FlowLogger.error("GraphSyncHandler",
                "Error in full graph sync - GraphId: $graphId, CorrelationId: $correlationId", e)
            sendSyncErrorResponse(session, messageId, "Full graph sync failed: ${e.message}", correlationId)
        }
    }


    private suspend fun broadcastGraphUpdate(
        graphId: String,
        updateType: String,
        nodeJson: JsonObject?,
        connectionJson: JsonObject?,
        correlationId: String,
        excludeSession: DefaultWebSocketSession
    ) {
        val broadcastData = buildJsonObject {
            put("graphId", JsonPrimitive(graphId))
            put("updateType", JsonPrimitive(updateType))
            put("correlationId", JsonPrimitive(correlationId))
            put("timestamp", JsonPrimitive(System.currentTimeMillis()))

            if (nodeJson != null) {
                put("node", nodeJson)
            }
            if (connectionJson != null) {
                put("connection", connectionJson)
            }
        }

        val broadcastMessage = WebSocketMessage(
            type = "graph_update_broadcast",
            data = broadcastData
        )

        try {
            sessionManager.broadcast(broadcastMessage, excludeSession = excludeSession)
            FlowLogger.debug("GraphSyncHandler",
                "Graph update broadcasted - GraphId: $graphId, UpdateType: $updateType, CorrelationId: $correlationId")
        } catch (e: Exception) {
            FlowLogger.error("GraphSyncHandler",
                "Failed to broadcast graph update - CorrelationId: $correlationId", e)
        }
    }


    private suspend fun sendSyncSuccessResponse(
        session: DefaultWebSocketSession,
        messageId: String?,
        message: String,
        correlationId: String
    ) {
        val response = WebSocketMessage(
            type = "graph_sync_success",
            id = messageId,
            data = buildJsonObject {
                put("success", JsonPrimitive(true))
                put("message", JsonPrimitive(message))
                put("correlationId", JsonPrimitive(correlationId))
                put("timestamp", JsonPrimitive(System.currentTimeMillis()))
            }
        )

        try {
            val jsonMessage = Json.encodeToString(WebSocketMessage.serializer(), response)
            session.send(Frame.Text(jsonMessage))
        } catch (e: Exception) {
            FlowLogger.error("GraphSyncHandler",
                "Failed to send sync success response - CorrelationId: $correlationId", e)
        }
    }


    private suspend fun sendSyncErrorResponse(
        session: DefaultWebSocketSession,
        messageId: String?,
        error: String,
        correlationId: String
    ) {
        val response = WebSocketMessage(
            type = "graph_sync_error",
            id = messageId,
            data = buildJsonObject {
                put("success", JsonPrimitive(false))
                put("error", JsonPrimitive(error))
                put("correlationId", JsonPrimitive(correlationId))
                put("timestamp", JsonPrimitive(System.currentTimeMillis()))
            }
        )

        try {
            val jsonMessage = Json.encodeToString(WebSocketMessage.serializer(), response)
            session.send(Frame.Text(jsonMessage))
        } catch (e: Exception) {
            FlowLogger.error("GraphSyncHandler",
                "Failed to send sync error response - CorrelationId: $correlationId", e)
        }
    }
}
