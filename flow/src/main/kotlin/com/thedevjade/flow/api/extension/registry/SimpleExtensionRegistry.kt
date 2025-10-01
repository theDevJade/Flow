package com.thedevjade.flow.extension.registry

import com.thedevjade.flow.api.FlowCore
import com.thedevjade.flow.extension.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExtensionName(val name: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowLangFunction(val name: String, val description: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowLangEvent(val name: String, val description: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowLangType(val name: String, val description: String = "")

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TerminalCommand(val name: String, val description: String = "")


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
    val outputs: List<GraphPort>,
    val properties: List<NodeProperty> = emptyList()
)

data class NodeProperty(
    val name: String,
    val type: String,
    val description: String,
    val defaultValue: Any? = null,
    val required: Boolean = false,
    val options: List<Any>? = null
)

data class ExtensionInfo(
    val name: String,
    val extensionClass: KClass<*>,
    val instance: Any,
    val registeredHandlers: MutableSet<String> = mutableSetOf()
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


    private val extensionRegistry = ConcurrentHashMap<String, ExtensionInfo>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    fun registerExtension(extensionClass: KClass<*>, instance: Any) {
        try {
            val extensionName = getExtensionName(extensionClass, instance)
            val extensionInfo = ExtensionInfo(extensionName, extensionClass, instance)


            registerAllHandlers(extensionClass, instance, extensionInfo)

            extensionRegistry[extensionName] = extensionInfo
            println("Successfully registered extension: $extensionName")
        } catch (e: Exception) {
            println("Failed to register extension: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getExtensionName(extensionClass: KClass<*>, instance: Any): String {

        val nameAnnotation = extensionClass.annotations.find { it is ExtensionName } as? ExtensionName
        if (nameAnnotation != null) {
            return nameAnnotation.name
        }


        try {
            val nameProperty = extensionClass.members.find { it.name == "name" }
            if (nameProperty != null) {
                val nameValue = nameProperty.call(instance) as? String
                if (!nameValue.isNullOrBlank()) {
                    return nameValue
                }
            }
        } catch (e: Exception) {
            // fallback
        }


        return extensionClass.simpleName ?: "UnknownExtension"
    }

    private fun registerAllHandlers(extensionClass: KClass<*>, instance: Any, extensionInfo: ExtensionInfo) {

        registerTriggerNodes(extensionClass, instance, extensionInfo)


        registerActionNodes(extensionClass, instance, extensionInfo)


        registerFlowLangFunctions(extensionClass, instance, extensionInfo)


        registerFlowLangEvents(extensionClass, instance, extensionInfo)


        registerFlowLangTypes(extensionClass, instance, extensionInfo)


        registerTerminalCommands(extensionClass, instance, extensionInfo)
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
                outputs = convertToWebSocketPorts(handler.outputs, false),
                properties = extractNodeProperties(handler)
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
                outputs = convertToWebSocketPorts(handler.outputs, false),
                properties = extractNodeProperties(handler)
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
                        when (val result = executeMethod.call(instance)) {
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

            override suspend fun execute(inputs: Map<String, Any?>, properties: Map<String, Any?>): ActionResult {
                return try {

                    if (executeMethod != null) {
                        when (val result = executeMethod.call(instance, inputs)) {
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

    fun registerActionNode(name: String, handler: ActionNodeHandler) {
        actionNodes[name] = handler
    }

    fun registerTriggerNode(name: String, handler: TriggerNodeHandler) {
        triggerNodes[name] = handler
    }

    fun registerFlowLangFunction(name: String, handler: FlowLangFunctionHandler) {
        flowLangFunctions[name] = handler
    }

    fun registerFlowLangType(name: String, handler: FlowLangTypeHandler) {
        flowLangTypes[name] = handler
    }

    fun registerFlowLangEvent(name: String, handler: FlowLangEventHandler) {
        flowLangEvents[name] = handler
    }

    fun registerTerminalCommand(name: String, handler: TerminalCommandHandler) {
        terminalCommands[name] = handler
    }

    private fun registerTriggerNodes(extensionClass: KClass<*>, instance: Any, extensionInfo: ExtensionInfo) {
        val triggerAnnotation = extensionClass.annotations.find { it is TriggerNode } as? TriggerNode
        if (triggerAnnotation != null) {
            val handler = createBasicTriggerNodeHandler(extensionClass, instance, triggerAnnotation)
            triggerNodes[triggerAnnotation.name] = handler
            extensionInfo.registeredHandlers.add("trigger:${triggerAnnotation.name}")
        }
    }

    private fun registerActionNodes(extensionClass: KClass<*>, instance: Any, extensionInfo: ExtensionInfo) {
        val actionAnnotation = extensionClass.annotations.find { it is ActionNode } as? ActionNode
        if (actionAnnotation != null) {
            val handler = createBasicActionNodeHandler(extensionClass, instance, actionAnnotation)
            actionNodes[actionAnnotation.name] = handler
            extensionInfo.registeredHandlers.add("action:${actionAnnotation.name}")
        }
    }

    private fun registerFlowLangFunctions(extensionClass: KClass<*>, instance: Any, extensionInfo: ExtensionInfo) {

        extensionClass.members.forEach { member ->
            val functionAnnotation = member.annotations.find { it is FlowLangFunction } as? FlowLangFunction
            if (functionAnnotation != null && member is KFunction<*>) {
                val handler = createFlowLangFunctionHandler(instance, member, functionAnnotation)
                flowLangFunctions[functionAnnotation.name] = handler
                extensionInfo.registeredHandlers.add("flowlang_function:${functionAnnotation.name}")
            }
        }
    }

    private fun registerFlowLangEvents(extensionClass: KClass<*>, instance: Any, extensionInfo: ExtensionInfo) {

        extensionClass.members.forEach { member ->
            val eventAnnotation = member.annotations.find { it is FlowLangEvent } as? FlowLangEvent
            if (eventAnnotation != null && member is KFunction<*>) {
                val handler = createFlowLangEventHandler(instance, member, eventAnnotation)
                flowLangEvents[eventAnnotation.name] = handler
                extensionInfo.registeredHandlers.add("flowlang_event:${eventAnnotation.name}")
            }
        }
    }

    private fun registerFlowLangTypes(extensionClass: KClass<*>, instance: Any, extensionInfo: ExtensionInfo) {

        extensionClass.members.forEach { member ->
            val typeAnnotation = member.annotations.find { it is FlowLangType } as? FlowLangType
            if (typeAnnotation != null && member is KFunction<*>) {
                val handler = createFlowLangTypeHandler(instance, member, typeAnnotation)
                flowLangTypes[typeAnnotation.name] = handler
                extensionInfo.registeredHandlers.add("flowlang_type:${typeAnnotation.name}")
            }
        }
    }

    private fun registerTerminalCommands(extensionClass: KClass<*>, instance: Any, extensionInfo: ExtensionInfo) {

        extensionClass.members.forEach { member ->
            val commandAnnotation = member.annotations.find { it is TerminalCommand } as? TerminalCommand
            if (commandAnnotation != null && member is KFunction<*>) {
                val handler = createTerminalCommandHandler(instance, member, commandAnnotation)
                terminalCommands[commandAnnotation.name] = handler
                extensionInfo.registeredHandlers.add("terminal_command:${commandAnnotation.name}")
            }
        }
    }


    fun unregisterExtension(extensionName: String): Boolean {
        val extensionInfo = extensionRegistry.remove(extensionName) ?: return false

        try {

            extensionInfo.registeredHandlers.forEach { handlerId ->
                val (type, name) = handlerId.split(":", limit = 2)
                when (type) {
                    "trigger" -> triggerNodes.remove(name)
                    "action" -> actionNodes.remove(name)
                    "flowlang_function" -> flowLangFunctions.remove(name)
                    "flowlang_event" -> flowLangEvents.remove(name)
                    "flowlang_type" -> flowLangTypes.remove(name)
                    "terminal_command" -> terminalCommands.remove(name)
                }
            }

            println("Successfully unregistered extension: $extensionName")
            return true
        } catch (e: Exception) {
            println("Failed to unregister extension $extensionName: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun getRegisteredExtensions(): Map<String, ExtensionInfo> = extensionRegistry.toMap()

    fun isExtensionRegistered(extensionName: String): Boolean = extensionRegistry.containsKey(extensionName)

    private fun createFlowLangFunctionHandler(
        instance: Any,
        method: KFunction<*>,
        annotation: FlowLangFunction
    ): FlowLangFunctionHandler {
        return object : FlowLangFunctionHandler {
            override val name: String = annotation.name
            override val description: String = annotation.description
            override val category: String = "Extension"
            override val async: Boolean = false
            override val parameters: List<FlowLangParameterDefinition> = extractMethodParameters(method)

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any? {
                return try {
                    method.call(instance, *args)
                } catch (e: Exception) {
                    println("Error executing FlowLang function ${annotation.name}: ${e.message}")
                    null
                }
            }
        }
    }

    private fun createFlowLangEventHandler(
        instance: Any,
        method: KFunction<*>,
        annotation: FlowLangEvent
    ): FlowLangEventHandler {
        return object : FlowLangEventHandler {
            override val name: String = annotation.name
            override val description: String = annotation.description
            override val category: String = "Extension"

            override suspend fun handle(context: FlowLangContext, event: Any) {
                try {
                    method.call(instance, context, event)
                } catch (e: Exception) {
                    println("Error handling FlowLang event ${annotation.name}: ${e.message}")
                }
            }
        }
    }

    private fun createFlowLangTypeHandler(
        instance: Any,
        method: KFunction<*>,
        annotation: FlowLangType
    ): FlowLangTypeHandler {
        return object : FlowLangTypeHandler {
            override val name: String = annotation.name
            override val description: String = annotation.description
            override val category: String = "Extension"
            override val javaType: Class<*> = extractMethodReturnType(method)

            override fun convert(value: Any?): Any? {
                return try {
                    method.call(instance, value)
                } catch (e: Exception) {
                    println("Error converting FlowLang type ${annotation.name}: ${e.message}")
                    value
                }
            }

            override fun validate(value: Any?): Boolean {
                return try {
                    method.call(instance, value) as? Boolean ?: false
                } catch (e: Exception) {
                    println("Error validating FlowLang type ${annotation.name}: ${e.message}")
                    false
                }
            }
        }
    }

    private fun createTerminalCommandHandler(
        instance: Any,
        method: KFunction<*>,
        annotation: TerminalCommand
    ): TerminalCommandHandler {
        return object : TerminalCommandHandler {
            override val name: String = annotation.name
            override val description: String = annotation.description
            override val usage: String = ""
            override val aliases: List<String> = emptyList()
            override val permission: String = ""
            override val async: Boolean = false

            override suspend fun execute(context: CommandContext, args: List<String>): CommandResult {
                return try {
                    when (val result = method.call(instance, context, args)) {
                        is CommandResult -> result
                        is Boolean -> if (result) CommandResult.Success else CommandResult.Error("Command failed")
                        else -> CommandResult.Success
                    }
                } catch (e: Exception) {
                    println("Error executing terminal command ${annotation.name}: ${e.message}")
                    CommandResult.Error("Command execution failed: ${e.message}")
                }
            }
        }
    }

    private fun extractMethodParameters(method: KFunction<*>): List<FlowLangParameterDefinition> {
        return method.parameters
            .drop(1)
            .map { param ->
                val paramAnnotation =
                    param.annotations.find { it is FlowLangParameter } as? FlowLangParameter
                FlowLangParameterDefinition(
                    name = paramAnnotation?.name?.ifEmpty { param.name } ?: param.name ?: "param",
                    type = paramAnnotation?.type ?: detectType(param.type.classifier as? KClass<*>),
                    description = paramAnnotation?.description ?: "",
                    required = paramAnnotation?.required ?: !param.isOptional,
                    defaultValue = paramAnnotation?.defaultValue ?: ""
                )
            }
    }

    private fun extractMethodReturnType(method: KFunction<*>): Class<*> {
        return when (val returnType = method.returnType.classifier as? KClass<*>) {
            String::class -> String::class.java
            Int::class -> Int::class.java
            Long::class -> Long::class.java
            Double::class -> Double::class.java
            Float::class -> Float::class.java
            Boolean::class -> Boolean::class.java
            List::class -> List::class.java
            Map::class -> Map::class.java
            else -> returnType?.java ?: Any::class.java
        }
    }

    private fun extractNodeProperties(handler: Any): List<NodeProperty> {
        return when (handler) {
            is TriggerNodeHandler -> extractPropertiesFromHandler(handler)
            is ActionNodeHandler -> extractPropertiesFromHandler(handler)
            else -> emptyList()
        }
    }

    private fun extractPropertiesFromHandler(handler: Any): List<NodeProperty> {


        return when (handler.javaClass.simpleName) {
            "StringOutputActionNode" -> listOf(
                NodeProperty(
                    name = "value",
                    type = "string",
                    description = "The string value to output",
                    defaultValue = "Hello World",
                    required = false
                )
            )

            "NumberOutputActionNode" -> listOf(
                NodeProperty(
                    name = "value",
                    type = "number",
                    description = "The number value to output",
                    defaultValue = 42,
                    required = false
                )
            )

            else -> emptyList()
        }
    }

    fun dispose() {
        triggerNodes.clear()
        actionNodes.clear()
        flowLangFunctions.clear()
        flowLangEvents.clear()
        flowLangTypes.clear()
        terminalCommands.clear()
        extensionRegistry.clear()
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

    suspend fun execute(inputs: Map<String, Any?>, properties: Map<String, Any?> = emptyMap()): ActionResult
}