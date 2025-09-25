package com.thedevjade.flow.webserver.websocket

import com.thedevjade.flow.common.models.*
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class GraphDataManager {
    private val graphs = ConcurrentHashMap<String, GraphData>()
    private val workspaces = ConcurrentHashMap<String, WorkspaceData>()
    private val baseDataDir = "data"
    private val graphsDir = "$baseDataDir/graphs"
    private val workspacesDir = "$baseDataDir/workspaces"
    private val filesDir = "$baseDataDir/files"

    init {

        File(graphsDir).mkdirs()
        File(workspacesDir).mkdirs()
        File(filesDir).mkdirs()
        FlowLogger.debug("GraphDataManager initialized with directories: graphs=$graphsDir, workspaces=$workspacesDir, files=$filesDir")
    }

    fun saveGraph(graphId: String, graphData: GraphData): Boolean {
        return try {
            graphs[graphId] = graphData


            val file = File("$graphsDir/$graphId.json")
            val jsonString = Json.encodeToString(GraphData.serializer(), graphData)
            file.writeText(jsonString)

            FlowLogger.debug("Saved graph: $graphId")
            true
        } catch (e: Exception) {
            FlowLogger.debug("Failed to save graph $graphId: ${e.message}")
            false
        }
    }

    fun loadGraph(graphId: String): GraphData? {
        return try {

            graphs[graphId]?.let { return it }


            val file = File("$graphsDir/$graphId.json")
            if (file.exists()) {
                val jsonString = file.readText()
                val graphData = Json.decodeFromString(GraphData.serializer(), jsonString)
                graphs[graphId] = graphData
                graphData
            } else {
                null
            }
        } catch (e: Exception) {
            FlowLogger.debug("Failed to load graph $graphId: ${e.message}")
            null
        }
    }

    fun deleteGraph(graphId: String): Boolean {
        return try {
            graphs.remove(graphId)
            val file = File("$graphsDir/$graphId.json")
            if (file.exists()) {
                file.delete()
            }
            true
        } catch (e: Exception) {
            FlowLogger.debug("Failed to delete graph $graphId: ${e.message}")
            false
        }
    }

    fun listGraphs(): List<String> {
        val fileGraphs = File(graphsDir).listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()

        return (graphs.keys + fileGraphs).distinct()
    }

    fun saveFile(path: String, content: String): Boolean {
        return try {
            val file = File(filesDir, path)
            file.parentFile.mkdirs()
            file.writeText(content)
            FlowLogger.debug("Saved file: $path")
            true
        } catch (e: Exception) {
            FlowLogger.debug("Failed to save file $path: ${e.message}")
            false
        }
    }

    fun loadFile(path: String): String? {
        return try {
            val file = File(filesDir, path)
            if (file.exists() && file.isFile) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            FlowLogger.debug("Failed to load file $path: ${e.message}")
            null
        }
    }

    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(filesDir, path)
            if (file.exists()) {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
            true
        } catch (e: Exception) {
            FlowLogger.debug("Failed to delete file $path: ${e.message}")
            false
        }
    }

    fun createFile(path: String, content: String = ""): Boolean {
        return try {
            val file = File(filesDir, path)
            file.parentFile.mkdirs()
            if (!file.exists()) {
                file.writeText(content)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            FlowLogger.debug("Failed to create file $path: ${e.message}")
            false
        }
    }

    fun renameFile(oldPath: String, newPath: String): Boolean {
        return try {
            val oldFile = File(filesDir, oldPath)
            val newFile = File(filesDir, newPath)

            if (oldFile.exists()) {
                newFile.parentFile.mkdirs()
                oldFile.renameTo(newFile)
            } else {
                false
            }
        } catch (e: Exception) {
            FlowLogger.debug("Failed to rename file $oldPath to $newPath: ${e.message}")
            false
        }
    }

    fun updateNode(graphId: String, nodeId: String, nodeData: NodeData): Boolean {
        return try {
            val graph = loadGraph(graphId) ?: return false
            val updatedNodes = graph.nodes.map {
                if (it.id == nodeId) nodeData else it
            }
            val updatedGraph = graph.copy(nodes = updatedNodes as List<GraphNode>)
            saveGraph(graphId, updatedGraph)
        } catch (e: Exception) {
            FlowLogger.debug("Failed to update node $nodeId in graph $graphId: ${e.message}")
            false
        }
    }

    fun updateNodePosition(graphId: String, nodeId: String, position: Position): Boolean {
        return try {
            val graph = loadGraph(graphId) ?: return false
            val updatedNodes = graph.nodes.map {
                if (it.id == nodeId) it.copy(position = position) else it
            }
            val updatedGraph = graph.copy(nodes = updatedNodes)
            saveGraph(graphId, updatedGraph)
        } catch (e: Exception) {
            FlowLogger.debug("Failed to update node position for $nodeId in graph $graphId: ${e.message}")
            false
        }
    }

    fun getFileTree(): FileTreeNode? {
        return try {
            val rootDir = File(filesDir)
            if (rootDir.exists()) {
                buildFileTree(rootDir, filesDir)
            } else {
                null
            }
        } catch (e: Exception) {
            FlowLogger.debug("Failed to build file tree: ${e.message}")
            null
        }
    }

    private fun buildFileTree(file: File, basePath: String): FileTreeNode {
        val relativePath = file.absolutePath.removePrefix(File(basePath).absolutePath).removePrefix("/")

        return if (file.isDirectory) {
            val children = file.listFiles()
                ?.map { buildFileTree(it, basePath) }
                ?.sortedWith(compareBy({ it.type != "directory" }, { it.name }))
                ?: emptyList()

            FileTreeNode(
                name = file.name.ifEmpty { "root" },
                type = "directory",
                path = relativePath,
                children = children,
                lastModified = file.lastModified()
            )
        } else {
            FileTreeNode(
                name = file.name,
                type = "file",
                path = relativePath,
                size = file.length(),
                lastModified = file.lastModified()
            )
        }
    }


    fun saveWorkspace(workspaceId: String, workspaceData: WorkspaceData): Boolean {
        return try {
            workspaces[workspaceId] = workspaceData


            val file = File("$workspacesDir/$workspaceId.json")
            val jsonString = Json.encodeToString(WorkspaceData.serializer(), workspaceData)
            file.writeText(jsonString)

            FlowLogger.debug("Saved workspace: $workspaceId")
            true
        } catch (e: Exception) {
            FlowLogger.debug("Failed to save workspace $workspaceId: ${e.message}")
            false
        }
    }

    fun loadWorkspace(workspaceId: String): WorkspaceData? {
        return try {

            workspaces[workspaceId]?.let { return it }


            val file = File("$workspacesDir/$workspaceId.json")
            if (file.exists()) {
                val jsonString = file.readText()
                val workspaceData = Json.decodeFromString(WorkspaceData.serializer(), jsonString)
                workspaces[workspaceId] = workspaceData
                workspaceData
            } else {
                null
            }
        } catch (e: Exception) {
            FlowLogger.debug("Failed to load workspace $workspaceId: ${e.message}")
            null
        }
    }

    fun deleteWorkspace(workspaceId: String): Boolean {
        return try {
            workspaces.remove(workspaceId)
            val file = File("$workspacesDir/$workspaceId.json")
            if (file.exists()) {
                file.delete()
            }
            FlowLogger.debug("Deleted workspace: $workspaceId")
            true
        } catch (e: Exception) {
            FlowLogger.debug("Failed to delete workspace $workspaceId: ${e.message}")
            false
        }
    }

    fun listWorkspaces(): List<String> {
        val fileWorkspaces = File(workspacesDir).listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()

        return (workspaces.keys + fileWorkspaces).distinct()
    }

    fun updateWorkspacePage(workspaceId: String, pageData: PageData): Boolean {
        return try {
            val workspace = loadWorkspace(workspaceId) ?: return false
            val updatedPages = workspace.pages.toMutableList()
            val existingPageIndex = updatedPages.indexOfFirst { it.id == pageData.id }

            if (existingPageIndex >= 0) {
                updatedPages[existingPageIndex] = pageData
            } else {
                updatedPages.add(pageData)
            }

            val updatedWorkspace = workspace.copy(
                pages = updatedPages,
                lastModified = java.time.Instant.now().toString()
            )

            saveWorkspace(workspaceId, updatedWorkspace)
        } catch (e: Exception) {
            FlowLogger.debug("Failed to update workspace page: ${e.message}")
            false
        }
    }
}