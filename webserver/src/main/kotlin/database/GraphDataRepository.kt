package com.thedevjade.flow.webserver.database

import com.thedevjade.flow.common.models.FlowLogger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object GraphDataRepository {

    fun saveGraph(
        graphId: String,
        userId: Int,
        workspaceId: String,
        name: String,
        data: String,
        version: String = "1.0.0"
    ): Boolean =
        transaction(DatabaseManager.getDatabase()) {
            try {
                val existingGraph = GraphDataTable.select { GraphDataTable.graphId eq graphId }.singleOrNull()

                if (existingGraph != null) {
                    GraphDataTable.update({ GraphDataTable.graphId eq graphId }) {
                        it[GraphDataTable.name] = name
                        it[GraphDataTable.data] = data
                        it[GraphDataTable.version] = version
                        it[updatedAt] = Instant.now()
                    } > 0
                } else {
                    GraphDataTable.insert {
                        it[GraphDataTable.graphId] = graphId
                        it[GraphDataTable.userId] = userId
                        it[GraphDataTable.workspaceId] = workspaceId
                        it[GraphDataTable.name] = name
                        it[GraphDataTable.data] = data
                        it[GraphDataTable.version] = version
                        it[isActive] = true
                        it[createdAt] = Instant.now()
                        it[updatedAt] = Instant.now()
                    }.insertedCount > 0
                }
            } catch (e: Exception) {
                FlowLogger.debug("Error saving graph $graphId: ${e.message}")
                false
            }
        }

    fun loadGraph(graphId: String): GraphData? = transaction(DatabaseManager.getDatabase()) {
        try {
            GraphDataTable.select { (GraphDataTable.graphId eq graphId) and (GraphDataTable.isActive eq true) }
                .singleOrNull()?.let { row ->
                    GraphData(
                        graphId = row[GraphDataTable.graphId],
                        userId = row[GraphDataTable.userId].value,
                        workspaceId = row[GraphDataTable.workspaceId],
                        name = row[GraphDataTable.name],
                        data = row[GraphDataTable.data],
                        version = row[GraphDataTable.version],
                        isActive = row[GraphDataTable.isActive],
                        createdAt = row[GraphDataTable.createdAt],
                        updatedAt = row[GraphDataTable.updatedAt]
                    )
                }
        } catch (e: Exception) {
            FlowLogger.debug("Error loading graph $graphId: ${e.message}")
            null
        }
    }

    fun loadGraphsByWorkspace(workspaceId: String): List<GraphData> = transaction(DatabaseManager.getDatabase()) {
        try {
            GraphDataTable.select { (GraphDataTable.workspaceId eq workspaceId) and (GraphDataTable.isActive eq true) }
                .orderBy(GraphDataTable.updatedAt, SortOrder.DESC)
                .map { row ->
                    GraphData(
                        graphId = row[GraphDataTable.graphId],
                        userId = row[GraphDataTable.userId].value,
                        workspaceId = row[GraphDataTable.workspaceId],
                        name = row[GraphDataTable.name],
                        data = row[GraphDataTable.data],
                        version = row[GraphDataTable.version],
                        isActive = row[GraphDataTable.isActive],
                        createdAt = row[GraphDataTable.createdAt],
                        updatedAt = row[GraphDataTable.updatedAt]
                    )
                }
        } catch (e: Exception) {
            FlowLogger.debug("Error loading graphs for workspace $workspaceId: ${e.message}")
            emptyList()
        }
    }

    fun loadGraphsByUser(userId: Int): List<GraphData> = transaction(DatabaseManager.getDatabase()) {
        try {
            GraphDataTable.select { (GraphDataTable.userId eq userId) and (GraphDataTable.isActive eq true) }
                .orderBy(GraphDataTable.updatedAt, SortOrder.DESC)
                .map { row ->
                    GraphData(
                        graphId = row[GraphDataTable.graphId],
                        userId = row[GraphDataTable.userId].value,
                        workspaceId = row[GraphDataTable.workspaceId],
                        name = row[GraphDataTable.name],
                        data = row[GraphDataTable.data],
                        version = row[GraphDataTable.version],
                        isActive = row[GraphDataTable.isActive],
                        createdAt = row[GraphDataTable.createdAt],
                        updatedAt = row[GraphDataTable.updatedAt]
                    )
                }
        } catch (e: Exception) {
            FlowLogger.debug("Error loading graphs for user $userId: ${e.message}")
            emptyList()
        }
    }

    fun deleteGraph(graphId: String): Boolean = transaction(DatabaseManager.getDatabase()) {
        try {

            GraphDataTable.update({ GraphDataTable.graphId eq graphId }) {
                it[isActive] = false
                it[updatedAt] = Instant.now()
            } > 0
        } catch (e: Exception) {
            FlowLogger.debug("Error deleting graph $graphId: ${e.message}")
            false
        }
    }

    fun hardDeleteGraph(graphId: String): Boolean = transaction(DatabaseManager.getDatabase()) {
        try {
            GraphDataTable.deleteWhere { GraphDataTable.graphId eq graphId } > 0
        } catch (e: Exception) {
            FlowLogger.debug("Error hard deleting graph $graphId: ${e.message}")
            false
        }
    }

    fun updateGraphName(graphId: String, name: String): Boolean = transaction(DatabaseManager.getDatabase()) {
        try {
            GraphDataTable.update({ GraphDataTable.graphId eq graphId }) {
                it[GraphDataTable.name] = name
                it[updatedAt] = Instant.now()
            } > 0
        } catch (e: Exception) {
            FlowLogger.debug("Error updating graph name $graphId: ${e.message}")
            false
        }
    }

    fun getGraphMetadata(graphId: String): GraphMetadata? = transaction(DatabaseManager.getDatabase()) {
        try {
            GraphDataTable.select { GraphDataTable.graphId eq graphId }
                .singleOrNull()?.let { row ->
                    GraphMetadata(
                        graphId = row[GraphDataTable.graphId],
                        name = row[GraphDataTable.name],
                        workspaceId = row[GraphDataTable.workspaceId],
                        version = row[GraphDataTable.version],
                        isActive = row[GraphDataTable.isActive],
                        createdAt = row[GraphDataTable.createdAt],
                        updatedAt = row[GraphDataTable.updatedAt]
                    )
                }
        } catch (e: Exception) {
            FlowLogger.debug("Error getting graph metadata $graphId: ${e.message}")
            null
        }
    }
}

data class GraphData(
    val graphId: String,
    val userId: Int,
    val workspaceId: String,
    val name: String,
    val data: String,
    val version: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class GraphMetadata(
    val graphId: String,
    val name: String,
    val workspaceId: String,
    val version: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)