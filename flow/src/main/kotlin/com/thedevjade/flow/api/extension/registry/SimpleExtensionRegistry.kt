package com.thedevjade.flow.extension.registry

import com.thedevjade.flow.api.FlowCore
import com.thedevjade.flow.extension.api.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction


data class GraphPort(
    val id: String,
    val name: String,
    val isInput: Boolean,
    val color: Long
)


data class NodeTemplate(
    val name: String,
    val category: String,
    val description: String,
    val icon: String,
    val color: String,
    val nodeType: String,
    val inputs: List<GraphPort>,
    val outputs: List<GraphPort>
)


class SimpleExtensionRegistry(
    private val flowCore: FlowCore
) {
    private val triggerNodes = ConcurrentHashMap<String, TriggerNodeHandler>()
    private val actionNodes = ConcurrentHashMap<String, ActionNodeHandler>()
    private val flowLangFunctions = ConcurrentHashMap<String, FlowLangFunctionHandler>()
    private val flowLangEvents = ConcurrentHashMap<String, FlowLangEventHandler>()
    private val flowLangTypes = ConcurrentHashMap<String, FlowLangTypeHandler>()
    private val terminalCommands = ConcurrentHashMap<String, TerminalCommandHandler>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    fun registerExtension(extensionClass: KClass<*>, instance: Any) {
        // @TODO Use full reflection, working now but not good

        createBasicHandlers(extensionClass, instance)
    }

    private fun createBasicHandlers(extensionClass: KClass<*>, instance: Any) {

        val triggerAnnotation = extensionClass.annotations.find { it is TriggerNode } as? TriggerNode
        if (triggerAnnotation != null) {
            val handler = createBasicTriggerNodeHandler(extensionClass, instance, triggerAnnotation)
            triggerNodes[triggerAnnotation.name] = handler
        }


        val actionAnnotation = extensionClass.annotations.find { it is ActionNode } as? ActionNode
        if (actionAnnotation != null) {
            val handler = createBasicActionNodeHandler(extensionClass, instance, actionAnnotation)
            actionNodes[actionAnnotation.name] = handler
        }
    }


    private fun detectInputs(executeMethod: KFunction<*>?): List<GraphPortDefinition> {
        if (executeMethod == null) return emptyList()

        return executeMethod.parameters
            .drop(1)
            .filter { it.name != "inputs" }
            .map { param ->
                val portAnnotation = param.annotations.find { it is Port } as? Port
                GraphPortDefinition(
                    name = portAnnotation?.name?.ifEmpty { param.name } ?: param.name ?: "input",
                    type = portAnnotation?.type ?: detectType(param.type.classifier as? KClass<*>),
                    description = portAnnotation?.description ?: "",
                    required = portAnnotation?.required ?: !param.isOptional,
                    defaultValue = portAnnotation?.defaultValue ?: ""
                )
            }
    }


    private fun detectOutputs(nodeClass: KClass<*>): List<GraphPortDefinition> {
        return nodeClass.members
            .filter { it.annotations.any { ann -> ann is Output } }
            .map { property ->
                val outputAnnotation = property.annotations.find { it is Output } as? Output
                GraphPortDefinition(
                    name = outputAnnotation?.name?.ifEmpty { property.name } ?: property.name,
                    type = outputAnnotation?.type ?: "any",
                    description = outputAnnotation?.description ?: "",
                    required = false,
                    defaultValue = ""
                )
            }
    }


    private fun detectType(type: KClass<*>?): String {
        return when (type) {
            String::class -> "text"
            Int::class, Long::class, Double::class, Float::class -> "number"
            Boolean::class -> "boolean"
            Map::class -> "object"
            List::class -> "array"
            else -> type?.simpleName?.lowercase() ?: "any"
        }
    }


    fun convertToWebSocketPorts(ports: List<GraphPortDefinition>, isInput: Boolean): List<GraphPort> {
        return ports.mapIndexed { index, port ->
            GraphPort(
                id = "${if (isInput) "input" else "output"}_${index}",
                name = port.name,
                isInput = isInput,
                color = getPortColor(port.type)
            )
        }
    }


    private fun getPortColor(type: String): Long {
        return when (type.lowercase()) {
            "text" -> 0xFF4CAF50
            "number" -> 0xFF2196F3
            "boolean" -> 0xFFFF9800
            "object" -> 0xFF9C27B0
            "array" -> 0xFF607D8B
            else -> 0xFF757575
        }
    }


    fun getAvailableNodeTemplates(): Map<String, NodeTemplate> {
        val templates = mutableMapOf<String, NodeTemplate>()


        triggerNodes.forEach { (name, handler) ->
            templates[name] = NodeTemplate(
                name = handler.name,
                category = handler.category,
                description = handler.description,
                icon = handler.icon,
                color = handler.color,
                nodeType = "trigger",
                inputs = convertToWebSocketPorts(handler.inputs, true),
                outputs = convertToWebSocketPorts(handler.outputs, false)
            )
        }


        actionNodes.forEach { (name, handler) ->
            templates[name] = NodeTemplate(
                name = handler.name,
                category = handler.category,
                description = handler.description,
                icon = handler.icon,
                color = handler.color,
                nodeType = "action",
                inputs = convertToWebSocketPorts(handler.inputs, true),
                outputs = convertToWebSocketPorts(handler.outputs, false)
            )
        }

        return templates
    }

    private fun createBasicTriggerNodeHandler(
        extensionClass: KClass<*>,
        instance: Any,
        annotation: TriggerNode
    ): TriggerNodeHandler {
        val executeMethod = extensionClass.members.find { it.name == "execute" } as? KFunction<*>
        val inputs = detectInputs(executeMethod)
        val outputs = detectOutputs(extensionClass)

        return object : TriggerNodeHandler {
            override val name: String = annotation.name
            override val category: String = annotation.category
            override val description: String = annotation.description
            override val icon: String = annotation.icon
            override val color: String = annotation.color
            override val nodeType: NodeType = NodeType.TRIGGER
            override val inputs: List<GraphPortDefinition> = inputs
            override val outputs: List<GraphPortDefinition> = outputs

            override suspend fun execute(): TriggerResult {
                return try {

                    if (executeMethod != null) {
                        val result = executeMethod.call(instance)
                        when (result) {
                            is TriggerResult -> result
                            else -> TriggerResult.Success
                        }
                    } else {
                        TriggerResult.Error("No execute method found in trigger node")
                    }
                } catch (e: Exception) {
                    TriggerResult.Error("Trigger execution failed: ${e.message}")
                }
            }
        }
    }

    private fun createBasicActionNodeHandler(
        extensionClass: KClass<*>,
        instance: Any,
        annotation: ActionNode
    ): ActionNodeHandler {
        val executeMethod = extensionClass.members.find { it.name == "execute" } as? KFunction<*>
        val inputs = detectInputs(executeMethod)
        val outputs = detectOutputs(extensionClass)

        return object : ActionNodeHandler {
            override val name: String = annotation.name
            override val category: String = annotation.category
            override val description: String = annotation.description
            override val icon: String = annotation.icon
            override val color: String = annotation.color
            override val nodeType: NodeType = NodeType.ACTION
            override val inputs: List<GraphPortDefinition> = inputs
            override val outputs: List<GraphPortDefinition> = outputs

            override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
                return try {

                    if (executeMethod != null) {
                        val result = executeMethod.call(instance, inputs)
                        when (result) {
                            is ActionResult -> result
                            else -> ActionResult.Success(emptyMap())
                        }
                    } else {
                        ActionResult.Error("No execute method found in action node")
                    }
                } catch (e: Exception) {
                    ActionResult.Error("Action execution failed: ${e.message}")
                }
            }
        }
    }


    fun getTriggerNode(name: String): TriggerNodeHandler? = triggerNodes[name]
    fun getActionNode(name: String): ActionNodeHandler? = actionNodes[name]
    fun getFlowLangFunction(name: String): FlowLangFunctionHandler? = flowLangFunctions[name]
    fun getFlowLangEvent(name: String): FlowLangEventHandler? = flowLangEvents[name]
    fun getFlowLangType(name: String): FlowLangTypeHandler? = flowLangTypes[name]
    fun getTerminalCommand(name: String): TerminalCommandHandler? = terminalCommands[name]

    fun getAllTriggerNodes(): Map<String, TriggerNodeHandler> = triggerNodes.toMap()
    fun getAllActionNodes(): Map<String, ActionNodeHandler> = actionNodes.toMap()
    fun getAllFlowLangFunctions(): Map<String, FlowLangFunctionHandler> = flowLangFunctions.toMap()
    fun getAllFlowLangEvents(): Map<String, FlowLangEventHandler> = flowLangEvents.toMap()
    fun getAllFlowLangTypes(): Map<String, FlowLangTypeHandler> = flowLangTypes.toMap()
    fun getAllTerminalCommands(): Map<String, TerminalCommandHandler> = terminalCommands.toMap()


    fun dispose() {
        triggerNodes.clear()
        actionNodes.clear()
        flowLangFunctions.clear()
        flowLangEvents.clear()
        flowLangTypes.clear()
        terminalCommands.clear()
        scope.cancel()
    }
}


interface TriggerNodeHandler {
    val name: String
    val category: String
    val description: String
    val icon: String
    val color: String
    val nodeType: NodeType
    val inputs: List<GraphPortDefinition>
    val outputs: List<GraphPortDefinition>

    suspend fun execute(): TriggerResult
}


interface ActionNodeHandler {
    val name: String
    val category: String
    val description: String
    val icon: String
    val color: String
    val nodeType: NodeType
    val inputs: List<GraphPortDefinition>
    val outputs: List<GraphPortDefinition>

    suspend fun execute(inputs: Map<String, Any?>): ActionResult
}