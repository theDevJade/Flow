package com.thedevjade.flow.webserver.database

import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.common.config.FlowConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import javax.sql.DataSource
import org.sqlite.SQLiteDataSource

object DatabaseManager {
    private var database: Database? = null
    private var dataSource: DataSource? = null

    fun initialize() {
        val dbPath = FlowConfiguration.databaseConfig.databasePath ?: "./data/flow.db"
        val dbFile = File(dbPath)


        dbFile.parentFile?.mkdirs()

        FlowLogger.debug("Initializing SQLite database at: $dbPath")


        val sqliteDataSource = SQLiteDataSource().apply {
            url = "jdbc:sqlite:$dbPath"

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


        createTables()
        initializeDefaultData()

        FlowLogger.debug("SQLite database initialized successfully")
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