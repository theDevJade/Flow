package com.thedevjade.flow.webserver.database

import com.thedevjade.flow.common.models.FlowLogger
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object WorkspaceRepository {

    fun saveWorkspace(workspaceId: String, userId: Int, name: String, data: String, currentPage: String?, settings: String = "{}"): Boolean =
        transaction(DatabaseManager.getDatabase()) {
            try {
                val existingWorkspace = WorkspaceDataTable.select { WorkspaceDataTable.workspaceId eq workspaceId }.singleOrNull()

                if (existingWorkspace != null) {
                    WorkspaceDataTable.update({ WorkspaceDataTable.workspaceId eq workspaceId }) {
                        it[WorkspaceDataTable.name] = name
                        it[WorkspaceDataTable.data] = data
                        it[WorkspaceDataTable.currentPage] = currentPage
                        it[WorkspaceDataTable.settings] = settings
                        it[updatedAt] = Instant.now()
                    } > 0
                } else {
                    WorkspaceDataTable.insert {
                        it[WorkspaceDataTable.workspaceId] = workspaceId
                        it[WorkspaceDataTable.userId] = userId
                        it[WorkspaceDataTable.name] = name
                        it[WorkspaceDataTable.data] = data
                        it[WorkspaceDataTable.currentPage] = currentPage
                        it[WorkspaceDataTable.settings] = settings
                        it[createdAt] = Instant.now()
                        it[updatedAt] = Instant.now()
                    }.insertedCount > 0
                }
            } catch (e: Exception) {
                FlowLogger.debug("Error saving workspace $workspaceId: ${e.message}")
                false
            }
        }

    fun loadWorkspace(workspaceId: String): WorkspaceData? = transaction(DatabaseManager.getDatabase()) {
        try {
            WorkspaceDataTable.select { WorkspaceDataTable.workspaceId eq workspaceId }
                .singleOrNull()?.let { row ->
                    WorkspaceData(
                        workspaceId = row[WorkspaceDataTable.workspaceId],
                        userId = row[WorkspaceDataTable.userId].value,
                        name = row[WorkspaceDataTable.name],
                        data = row[WorkspaceDataTable.data],
                        currentPage = row[WorkspaceDataTable.currentPage],
                        settings = row[WorkspaceDataTable.settings],
                        createdAt = row[WorkspaceDataTable.createdAt],
                        updatedAt = row[WorkspaceDataTable.updatedAt]
                    )
                }
        } catch (e: Exception) {
            FlowLogger.debug("Error loading workspace $workspaceId: ${e.message}")
            null
        }
    }

    fun loadWorkspacesByUser(userId: Int): List<WorkspaceData> = transaction(DatabaseManager.getDatabase()) {
        try {
            WorkspaceDataTable.select { WorkspaceDataTable.userId eq userId }
                .orderBy(WorkspaceDataTable.updatedAt, SortOrder.DESC)
                .map { row ->
                    WorkspaceData(
                        workspaceId = row[WorkspaceDataTable.workspaceId],
                        userId = row[WorkspaceDataTable.userId].value,
                        name = row[WorkspaceDataTable.name],
                        data = row[WorkspaceDataTable.data],
                        currentPage = row[WorkspaceDataTable.currentPage],
                        settings = row[WorkspaceDataTable.settings],
                        createdAt = row[WorkspaceDataTable.createdAt],
                        updatedAt = row[WorkspaceDataTable.updatedAt]
                    )
                }
        } catch (e: Exception) {
            FlowLogger.debug("Error loading workspaces for user $userId: ${e.message}")
            emptyList()
        }
    }

    fun deleteWorkspace(workspaceId: String): Boolean = transaction(DatabaseManager.getDatabase()) {
        try {

            GraphDataRepository.deleteGraphsByWorkspace(workspaceId)

            WorkspaceDataTable.deleteWhere { WorkspaceDataTable.workspaceId eq workspaceId } > 0
        } catch (e: Exception) {
            FlowLogger.debug("Error deleting workspace $workspaceId: ${e.message}")
            false
        }
    }

    fun updateWorkspaceName(workspaceId: String, name: String): Boolean = transaction(DatabaseManager.getDatabase()) {
        try {
            WorkspaceDataTable.update({ WorkspaceDataTable.workspaceId eq workspaceId }) {
                it[WorkspaceDataTable.name] = name
                it[updatedAt] = Instant.now()
            } > 0
        } catch (e: Exception) {
            FlowLogger.debug("Error updating workspace name $workspaceId: ${e.message}")
            false
        }
    }

    fun updateCurrentPage(workspaceId: String, currentPage: String): Boolean = transaction(DatabaseManager.getDatabase()) {
        try {
            WorkspaceDataTable.update({ WorkspaceDataTable.workspaceId eq workspaceId }) {
                it[WorkspaceDataTable.currentPage] = currentPage
                it[updatedAt] = Instant.now()
            } > 0
        } catch (e: Exception) {
            FlowLogger.debug("Error updating current page for workspace $workspaceId: ${e.message}")
            false
        }
    }

    fun updateSettings(workspaceId: String, settings: String): Boolean = transaction(DatabaseManager.getDatabase()) {
        try {
            WorkspaceDataTable.update({ WorkspaceDataTable.workspaceId eq workspaceId }) {
                it[WorkspaceDataTable.settings] = settings
                it[updatedAt] = Instant.now()
            } > 0
        } catch (e: Exception) {
            FlowLogger.debug("Error updating settings for workspace $workspaceId: ${e.message}")
            false
        }
    }

    fun getWorkspaceMetadata(workspaceId: String): WorkspaceMetadata? = transaction(DatabaseManager.getDatabase()) {
        try {
            WorkspaceDataTable.select { WorkspaceDataTable.workspaceId eq workspaceId }
                .singleOrNull()?.let { row ->
                    WorkspaceMetadata(
                        workspaceId = row[WorkspaceDataTable.workspaceId],
                        name = row[WorkspaceDataTable.name],
                        currentPage = row[WorkspaceDataTable.currentPage],
                        createdAt = row[WorkspaceDataTable.createdAt],
                        updatedAt = row[WorkspaceDataTable.updatedAt]
                    )
                }
        } catch (e: Exception) {
            FlowLogger.debug("Error getting workspace metadata $workspaceId: ${e.message}")
            null
        }
    }
}


fun GraphDataRepository.deleteGraphsByWorkspace(workspaceId: String): Boolean = transaction(DatabaseManager.getDatabase()) {
    try {
        GraphDataTable.update({ GraphDataTable.workspaceId eq workspaceId }) {
            it[isActive] = false
            it[updatedAt] = Instant.now()
        } >= 0
    } catch (e: Exception) {
        FlowLogger.debug("Error deleting graphs for workspace $workspaceId: ${e.message}")
        false
    }
}

data class WorkspaceData(
    val workspaceId: String,
    val userId: Int,
    val name: String,
    val data: String,
    val currentPage: String?,
    val settings: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class WorkspaceMetadata(
    val workspaceId: String,
    val name: String,
    val currentPage: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)