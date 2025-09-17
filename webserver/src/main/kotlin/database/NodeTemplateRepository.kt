package com.thedevjade.flow.webserver.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object NodeTemplateRepository {
    
    fun initializeDefaultTemplates() {
        if (getTemplateCount() == 0L) {
            val defaultTemplates = getDefaultNodeTemplates()
            defaultTemplates.forEach { template ->
                insertTemplate(template)
            }
        }
    }
    
    private fun getTemplateCount(): Long = transaction(DatabaseManager.getDatabase()) {
        NodeTemplatesTable.selectAll().count()
    }
    
    fun insertTemplate(template: NodeTemplate) = transaction(DatabaseManager.getDatabase()) {
        NodeTemplatesTable.insert {
            it[templateId] = template.templateId
            it[name] = template.name
            it[category] = template.category
            it[description] = template.description
            it[templateData] = template.templateData
            it[isDefault] = template.isDefault
            it[version] = template.version
            it[createdAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
    }
    
    fun getAllTemplates(): List<NodeTemplate> = transaction(DatabaseManager.getDatabase()) {
        NodeTemplatesTable.selectAll().map { row ->
            NodeTemplate(
                templateId = row[NodeTemplatesTable.templateId],
                name = row[NodeTemplatesTable.name],
                category = row[NodeTemplatesTable.category],
                description = row[NodeTemplatesTable.description],
                templateData = row[NodeTemplatesTable.templateData],
                isDefault = row[NodeTemplatesTable.isDefault],
                version = row[NodeTemplatesTable.version]
            )
        }
    }
    
    fun getTemplatesByCategory(category: String): List<NodeTemplate> = transaction(DatabaseManager.getDatabase()) {
        NodeTemplatesTable.select { NodeTemplatesTable.category eq category }.map { row ->
            NodeTemplate(
                templateId = row[NodeTemplatesTable.templateId],
                name = row[NodeTemplatesTable.name],
                category = row[NodeTemplatesTable.category],
                description = row[NodeTemplatesTable.description],
                templateData = row[NodeTemplatesTable.templateData],
                isDefault = row[NodeTemplatesTable.isDefault],
                version = row[NodeTemplatesTable.version]
            )
        }
    }
    
    fun updateTemplate(templateId: String, template: NodeTemplate) = transaction(DatabaseManager.getDatabase()) {
        NodeTemplatesTable.update({ NodeTemplatesTable.templateId eq templateId }) {
            it[name] = template.name
            it[category] = template.category
            it[description] = template.description
            it[templateData] = template.templateData
            it[version] = template.version
            it[updatedAt] = Instant.now()
        }
    }
    
    fun deleteTemplate(templateId: String) = transaction(DatabaseManager.getDatabase()) {
        NodeTemplatesTable.deleteWhere { NodeTemplatesTable.templateId eq templateId }
    }
    
    private fun getDefaultNodeTemplates(): List<NodeTemplate> {
        return listOf(
            NodeTemplate(
                templateId = "input_material",
                name = "Material Input",
                category = "Input",
                description = "Material input node for shader graphs",
                templateData = buildJsonObject {
                    put("color", buildJsonObject { put("r", 0.2); put("g", 0.6); put("b", 0.2); put("a", 1.0) })
                    put("size", buildJsonObject { put("width", 150); put("height", 80) })
                    put("inputs", JsonArray(emptyList()))
                    put("outputs", JsonArray(listOf(
                        buildJsonObject {
                            put("id", "material")
                            put("name", "Material")
                            put("type", "material")
                            put("color", buildJsonObject { put("r", 1.0); put("g", 0.8); put("b", 0.0); put("a", 1.0) })
                        }
                    )))
                    put("properties", JsonArray(listOf(
                        buildJsonObject {
                            put("name", "material_name")
                            put("type", "string")
                            put("label", "Material Name")
                            put("default", "DefaultMaterial")
                            put("description", "Name of the material to load")
                        }
                    )))
                }.toString(),
                isDefault = true,
                version = "1.0.0"
            ),
            NodeTemplate(
                templateId = "math_add",
                name = "Add",
                category = "Math",
                description = "Add two numeric values together",
                templateData = buildJsonObject {
                    put("color", buildJsonObject { put("r", 0.1); put("g", 0.4); put("b", 0.8); put("a", 1.0) })
                    put("size", buildJsonObject { put("width", 120); put("height", 60) })
                    put("inputs", JsonArray(listOf(
                        buildJsonObject {
                            put("id", "a")
                            put("name", "A")
                            put("type", "float")
                            put("color", buildJsonObject { put("r", 0.8); put("g", 0.8); put("b", 0.8); put("a", 1.0) })
                        },
                        buildJsonObject {
                            put("id", "b")
                            put("name", "B")
                            put("type", "float")
                            put("color", buildJsonObject { put("r", 0.8); put("g", 0.8); put("b", 0.8); put("a", 1.0) })
                        }
                    )))
                    put("outputs", JsonArray(listOf(
                        buildJsonObject {
                            put("id", "result")
                            put("name", "Result")
                            put("type", "float")
                            put("color", buildJsonObject { put("r", 0.8); put("g", 0.8); put("b", 0.8); put("a", 1.0) })
                        }
                    )))
                    put("properties", JsonArray(emptyList()))
                }.toString(),
                isDefault = true,
                version = "1.0.0"
            ),
            NodeTemplate(
                templateId = "output_final",
                name = "Final Output",
                category = "Output",
                description = "Final output node for the graph",
                templateData = buildJsonObject {
                    put("color", buildJsonObject { put("r", 0.8); put("g", 0.2); put("b", 0.2); put("a", 1.0) })
                    put("size", buildJsonObject { put("width", 150); put("height", 80) })
                    put("inputs", JsonArray(listOf(
                        buildJsonObject {
                            put("id", "input")
                            put("name", "Input")
                            put("type", "any")
                            put("color", buildJsonObject { put("r", 1.0); put("g", 1.0); put("b", 1.0); put("a", 1.0) })
                        }
                    )))
                    put("outputs", JsonArray(emptyList()))
                    put("properties", JsonArray(emptyList()))
                }.toString(),
                isDefault = true,
                version = "1.0.0"
            )
        )
    }
}

data class NodeTemplate(
    val templateId: String,
    val name: String,
    val category: String,
    val description: String?,
    val templateData: String,
    val isDefault: Boolean = false,
    val version: String = "1.0.0"
)