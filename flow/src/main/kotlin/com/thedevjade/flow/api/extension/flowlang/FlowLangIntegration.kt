package com.thedevjade.flow.extension.flowlang

import com.thedevjade.flow.extension.api.FlowLangContext
import com.thedevjade.flow.extension.api.FlowLangEventHandler
import com.thedevjade.flow.extension.api.FlowLangFunctionHandler
import com.thedevjade.flow.extension.api.FlowLangTypeHandler
import com.thedevjade.flow.extension.registry.SimpleExtensionRegistry
import com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.FlowLangEngine
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap


class FlowLangIntegration(
    private val extensionRegistry: SimpleExtensionRegistry
) {
    private val registeredFunctions = ConcurrentHashMap<String, FlowLangFunctionHandler>()
    private val registeredTypes = ConcurrentHashMap<String, FlowLangTypeHandler>()
    private val registeredEvents = ConcurrentHashMap<String, FlowLangEventHandler>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    fun initialize() {

        registeredFunctions.putAll(extensionRegistry.getAllFlowLangFunctions())
        registeredTypes.putAll(extensionRegistry.getAllFlowLangTypes())
        registeredEvents.putAll(extensionRegistry.getAllFlowLangEvents())
    }


    suspend fun executeFlowLang(
        code: String,
        context: FlowLangContext? = null
    ): Any {
        return try {

            val engine = FlowLangEngine.getInstance()


            val langExecutionContext =
                com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangContext()


            context?.let { flowContext ->
                flowContext.variables.forEach { (name, value) ->
                    langExecutionContext.setVariable(name, value)
                }
            }


            registeredFunctions.forEach { (name, handler) ->
                val flowLangFunction =
                    com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangFunction(
                        name = name,
                        implementation = { args: Array<Any?> ->

                            val bridgeContext = createFlowLangContext()

                            bridgeContext.variables.putAll(context?.variables ?: emptyMap())

                            runBlocking {
                                handler.execute(bridgeContext, args)
                            }
                        },
                        parameters = extractFlowLangParameters(handler.parameters)
                    )
                engine.registerFunction(flowLangFunction)
            }

            registeredTypes.forEach { (name, handler) ->
                val flowLangType = com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangType(
                    name = name,
                    kotlinType = handler.javaType
                )
                engine.registerType(flowLangType)
            }

            registeredEvents.forEach { (name, _) ->
                val flowLangEvent =
                    com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangEvent(
                        name = name,
                        parameters = extractFlowLangEventParameters(registeredEvents[name]!!),
                        description = "Extension event: $name"
                    )
                engine.registerEvent(flowLangEvent)
            }


            val result = engine.execute(code, langExecutionContext)


            context?.let { flowContext ->
                // @todo FlowLangContext needs to expose variables to transfer back
            }

            result ?: "null"
        } catch (e: Exception) {
            scope.launch {
                println("FlowLang execution error: ${e.message}")
                e.printStackTrace()
            }
            "Error executing FlowLang: ${e.message}"
        }
    }


    fun triggerEvent(eventName: String, vararg parameters: Any) {
        val handler = registeredEvents[eventName]
        if (handler != null) {
            scope.launch {
                try {
                    val context = createFlowLangContext()
                    handler.handle(context, parameters)
                } catch (e: Exception) {
                    println("Event handler '$eventName' failed: ${e.message}")
                }
            }
        }
    }


    fun getRegisteredFunctions(): Map<String, FlowLangFunctionHandler> = registeredFunctions.toMap()


    fun getRegisteredTypes(): Map<String, FlowLangTypeHandler> = registeredTypes.toMap()


    fun getRegisteredEvents(): Map<String, FlowLangEventHandler> = registeredEvents.toMap()

    private fun createFlowLangContext(): FlowLangContext {
        return object : FlowLangContext {
            override val variables: MutableMap<String, Any?> = ConcurrentHashMap()
            override val functions: MutableMap<String, FlowLangFunctionHandler> =
                registeredFunctions.toMutableMap()
            override val types: MutableMap<String, Class<*>> =
                registeredTypes.mapValues { it.value.javaType }.toMutableMap()
            override val executionId: String = "flowlang_${System.currentTimeMillis()}"
        }
    }


    private fun extractFlowLangParameters(parameters: List<com.thedevjade.flow.extension.api.FlowLangParameterDefinition>): Array<com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangParameter> {
        return parameters.map { param ->
            com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangParameter(
                name = param.name,
                typeName = param.type,
                isOptional = !param.required,
                defaultValue = param.defaultValue.ifEmpty { null }
            )
        }.toTypedArray()
    }

    private fun extractFlowLangEventParameters(handler: FlowLangEventHandler): Array<com.thedevjade.io.flowlang.com.thedevjade.flow.flowlang.language.memory.FlowLangParameter> {
        // @TODO extract parameters for use in events
        return emptyArray()
    }

    fun dispose() {
        registeredFunctions.clear()
        registeredTypes.clear()
        registeredEvents.clear()
        scope.cancel()
    }
}