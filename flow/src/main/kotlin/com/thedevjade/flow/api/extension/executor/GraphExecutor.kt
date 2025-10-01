package com.thedevjade.flow.extension.executor

import com.thedevjade.flow.api.graph.FlowGraph
import com.thedevjade.flow.api.graph.GraphNode
import com.thedevjade.flow.extension.api.ActionResult
import com.thedevjade.flow.extension.api.TriggerResult
import com.thedevjade.flow.extension.registry.ActionNodeHandler
import com.thedevjade.flow.extension.registry.SimpleExtensionRegistry
import com.thedevjade.flow.extension.registry.TriggerNodeHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


class GraphExecutor(
    private val extensionRegistry: SimpleExtensionRegistry
) {
    private val activeExecutions = ConcurrentHashMap<String, GraphExecution>()
    private val executionIdGenerator = AtomicLong(0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    suspend fun executeGraph(
        graph: FlowGraph,
        inputs: Map<String, Any?> = emptyMap(),
        context: GraphExecutionContext? = null
    ): GraphExecutionResult {
        val executionId = generateExecutionId()
        val executionContext = context ?: createDefaultExecutionContext(executionId)

        // @todo setup debug logging for this
        executionContext.logger.info("Starting graph execution (ID: $executionId)")
        executionContext.logger.info("Graph: '${graph.name}' (${graph.id})")
        executionContext.logger.info("Nodes: ${graph.nodes.size}, Connections: ${graph.connections.size}")

        val execution = GraphExecution(
            id = executionId,
            graph = graph,
            context = executionContext,
            status = ExecutionStatus.RUNNING,
            startTime = System.currentTimeMillis(),
            currentNodeId = null,
            completedNodes = mutableSetOf(),
            failedNodes = mutableSetOf(),
            nodeResults = ConcurrentHashMap(),
            executionLog = mutableListOf()
        )

        activeExecutions[executionId] = execution

        return try {
            val result = executeGraphInternal(execution, inputs)
            execution.status = ExecutionStatus.COMPLETED
            execution.endTime = System.currentTimeMillis()

            val totalDuration = execution.endTime?.let { it - execution.startTime } ?: 0L
            executionContext.logger.info("Graph execution completed (ID: $executionId) in ${totalDuration}ms")

            result
        } catch (e: Exception) {
            execution.status = ExecutionStatus.FAILED
            execution.endTime = System.currentTimeMillis()
            execution.executionLog.add("Execution failed: ${e.message}")

            val totalDuration = execution.endTime?.let { it - execution.startTime } ?: 0L
            executionContext.logger.error(
                "Graph execution failed (ID: $executionId) after ${totalDuration}ms: ${e.message}",
                e
            )

            GraphExecutionResult.Failure(executionId, e.message ?: "Unknown error", e)
        } finally {
            activeExecutions.remove(executionId)
            executionContext.logger.info("Cleaned up execution (ID: $executionId)")
        }
    }


    suspend fun executeNode(
        graph: FlowGraph,
        nodeId: String,
        inputs: Map<String, Any?> = emptyMap(),
        context: GraphExecutionContext? = null
    ): NodeExecutionResult {
        val node = graph.nodes.find { it.id == nodeId }
            ?: return NodeExecutionResult.Failure("Node not found: $nodeId")

        val executionId = generateExecutionId()
        val executionContext = context ?: createDefaultExecutionContext(executionId)

        return try {
            executeNodeInternal(node, inputs, executionContext)
        } catch (e: Exception) {
            NodeExecutionResult.Failure("Node execution failed: ${e.message}")
        }
    }


    fun getExecutionStatus(executionId: String): GraphExecution? = activeExecutions[executionId]


    fun cancelExecution(executionId: String): Boolean {
        val execution = activeExecutions[executionId] ?: return false
        execution.status = ExecutionStatus.CANCELLED
        execution.endTime = System.currentTimeMillis()
        return true
    }


    fun getActiveExecutions(): Map<String, GraphExecution> = activeExecutions.toMap()

    private suspend fun executeGraphInternal(
        execution: GraphExecution,
        inputs: Map<String, Any?>
    ): GraphExecutionResult {
        val graph = execution.graph
        val context = execution.context

        execution.executionLog.add("Starting graph execution: '${graph.name}' (ID: ${graph.id})")
        execution.executionLog.add("Graph has ${graph.nodes.size} nodes and ${graph.connections.size} connections")
        execution.executionLog.add("Input parameters: ${inputs.keys.joinToString(", ")}")

        val entryNodes = findEntryNodes(graph)
        if (entryNodes.isEmpty()) {
            execution.executionLog.add("No entry nodes found in graph")
            return GraphExecutionResult.Failure(execution.id, "No entry nodes found in graph")
        }

        execution.executionLog.add(
            "Found ${entryNodes.size} entry nodes: ${
                entryNodes.map { it.name }.joinToString(", ")
            }"
        )

        val nodeInputs = HashMap<String, Map<String, Any?>>()
        entryNodes.forEach { node ->
            nodeInputs[node.id] = inputs
            execution.executionLog.add("Initializing entry node '${node.name}' with inputs: ${inputs.keys.joinToString(", ")}")
        }

        val executionOrder = calculateExecutionOrder(graph)
        execution.executionLog.add("Execution order: ${executionOrder.joinToString(" -> ")}")
        val nodeQueue = executionOrder.toMutableList()
        var executionStep = 1

        while (nodeQueue.isNotEmpty()) {
            val currentNodeId = nodeQueue.removeAt(0)
            val currentNode = graph.nodes.find { it.id == currentNodeId }
                ?: continue

            execution.executionLog.add("Step $executionStep: Processing node '${currentNode.name}' (ID: $currentNodeId, Type: ${currentNode.type})")

            if (execution.completedNodes.contains(currentNodeId) ||
                execution.failedNodes.contains(currentNodeId)
            ) {
                execution.executionLog.add("Node '${currentNode.name}' already processed, skipping")
                continue
            }

            val dependencies = getNodeDependencies(graph, currentNodeId)
            if (!dependencies.all { execution.completedNodes.contains(it) }) {
                val missingDeps = dependencies.filter { !execution.completedNodes.contains(it) }
                execution.executionLog.add(
                    "Node '${currentNode.name}' waiting for dependencies: ${
                        missingDeps.joinToString(
                            ", "
                        )
                    }"
                )
                nodeQueue.add(currentNodeId)
                continue
            }

            execution.executionLog.add("All dependencies satisfied for node '${currentNode.name}', executing...")

            val nodeInput = nodeInputs[currentNodeId] ?: emptyMap()
            execution.executionLog.add("Node inputs: ${nodeInput.keys.joinToString(", ")}")

            val nodeStartTime = System.currentTimeMillis()
            when (val nodeResult = executeNodeInternal(currentNode, nodeInput, context)) {
                is NodeExecutionResult.Success -> {
                    val nodeDuration = System.currentTimeMillis() - nodeStartTime
                    execution.completedNodes.add(currentNodeId)
                    execution.nodeResults[currentNodeId] = nodeResult.result
                    execution.executionLog.add("Node '${currentNode.name}' completed successfully in ${nodeDuration}ms")
                    execution.executionLog.add("Node outputs: ${nodeResult.result.keys.joinToString(", ")}")

                    propagateNodeOutputs(graph, currentNodeId, nodeResult.result, nodeInputs)
                }

                is NodeExecutionResult.Failure -> {
                    val nodeDuration = System.currentTimeMillis() - nodeStartTime
                    execution.failedNodes.add(currentNodeId)
                    execution.executionLog.add("Node '${currentNode.name}' failed after ${nodeDuration}ms: ${nodeResult.error}")

                    if (isCriticalNode(graph, currentNodeId)) {
                        execution.executionLog.add(" Critical node '${currentNode.name}' failed, aborting graph execution")
                        return GraphExecutionResult.Failure(execution.id, "Critical node failed: ${nodeResult.error}")
                    } else {
                        execution.executionLog.add(" Non-critical node '${currentNode.name}' failed, continuing execution")
                    }
                }
            }

            executionStep++
        }

        val totalNodes = graph.nodes.size
        val completedNodes = execution.completedNodes.size
        val failedNodes = execution.failedNodes.size
        val executionDuration = System.currentTimeMillis() - execution.startTime

        execution.executionLog.add("Graph execution completed in ${executionDuration}ms")
        execution.executionLog.add("Final results: $completedNodes/$totalNodes nodes completed, $failedNodes failed")

        return if (completedNodes == totalNodes) {
            execution.executionLog.add("All nodes completed successfully!")
            GraphExecutionResult.Success(execution.id, execution.nodeResults)
        } else {
            execution.executionLog.add("Partial success: Some nodes failed or were skipped")
            GraphExecutionResult.PartialSuccess(
                execution.id,
                execution.nodeResults,
                "Completed $completedNodes/$totalNodes nodes, $failedNodes failed"
            )
        }
    }

    private suspend fun executeNodeInternal(
        node: GraphNode,
        inputs: Map<String, Any?>,
        context: GraphExecutionContext
    ): NodeExecutionResult {
        context.logger.info("Executing node '${node.name}' (Type: ${node.type}, ID: ${node.id})")
        context.logger.info("Node inputs: ${inputs.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
        context.logger.info("Node position: (${node.position.x}, ${node.position.y})")

        val actionHandler = extensionRegistry.getActionNode(node.type)
        val triggerHandler = extensionRegistry.getTriggerNode(node.type)

        val handler = actionHandler ?: triggerHandler
        if (handler == null) {
            context.logger.error("No handler found for node type: ${node.type}")
            return NodeExecutionResult.Failure("No handler found for node type: ${node.type}")
        }

        context.logger.info("Found handler: ${handler::class.simpleName}")

        return try {
            when (handler) {
                is ActionNodeHandler -> {
                    context.logger.info("Executing ActionNodeHandler for '${node.name}'")

                    val propertiesMap = convertPropertiesToMap(node.properties, context)
                    when (val result = handler.execute(inputs, propertiesMap)) {
                        is ActionResult.Success -> {
                            context.logger.info(
                                "ActionNode '${node.name}' succeeded with outputs: ${
                                    result.outputs.keys.joinToString(
                                        ", "
                                    )
                                }"
                            )
                            NodeExecutionResult.Success(node.id, result.outputs)
                        }

                        is ActionResult.Error -> {
                            context.logger.error("ActionNode '${node.name}' error: ${result.message}")
                            NodeExecutionResult.Failure(result.message)
                        }

                        is ActionResult.Skip -> {
                            context.logger.info("ActionNode '${node.name}' skipped")
                            NodeExecutionResult.Success(node.id, emptyMap())
                        }
                    }
                }

                is TriggerNodeHandler -> {
                    context.logger.info("Executing TriggerNodeHandler for '${node.name}'")
                    when (val result = handler.execute()) {
                        is TriggerResult.Success -> {
                            context.logger.info("TriggerNode '${node.name}' succeeded")

                            NodeExecutionResult.Success(node.id, inputs)
                        }

                        is TriggerResult.Error -> {
                            context.logger.error("TriggerNode '${node.name}' error: ${result.message}")
                            NodeExecutionResult.Failure(result.message)
                        }

                        is TriggerResult.Skip -> {
                            context.logger.info(" TriggerNode '${node.name}' skipped")
                            NodeExecutionResult.Success(node.id, emptyMap())
                        }
                    }
                }

                else -> {
                    context.logger.error(" Unknown handler type for node '${node.name}': ${handler::class.simpleName}")
                    NodeExecutionResult.Failure("Unknown handler type")
                }
            }
        } catch (e: Exception) {
            context.logger.error(" Exception in node '${node.name}' execution: ${e.message}", e)
            NodeExecutionResult.Failure("Node execution failed: ${e.message}")
        }
    }

    private fun findEntryNodes(graph: FlowGraph): List<GraphNode> {
        val nodesWithInputs = graph.connections.map { it.toNodeId }.toSet()
        return graph.nodes.filter { !nodesWithInputs.contains(it.id) }
    }

    private fun calculateExecutionOrder(graph: FlowGraph): List<String> {
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val result = mutableListOf<String>()

        fun visit(nodeId: String) {
            if (visiting.contains(nodeId)) {
                throw IllegalStateException("Circular dependency detected in graph")
            }
            if (visited.contains(nodeId)) return

            visiting.add(nodeId)


            val dependencies = getNodeDependencies(graph, nodeId)
            dependencies.forEach { visit(it) }

            visiting.remove(nodeId)
            visited.add(nodeId)
            result.add(nodeId)
        }

        graph.nodes.forEach { visit(it.id) }
        return result
    }

    private fun getNodeDependencies(graph: FlowGraph, nodeId: String): List<String> {
        return graph.connections
            .filter { it.toNodeId == nodeId }
            .map { it.fromNodeId }
    }

    private fun isCriticalNode(graph: FlowGraph, nodeId: String): Boolean {

        return graph.connections.any { it.fromNodeId == nodeId }
    }

    private fun propagateNodeOutputs(
        graph: FlowGraph,
        nodeId: String,
        outputs: Map<String, Any?>,
        nodeInputs: MutableMap<String, Map<String, Any?>>
    ) {
        val outgoingConnections = graph.connections.filter { it.fromNodeId == nodeId }

        outgoingConnections.forEach { connection ->
            val targetNodeId = connection.toNodeId
            val targetNode = graph.nodes.find { it.id == targetNodeId } ?: return@forEach


            val targetPort = targetNode.inputs.find { it.id == connection.toPortId } ?: return@forEach
            val sourcePort = graph.nodes.find { it.id == nodeId }?.outputs?.find { it.id == connection.fromPortId }
                ?: return@forEach


            val outputValue = outputs[sourcePort.name]


            val currentInputs = nodeInputs.getOrDefault(targetNodeId, emptyMap()).toMutableMap()
            currentInputs[targetPort.name] = outputValue
            nodeInputs[targetNodeId] = currentInputs
        }
    }

    private fun createDefaultExecutionContext(executionId: String): GraphExecutionContext {
        return object : GraphExecutionContext {
            override val executionId: String = executionId
            override val graphId: String = ""
            override val variables: MutableMap<String, Any?> = ConcurrentHashMap()
            override val startTime: Long = System.currentTimeMillis()
            override val logger: com.thedevjade.flow.extension.api.ExtensionLogger =
                object : com.thedevjade.flow.extension.api.ExtensionLogger {
                    override fun debug(message: String, vararg args: Any?) = println("[DEBUG] $message")
                    override fun info(message: String, vararg args: Any?) = println("[INFO] $message")
                    override fun warn(message: String, vararg args: Any?) = println("[WARN] $message")
                    override fun error(message: String, vararg args: Any?) = println("[ERROR] $message")
                    override fun error(message: String, throwable: Throwable, vararg args: Any?) =
                        println("[ERROR] $message: ${throwable.message}")
                }
        }
    }

    private fun generateExecutionId(): String = "exec_${executionIdGenerator.incrementAndGet()}"


    private fun convertPropertiesToMap(properties: Any?, context: GraphExecutionContext): Map<String, Any?> {
        return when (properties) {
            is Map<*, *> -> properties as Map<String, Any?>
            is JsonObject -> {
                try {
                    properties.mapValues { (_, value) ->
                        when (value) {
                            is JsonPrimitive -> {
                                if (value.isString) {
                                    value.content
                                } else {

                                    try {
                                        value.content.toDoubleOrNull() ?: value.content
                                    } catch (e: Exception) {
                                        value.content
                                    }
                                }
                            }

                            is JsonArray -> value.map { it.toString() }
                            is JsonObject -> value.toString()
                            else -> value.toString()
                        }
                    }
                } catch (e: Exception) {
                    context.logger.warn("Failed to convert JsonObject properties: ${e.message}")
                    emptyMap<String, Any?>()
                }
            }

            else -> {
                context.logger.warn("Unknown properties type: ${properties?.javaClass?.simpleName}")
                emptyMap<String, Any?>()
            }
        }
    }

    fun dispose() {
        activeExecutions.clear()
        scope.cancel()
    }
}


