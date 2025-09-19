package com.thedevjade.flow.webserver.websocket

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class WebSocketMessage(
    val type: String,
    val id: String? = null,
    val data: JsonObject = JsonObject(emptyMap()),
    val timestamp: String = java.time.Instant.now().toString(),
    val userId: String? = null,
    val sessionId: String? = null
)

@Serializable
data class GraphData(
    val nodes: List<GraphNode>,
    val connections: List<GraphConnection>,
    val version: String = "1.0.0",
    val metadata: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class GraphNode(
    val id: String,
    val name: String,
    val inputs: List<GraphPort>,
    val outputs: List<GraphPort>,
    val color: Long,
    val position: Position,
    val size: Size? = null,
    val templateId: String? = null,
    val properties: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class GraphPort(
    val id: String,
    val name: String,
    val isInput: Boolean,
    val color: Long
)

@Serializable
data class GraphConnection(
    val id: String,
    val fromNodeId: String,
    val fromPortId: String,
    val toNodeId: String,
    val toPortId: String,
    val color: Long
)

@Serializable
data class Position(
    val x: Double,
    val y: Double
)

@Serializable
data class Size(
    val width: Double,
    val height: Double
)

@Serializable
data class FileTreeNode(
    val name: String,
    val type: String,
    val path: String,
    val children: List<FileTreeNode>? = null,
    val size: Long? = null,
    val lastModified: Long? = null
)

@Serializable
data class UserSession(
    val userId: String,
    val sessionId: String,
    val username: String? = null,
    val connectedAt: String = java.time.Instant.now().toString(),
    val lastActivity: String = java.time.Instant.now().toString()
)

@Serializable
data class CursorPosition(
    val x: Double,
    val y: Double,
    val userId: String,
    val timestamp: String = java.time.Instant.now().toString()
)

@Serializable
data class UserSelection(
    val userId: String,
    val selectedNodes: List<String>,
    val selectedConnections: List<String>,
    val timestamp: String = java.time.Instant.now().toString()
)

@Serializable
data class ViewportState(
    val scale: Double,
    val panOffsetX: Double,
    val panOffsetY: Double,
    val graphId: String? = null
)

@Serializable
data class WorkspaceData(
    val id: String,
    val name: String,
    val currentPage: String? = null,
    val pages: List<PageData> = emptyList(),
    val settings: JsonObject = JsonObject(emptyMap()),
    val lastModified: String = java.time.Instant.now().toString(),
    val version: String = "1.0.0"
)

@Serializable
data class PageData(
    val id: String,
    val name: String,
    val type: String,
    val data: JsonObject = JsonObject(emptyMap()),
    val lastModified: String = java.time.Instant.now().toString()
)