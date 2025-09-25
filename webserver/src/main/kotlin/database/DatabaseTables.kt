package com.thedevjade.flow.webserver.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object UsersTable : IntIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

object AuthTokensTable : IntIdTable("auth_tokens") {
    val token = varchar("token", 255).uniqueIndex()
    val userId = reference("user_id", UsersTable)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").default(Instant.now())
}

object GraphDataTable : IntIdTable("graph_data") {
    val graphId = varchar("graph_id", 255).uniqueIndex()
    val userId = reference("user_id", UsersTable)
    val workspaceId = varchar("workspace_id", 255)
    val name = varchar("name", 255)
    val data = text("data")
    val version = varchar("version", 50).default("1.0.0")
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

object WorkspaceDataTable : IntIdTable("workspace_data") {
    val workspaceId = varchar("workspace_id", 255).uniqueIndex()
    val userId = reference("user_id", UsersTable)
    val name = varchar("name", 255)
    val data = text("data")
    val currentPage = varchar("current_page", 255).nullable()
    val settings = text("settings").default("{}")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

object NodeTemplatesTable : IntIdTable("node_templates") {
    val templateId = varchar("template_id", 255).uniqueIndex()
    val name = varchar("name", 255)
    val category = varchar("category", 100)
    val description = text("description").nullable()
    val templateData = text("template_data")
    val isDefault = bool("is_default").default(false)
    val version = varchar("version", 50).default("1.0.0")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

object FileSystemTable : IntIdTable("file_system") {
    val path = varchar("path", 500).uniqueIndex()
    val name = varchar("name", 255)
    val isDirectory = bool("is_directory").default(false)
    val parentPath = varchar("parent_path", 500).nullable()
    val size = long("size").default(0)
    val lastModified = timestamp("last_modified").default(Instant.now())
    val permissions = varchar("permissions", 50).default("rw-r--r--")
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
}

object SessionDataTable : IntIdTable("session_data") {
    val sessionId = varchar("session_id", 255).uniqueIndex()
    val userId = reference("user_id", UsersTable)
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val isActive = bool("is_active").default(true)
    val data = text("data").default("{}")
    val createdAt = timestamp("created_at").default(Instant.now())
    val lastActivity = timestamp("last_activity").default(Instant.now())
}