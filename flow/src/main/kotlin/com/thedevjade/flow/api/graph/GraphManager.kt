package com.thedevjade.flow.api.graph

import com.thedevjade.flow.api.FlowConfig
import com.thedevjade.flow.api.events.EventManager
import com.thedevjade.flow.api.events.GraphEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap


class GraphManager(
    private val eventManager: EventManager,
    private val config: FlowConfig
) {


    private val graphs = ConcurrentHashMap<String, FlowGraph>()
    private val userGraphs = ConcurrentHashMap<String, MutableSet<String>>()
    private val graphCollaborators = ConcurrentHashMap<String, MutableSet<String>>()
    private val graphMutex = Mutex()


    private val graphVersions = ConcurrentHashMap<String, MutableList<GraphVersion>>()


    suspend fun createGraph(
        graphId: String,
        ownerId: String,
        name: String,
        description: String = "",
        isPublic: Boolean = false
    ): GraphCreationResult = graphMutex.withLock {

        if (graphs.containsKey(graphId)) {
            return GraphCreationResult.Failure("Graph already exists")
        }

        val userGraphCount = userGraphs[ownerId]?.size ?: 0
        if (userGraphCount >= config.maxGraphsPerUser) {
            return GraphCreationResult.Failure("Maximum graphs per user limit reached")
        }

        val graph = FlowGraph(
            id = graphId,
            name = name,
            description = description,
            ownerId = ownerId,
            isPublic = isPublic,
            nodes = emptyList(),
            connections = emptyList(),
            createdAt = System.currentTimeMillis(),
            lastModifiedAt = System.currentTimeMillis(),
            version = 1
        )

        graphs[graphId] = graph
        userGraphs.getOrPut(ownerId) { mutableSetOf() }.add(graphId)
        graphCollaborators[graphId] = mutableSetOf(ownerId)


        createVersion(graphId, ownerId, "Initial version", graph)

        eventManager.emit(GraphEvent.GraphCreated(graphId, ownerId, System.currentTimeMillis()))

        GraphCreationResult.Success(graph)
    }


    fun getGraph(graphId: String): FlowGraph? = graphs[graphId]


    fun getUserGraphs(userId: String): List<FlowGraph> {
        val graphIds = userGraphs[userId] ?: return emptyList()
        return graphIds.mapNotNull { graphs[it] }
    }


    fun getAccessibleGraphs(userId: String): List<FlowGraph> {
        val ownedGraphs = getUserGraphs(userId)
        val sharedGraphs = graphCollaborators.entries
            .filter { it.value.contains(userId) }
            .mapNotNull { graphs[it.key] }
            .filter { it.ownerId != userId }

        return (ownedGraphs + sharedGraphs).distinctBy { it.id }
    }


    suspend fun updateGraph(
        graphId: String,
        userId: String,
        nodes: List<GraphNode>? = null,
        connections: List<GraphConnection>? = null,
        name: String? = null,
        description: String? = null,
        createVersion: Boolean = false
    ): GraphUpdateResult = graphMutex.withLock {

        val graph = graphs[graphId] ?: return GraphUpdateResult.Failure("Graph not found")

        if (!hasWriteAccess(graphId, userId)) {
            return GraphUpdateResult.Failure("Access denied")
        }

        val updatedGraph = graph.copy(
            nodes = nodes ?: graph.nodes,
            connections = connections ?: graph.connections,
            name = name ?: graph.name,
            description = description ?: graph.description,
            lastModifiedAt = System.currentTimeMillis(),
            lastModifiedBy = userId,
            version = if (createVersion) graph.version + 1 else graph.version
        )

        graphs[graphId] = updatedGraph

        if (createVersion) {
            createVersion(graphId, userId, "Auto-saved version", updatedGraph)
        }

        eventManager.emit(GraphEvent.GraphUpdated(graphId, userId, "data_update", System.currentTimeMillis()))

        GraphUpdateResult.Success(updatedGraph)
    }


    suspend fun addNode(
        graphId: String,
        userId: String,
        node: GraphNode
    ): NodeOperationResult = graphMutex.withLock {

        val graph = graphs[graphId] ?: return NodeOperationResult.Failure("Graph not found")

        if (!hasWriteAccess(graphId, userId)) {
            return NodeOperationResult.Failure("Access denied")
        }

        if (graph.nodes.any { it.id == node.id }) {
            return NodeOperationResult.Failure("Node with this ID already exists")
        }

        val updatedNodes = graph.nodes + node
        val updatedGraph = graph.copy(
            nodes = updatedNodes,
            lastModifiedAt = System.currentTimeMillis(),
            lastModifiedBy = userId
        )

        graphs[graphId] = updatedGraph

        eventManager.emit(GraphEvent.NodeAdded(graphId, node.id, userId, System.currentTimeMillis()))

        NodeOperationResult.Success(node)
    }


    suspend fun updateNode(
        graphId: String,
        userId: String,
        nodeId: String,
        updates: NodeUpdate
    ): NodeOperationResult = graphMutex.withLock {

        val graph = graphs[graphId] ?: return NodeOperationResult.Failure("Graph not found")

        if (!hasWriteAccess(graphId, userId)) {
            return NodeOperationResult.Failure("Access denied")
        }

        val nodeIndex = graph.nodes.indexOfFirst { it.id == nodeId }
        if (nodeIndex < 0) {
            return NodeOperationResult.Failure("Node not found")
        }

        val existingNode = graph.nodes[nodeIndex]
        val updatedNode = existingNode.copy(
            name = updates.name ?: existingNode.name,
            position = updates.position ?: existingNode.position,
            properties = updates.properties ?: existingNode.properties
        )

        val updatedNodes = graph.nodes.toMutableList()
        updatedNodes[nodeIndex] = updatedNode

        val updatedGraph = graph.copy(
            nodes = updatedNodes,
            lastModifiedAt = System.currentTimeMillis(),
            lastModifiedBy = userId
        )

        graphs[graphId] = updatedGraph

        eventManager.emit(GraphEvent.NodeUpdated(graphId, nodeId, userId, System.currentTimeMillis()))

        NodeOperationResult.Success(updatedNode)
    }

    /**
     * Delete a node from a graph
     */
    suspend fun deleteNode(
        graphId: String,
        userId: String,
        nodeId: String
    ): Boolean = graphMutex.withLock {

        val graph = graphs[graphId] ?: return false

        if (!hasWriteAccess(graphId, userId)) {
            return false
        }

        val updatedNodes = graph.nodes.filter { it.id != nodeId }
        val updatedConnections = graph.connections.filter {
            it.fromNodeId != nodeId && it.toNodeId != nodeId
        }

        val updatedGraph = graph.copy(
            nodes = updatedNodes,
            connections = updatedConnections,
            lastModifiedAt = System.currentTimeMillis(),
            lastModifiedBy = userId
        )

        graphs[graphId] = updatedGraph

        eventManager.emit(GraphEvent.NodeDeleted(graphId, nodeId, userId, System.currentTimeMillis()))

        true
    }

    /**
     * Add a connection between nodes
     */
    suspend fun addConnection(
        graphId: String,
        userId: String,
        connection: GraphConnection
    ): ConnectionOperationResult = graphMutex.withLock {

        val graph = graphs[graphId] ?: return ConnectionOperationResult.Failure("Graph not found")

        if (!hasWriteAccess(graphId, userId)) {
            return ConnectionOperationResult.Failure("Access denied")
        }

        if (graph.connections.any { it.id == connection.id }) {
            return ConnectionOperationResult.Failure("Connection with this ID already exists")
        }

        // Validate that nodes exist
        val fromNodeExists = graph.nodes.any { it.id == connection.fromNodeId }
        val toNodeExists = graph.nodes.any { it.id == connection.toNodeId }

        if (!fromNodeExists || !toNodeExists) {
            return ConnectionOperationResult.Failure("One or both nodes don't exist")
        }

        val updatedConnections = graph.connections + connection
        val updatedGraph = graph.copy(
            connections = updatedConnections,
            lastModifiedAt = System.currentTimeMillis(),
            lastModifiedBy = userId
        )

        graphs[graphId] = updatedGraph

        eventManager.emit(GraphEvent.ConnectionAdded(graphId, connection.id, userId, System.currentTimeMillis()))

        ConnectionOperationResult.Success(connection)
    }

    /**
     * Delete a connection
     */
    suspend fun deleteConnection(
        graphId: String,
        userId: String,
        connectionId: String
    ): Boolean = graphMutex.withLock {

        val graph = graphs[graphId] ?: return false

        if (!hasWriteAccess(graphId, userId)) {
            return false
        }

        val updatedConnections = graph.connections.filter { it.id != connectionId }
        val updatedGraph = graph.copy(
            connections = updatedConnections,
            lastModifiedAt = System.currentTimeMillis(),
            lastModifiedBy = userId
        )

        graphs[graphId] = updatedGraph

        eventManager.emit(GraphEvent.ConnectionDeleted(graphId, connectionId, userId, System.currentTimeMillis()))

        true
    }

    /**
     * Share a graph with another user
     */
    suspend fun shareGraph(
        graphId: String,
        ownerId: String,
        userId: String,
        permission: GraphPermission = GraphPermission.READ
    ): Boolean = graphMutex.withLock {

        val graph = graphs[graphId] ?: return false

        if (graph.ownerId != ownerId) {
            return false
        }

        graphCollaborators.getOrPut(graphId) { mutableSetOf() }.add(userId)

        eventManager.emit(GraphEvent.GraphShared(graphId, ownerId, userId, System.currentTimeMillis()))

        true
    }

    /**
     * Delete a graph
     */
    suspend fun deleteGraph(graphId: String, userId: String): Boolean = graphMutex.withLock {
        val graph = graphs[graphId] ?: return false

        if (graph.ownerId != userId) {
            return false
        }

        graphs.remove(graphId)
        userGraphs[userId]?.remove(graphId)
        graphCollaborators.remove(graphId)
        graphVersions.remove(graphId)

        eventManager.emit(GraphEvent.GraphDeleted(graphId, userId, System.currentTimeMillis()))

        true
    }

    /**
     * Create a version snapshot of a graph
     */
    private fun createVersion(graphId: String, userId: String, description: String, graph: FlowGraph) {
        val version = GraphVersion(
            versionNumber = graph.version,
            description = description,
            createdBy = userId,
            createdAt = System.currentTimeMillis(),
            graphSnapshot = graph
        )

        graphVersions.getOrPut(graphId) { mutableListOf() }.add(version)
    }

    /**
     * Get graph versions
     */
    fun getGraphVersions(graphId: String): List<GraphVersion> {
        return graphVersions[graphId] ?: emptyList()
    }

    /**
     * Restore a graph to a specific version
     */
    suspend fun restoreGraphVersion(
        graphId: String,
        userId: String,
        versionNumber: Int
    ): GraphUpdateResult = graphMutex.withLock {

        if (!hasWriteAccess(graphId, userId)) {
            return GraphUpdateResult.Failure("Access denied")
        }

        val versions = graphVersions[graphId] ?: return GraphUpdateResult.Failure("No versions found")
        val version = versions.find { it.versionNumber == versionNumber }
            ?: return GraphUpdateResult.Failure("Version not found")

        val restoredGraph = version.graphSnapshot.copy(
            lastModifiedAt = System.currentTimeMillis(),
            lastModifiedBy = userId,
            version = (graphs[graphId]?.version ?: 0) + 1
        )

        graphs[graphId] = restoredGraph

        // Create a new version for the restore
        createVersion(graphId, userId, "Restored from version $versionNumber", restoredGraph)

        eventManager.emit(GraphEvent.GraphUpdated(graphId, userId, "version_restore", System.currentTimeMillis()))

        GraphUpdateResult.Success(restoredGraph)
    }

    /**
     * Check if user has write access to graph
     */
    private fun hasWriteAccess(graphId: String, userId: String): Boolean {
        val graph = graphs[graphId] ?: return false
        return graph.ownerId == userId || graphCollaborators[graphId]?.contains(userId) == true
    }

    /**
     * Check if user has read access to graph
     */
    fun hasReadAccess(graphId: String, userId: String): Boolean {
        val graph = graphs[graphId] ?: return false
        return graph.ownerId == userId ||
               graph.isPublic ||
               graphCollaborators[graphId]?.contains(userId) == true
    }

    /**
     * Get graph statistics
     */
    fun getGraphStatistics(): GraphStatistics {
        return GraphStatistics(
            totalGraphs = graphs.size,
            publicGraphs = graphs.values.count { it.isPublic },
            privateGraphs = graphs.values.count { !it.isPublic },
            totalNodes = graphs.values.sumOf { it.nodes.size },
            totalConnections = graphs.values.sumOf { it.connections.size },
            totalVersions = graphVersions.values.sumOf { it.size }
        )
    }

    /**
     * Get total graph count
     */
    fun getTotalGraphCount(): Int = graphs.size

    /**
     * Search graphs
     */
    fun searchGraphs(
        query: String,
        userId: String? = null,
        includePublic: Boolean = true,
        limit: Int = 20
    ): List<FlowGraph> {
        return graphs.values
            .filter { graph ->
                val matchesQuery = graph.name.contains(query, ignoreCase = true) ||
                                   graph.description.contains(query, ignoreCase = true)
                val hasAccess = userId?.let { hasReadAccess(graph.id, it) } ?: includePublic && graph.isPublic
                matchesQuery && hasAccess
            }
            .take(limit)
    }

    /**
     * Dispose the graph manager
     */
    fun dispose() {
        graphs.clear()
        userGraphs.clear()
        graphCollaborators.clear()
        graphVersions.clear()
    }
}