interface GraphExecutionContext {
    val executionId: String
    val graphId: String
    val variables: MutableMap<String, Any?>
    val startTime: Long
    val logger: com.thedevjade.flow.extension.api.ExtensionLogger
}


data class GraphExecution(
    val id: String,
    val graph: FlowGraph,
    val context: GraphExecutionContext,
    var status: ExecutionStatus,
    val startTime: Long,
    var endTime: Long? = null,
    var currentNodeId: String?,
    val completedNodes: MutableSet<String>,
    val failedNodes: MutableSet<String>,
    val nodeResults: ConcurrentHashMap<String, Map<String, Any?>>,
    val executionLog: MutableList<String>
) {
    fun printExecutionLog() {
        context.logger.info("=== GRAPH EXECUTION LOG (ID: $id) ===")
        context.logger.info("Graph: '${graph.name}' (${graph.id})")
        context.logger.info("  Duration: ${if (endTime != null) "${endTime!! - startTime}ms" else "Still running"}")
        context.logger.info(" Status: $status")
        context.logger.info(" Completed nodes: ${completedNodes.size}/${graph.nodes.size}")
        context.logger.info(" Failed nodes: ${failedNodes.size}")
        context.logger.info(" Execution steps:")

        executionLog.forEachIndexed { index, logEntry ->
            context.logger.info("  ${index + 1}. $logEntry")
        }

        context.logger.info(" === END EXECUTION LOG ===")
    }
}


enum class ExecutionStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}


sealed class GraphExecutionResult {
    data class Success(val executionId: String, val results: Map<String, Map<String, Any?>>) : GraphExecutionResult()
    data class Failure(val executionId: String, val error: String, val cause: Throwable? = null) :
        GraphExecutionResult()

    data class PartialSuccess(
        val executionId: String,
        val results: Map<String, Map<String, Any?>>,
        val message: String
    ) : GraphExecutionResult()
}


sealed class NodeExecutionResult {
    data class Success(val nodeId: String, val result: Map<String, Any?>) : NodeExecutionResult()
    data class Failure(val error: String) : NodeExecutionResult()
}
