package com.thedevjade.flow.webserver.database

import config.FlowConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection
import javax.sql.DataSource
import org.sqlite.SQLiteDataSource

object DatabaseManager {
    private var database: Database? = null
    private var dataSource: DataSource? = null
    
    fun initialize() {
        val dbPath = FlowConfiguration.databaseConfig.databasePath ?: "./data/flow.db"
        val dbFile = File(dbPath)
        
        // Create directory if it doesn't exist
        dbFile.parentFile?.mkdirs()
        
        println("Initializing SQLite database at: $dbPath")
        
        // Configure SQLite data source
        val sqliteDataSource = SQLiteDataSource().apply {
            url = "jdbc:sqlite:$dbPath"
            // Enable WAL mode for better concurrent access
            if (FlowConfiguration.databaseConfig.enableWAL == true) {
                config.apply {
                    setJournalMode("WAL")
                    setSynchronous("NORMAL")
                    setTempStore("MEMORY")
                }
            }
        }
        
        dataSource = sqliteDataSource
        database = Database.connect(sqliteDataSource)
        
        // Create all tables and initialize default data
        createTables()
        initializeDefaultData()
        
        println("SQLite database initialized successfully")
    }
    
    private fun createTables() {
        transaction(database) {
            SchemaUtils.create(
                UsersTable,
                AuthTokensTable,
                GraphDataTable,
                WorkspaceDataTable,
                NodeTemplatesTable,
                FileSystemTable,
                SessionDataTable
            )
        }
    }
    
    private fun initializeDefaultData() {
        transaction(database) {
            // Initialize default node templates if they don't exist
            NodeTemplateRepository.initializeDefaultTemplates()
        }
    }
    
    fun getDatabase(): Database {
        return database ?: throw IllegalStateException("Database not initialized. Call initialize() first.")
    }
    
    fun getDataSource(): DataSource {
        return dataSource ?: throw IllegalStateException("Database not initialized. Call initialize() first.")
    }
    
    fun close() {
        database = null
        dataSource = null
    }
}