/**
 * Flow graph data model
 */
data class FlowGraph(
    val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val isPublic: Boolean,
    val nodes: List<GraphNode>,
    val connections: List<GraphConnection>,
    val createdAt: Long,
    val lastModifiedAt: Long,
    val lastModifiedBy: String? = null,
    val version: Int,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Graph node
 */
data class GraphNode(
    val id: String,
    val name: String,
    val type: String = "default",
    val position: Position,
    val properties: Map<String, Any> = emptyMap(),
    val inputs: List<GraphPort> = emptyList(),
    val outputs: List<GraphPort> = emptyList()
)

/**
 * Graph connection
 */
data class GraphConnection(
    val id: String,
    val fromNodeId: String,
    val fromPortId: String,
    val toNodeId: String,
    val toPortId: String,
    val properties: Map<String, Any> = emptyMap()
)

/**
 * Graph port
 */
data class GraphPort(
    val id: String,
    val name: String,
    val type: String = "default",
    val isInput: Boolean
)

/**
 * Position
 */
data class Position(
    val x: Double,
    val y: Double
)

/**
 * Graph version
 */
data class GraphVersion(
    val versionNumber: Int,
    val description: String,
    val createdBy: String,
    val createdAt: Long,
    val graphSnapshot: FlowGraph
)

/**
 * Node update data
 */
data class NodeUpdate(
    val name: String? = null,
    val position: Position? = null,
    val properties: Map<String, Any>? = null
)

/**
 * Graph permissions
 */
enum class GraphPermission {
    READ,
    WRITE,
    ADMIN
}

// Result types

sealed class GraphCreationResult {
    data class Success(val graph: FlowGraph) : GraphCreationResult()
    data class Failure(val error: String) : GraphCreationResult()
}

sealed class GraphUpdateResult {
    data class Success(val graph: FlowGraph) : GraphUpdateResult()
    data class Failure(val error: String) : GraphUpdateResult()
}

sealed class NodeOperationResult {
    data class Success(val node: GraphNode) : NodeOperationResult()
    data class Failure(val error: String) : NodeOperationResult()
}

sealed class ConnectionOperationResult {
    data class Success(val connection: GraphConnection) : ConnectionOperationResult()
    data class Failure(val error: String) : ConnectionOperationResult()
}

/**
 * Graph statistics
 */
data class GraphStatistics(
    val totalGraphs: Int,
    val publicGraphs: Int,
    val privateGraphs: Int,
    val totalNodes: Int,
    val totalConnections: Int,
    val totalVersions: Int
)