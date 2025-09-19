package com.thedevjade.flow.extension.executor

import com.thedevjade.flow.api.graph.FlowGraph
import com.thedevjade.flow.api.graph.GraphNode
import com.thedevjade.flow.extension.api.*
import com.thedevjade.flow.extension.registry.SimpleExtensionRegistry
import com.thedevjade.flow.extension.registry.TriggerNodeHandler
import com.thedevjade.flow.extension.registry.ActionNodeHandler
import kotlinx.coroutines.*
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
            result
        } catch (e: Exception) {
            execution.status = ExecutionStatus.FAILED
            execution.endTime = System.currentTimeMillis()
            execution.executionLog.add("Execution failed: ${e.message}")
            GraphExecutionResult.Failure(executionId, e.message ?: "Unknown error", e)
        } finally {
            activeExecutions.remove(executionId)
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


        val entryNodes = findEntryNodes(graph)
        if (entryNodes.isEmpty()) {
            return GraphExecutionResult.Failure(execution.id, "No entry nodes found in graph")
        }


        val nodeInputs = HashMap<String, Map<String, Any?>>()
        entryNodes.forEach { node ->
            nodeInputs[node.id] = inputs
        }


        val executionOrder = calculateExecutionOrder(graph)
        val nodeQueue = executionOrder.toMutableList()

        while (nodeQueue.isNotEmpty()) {
            val currentNodeId = nodeQueue.removeAt(0)
            val currentNode = graph.nodes.find { it.id == currentNodeId }
                ?: continue

            if (execution.completedNodes.contains(currentNodeId) ||
                execution.failedNodes.contains(currentNodeId)) {
                continue
            }


            val dependencies = getNodeDependencies(graph, currentNodeId)
            if (!dependencies.all { execution.completedNodes.contains(it) }) {

                nodeQueue.add(currentNodeId)
                continue
            }


            val nodeInput = nodeInputs[currentNodeId] ?: emptyMap()
            val nodeResult = executeNodeInternal(currentNode, nodeInput, context)

            when (nodeResult) {
                is NodeExecutionResult.Success -> {
                    execution.completedNodes.add(currentNodeId)
                    execution.nodeResults[currentNodeId] = nodeResult.result
                    execution.executionLog.add("Node '${currentNode.name}' completed successfully")


                    propagateNodeOutputs(graph, currentNodeId, nodeResult.result, nodeInputs)
                }
                is NodeExecutionResult.Failure -> {
                    execution.failedNodes.add(currentNodeId)
                    execution.executionLog.add("Node '${currentNode.name}' failed: ${nodeResult.error}")


                    if (isCriticalNode(graph, currentNodeId)) {
                        return GraphExecutionResult.Failure(execution.id, "Critical node failed: ${nodeResult.error}")
                    }
                }
            }
        }


        val totalNodes = graph.nodes.size
        val completedNodes = execution.completedNodes.size
        val failedNodes = execution.failedNodes.size

        return if (completedNodes == totalNodes) {
            GraphExecutionResult.Success(execution.id, execution.nodeResults)
        } else {
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

        val actionHandler = extensionRegistry.getActionNode(node.type)
        val triggerHandler = extensionRegistry.getTriggerNode(node.type)

        val handler = actionHandler ?: triggerHandler
            ?: return NodeExecutionResult.Failure("No handler found for node type: ${node.type}")

        return try {
            when (handler) {
                is ActionNodeHandler -> {
                    val result = handler.execute(inputs)
                    when (result) {
                        is ActionResult.Success -> NodeExecutionResult.Success(node.id, result.outputs)
                        is ActionResult.Error -> NodeExecutionResult.Failure(result.message)
                        is ActionResult.Skip -> NodeExecutionResult.Success(node.id, emptyMap())
                    }
                }
                is TriggerNodeHandler -> {
                    val result = handler.execute()
                    when (result) {
                        is TriggerResult.Success -> NodeExecutionResult.Success(node.id, emptyMap())
                        is TriggerResult.Error -> NodeExecutionResult.Failure(result.message)
                        is TriggerResult.Skip -> NodeExecutionResult.Success(node.id, emptyMap())
                    }
                }
                else -> NodeExecutionResult.Failure("Unknown handler type")
            }
        } catch (e: Exception) {
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
        }
    }

    private fun generateExecutionId(): String = "exec_${executionIdGenerator.incrementAndGet()}"


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
)


enum class ExecutionStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}


sealed class GraphExecutionResult {
    data class Success(val executionId: String, val results: Map<String, Map<String, Any?>>) : GraphExecutionResult()
    data class Failure(val executionId: String, val error: String, val cause: Throwable? = null) : GraphExecutionResult()
    data class PartialSuccess(val executionId: String, val results: Map<String, Map<String, Any?>>, val message: String) : GraphExecutionResult()
}


sealed class NodeExecutionResult {
    data class Success(val nodeId: String, val result: Map<String, Any?>) : NodeExecutionResult()
    data class Failure(val error: String) : NodeExecutionResult()
}
