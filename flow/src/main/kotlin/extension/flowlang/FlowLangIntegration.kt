package com.thedevjade.flow.extension.flowlang

import com.thedevjade.flow.extension.api.*
import com.thedevjade.flow.extension.registry.SimpleExtensionRegistry
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
    ): Any? {

        // @TODO Actually execute
        return "FlowLang execution: $code"
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


    fun dispose() {
        registeredFunctions.clear()
        registeredTypes.clear()
        registeredEvents.clear()
        scope.cancel()
    }
}