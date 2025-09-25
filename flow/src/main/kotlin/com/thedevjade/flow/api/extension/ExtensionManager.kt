package com.thedevjade.flow.extension

import com.thedevjade.flow.api.FlowCore
import com.thedevjade.flow.api.graph.FlowGraph
import com.thedevjade.flow.extension.api.*
import com.thedevjade.flow.extension.executor.*
import com.thedevjade.flow.extension.flowlang.FlowLangIntegration
import com.thedevjade.flow.extension.loader.HotReloadExtensionLoader
import com.thedevjade.flow.extension.registry.ActionNodeHandler
import com.thedevjade.flow.extension.registry.SimpleExtensionRegistry
import com.thedevjade.flow.extension.registry.TriggerNodeHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap


class ExtensionManager(
    private val flowCore: FlowCore,
    private val extensionsDirectory: File = File("extensions")
) {
    private val extensionLoader = HotReloadExtensionLoader(flowCore, extensionsDirectory)
    private val extensionRegistry = SimpleExtensionRegistry(flowCore)
    private val graphExecutor = GraphExecutor(extensionRegistry)
    private val flowLangIntegration = FlowLangIntegration(extensionRegistry)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val loadedExtensions = ConcurrentHashMap<String, LoadedExtensionInfo>()


    suspend fun initialize() {

        extensionLoader.startWatching()


        flowLangIntegration.initialize()


        loadExistingExtensions()
    }


    suspend fun loadExtension(jarFile: File): ExtensionLoadResult {
        val result = extensionLoader.loadExtension(jarFile)

        if (result is ExtensionLoadResult.Success) {
            val extensionInfo = LoadedExtensionInfo(
                extension = result.extension,
                metadata = result.metadata,
                classLoader = extensionLoader.getLoadedExtension(result.metadata.name)?.classLoader
            )

            loadedExtensions[result.metadata.name] = extensionInfo


            registerExtensionWithRegistry(result.extension)


            flowLangIntegration.initialize()
        }

        return result
    }


    suspend fun reloadExtension(extensionName: String): ExtensionReloadResult {
        val result = extensionLoader.reloadExtension(extensionName)

        if (result is ExtensionReloadResult.Success) {
            val extensionInfo = LoadedExtensionInfo(
                extension = result.extension,
                metadata = result.metadata,
                classLoader = extensionLoader.getLoadedExtension(result.metadata.name)?.classLoader
            )

            loadedExtensions[result.metadata.name] = extensionInfo


            unregisterExtensionFromRegistry(extensionName)
            registerExtensionWithRegistry(result.extension)


            flowLangIntegration.initialize()
        }

        return result
    }


    suspend fun unloadExtension(extensionName: String): Boolean {
        val success = extensionLoader.unloadExtension(extensionName)

        if (success) {
            loadedExtensions.remove(extensionName)
            unregisterExtensionFromRegistry(extensionName)
            flowLangIntegration.initialize()
        }

        return success
    }


    suspend fun executeGraph(
        graph: FlowGraph,
        inputs: Map<String, Any?> = emptyMap(),
        context: GraphExecutionContext? = null
    ): GraphExecutionResult {
        return graphExecutor.executeGraph(graph, inputs, context)
    }


    suspend fun executeNode(
        graph: FlowGraph,
        nodeId: String,
        inputs: Map<String, Any?> = emptyMap(),
        context: GraphExecutionContext? = null
    ): NodeExecutionResult {
        return graphExecutor.executeNode(graph, nodeId, inputs, context)
    }


    suspend fun executeFlowLang(
        code: String,
        context: FlowLangContext? = null
    ): Any {
        return flowLangIntegration.executeFlowLang(code, context)
    }


    fun triggerFlowLangEvent(eventName: String, vararg parameters: Any) {
        flowLangIntegration.triggerEvent(eventName, *parameters)
    }


    fun getLoadedExtensions(): Map<String, LoadedExtensionInfo> = loadedExtensions.toMap()


    fun getLoadedExtension(name: String): LoadedExtensionInfo? = loadedExtensions[name]


    fun isExtensionLoaded(name: String): Boolean = loadedExtensions.containsKey(name)


    fun getTerminalCommands(): Map<String, TerminalCommandHandler> = extensionRegistry.getAllTerminalCommands()


    fun getTriggerNodes(): Map<String, TriggerNodeHandler> = extensionRegistry.getAllTriggerNodes()


    fun getActionNodes(): Map<String, ActionNodeHandler> = extensionRegistry.getAllActionNodes()


    fun getFlowLangFunctions(): Map<String, FlowLangFunctionHandler> = extensionRegistry.getAllFlowLangFunctions()


    fun getFlowLangEvents(): Map<String, FlowLangEventHandler> = extensionRegistry.getAllFlowLangEvents()


    fun getFlowLangTypes(): Map<String, FlowLangTypeHandler> = extensionRegistry.getAllFlowLangTypes()


    fun getNodeTemplates(): Map<String, com.thedevjade.flow.extension.registry.NodeTemplate> =
        extensionRegistry.getAvailableNodeTemplates()

    fun registerActionNode(name: String, handler: ActionNodeHandler) {
        extensionRegistry.registerActionNode(name, handler)
    }

    fun registerTriggerNode(name: String, handler: TriggerNodeHandler) {
        extensionRegistry.registerTriggerNode(name, handler)
    }

    fun registerFlowLangFunction(name: String, handler: FlowLangFunctionHandler) {
        extensionRegistry.registerFlowLangFunction(name, handler)
    }

    fun registerFlowLangType(name: String, handler: FlowLangTypeHandler) {
        extensionRegistry.registerFlowLangType(name, handler)
    }

    fun registerFlowLangEvent(name: String, handler: FlowLangEventHandler) {
        extensionRegistry.registerFlowLangEvent(name, handler)
    }

    fun registerTerminalCommand(name: String, handler: TerminalCommandHandler) {
        extensionRegistry.registerTerminalCommand(name, handler)
    }

    fun getActiveExecutions(): Map<String, GraphExecution> =
        graphExecutor.getActiveExecutions()


    fun cancelExecution(executionId: String): Boolean = graphExecutor.cancelExecution(executionId)

    private suspend fun loadExistingExtensions() {
        val loadedExtensions = extensionLoader.getLoadedExtensions()
        loadedExtensions.forEach { (name, loadedExtension) ->
            val extensionInfo = LoadedExtensionInfo(
                extension = loadedExtension.extension,
                metadata = loadedExtension.metadata,
                classLoader = loadedExtension.classLoader
            )

            this.loadedExtensions[name] = extensionInfo
            registerExtensionWithRegistry(loadedExtension.extension)
        }
    }

    fun registerExtensionWithRegistry(extension: FlowExtension) {
        val extensionClass = extension::class
        extensionRegistry.registerExtension(extensionClass, extension)
    }

    fun unregisterExtensionFromRegistry(extensionName: String) {
        val extensionInfo = loadedExtensions[extensionName] ?: return
        extensionRegistry.unregisterExtension(extensionName)
    }


    fun dispose() {
        extensionLoader.dispose()
        extensionRegistry.dispose()
        graphExecutor.dispose()
        flowLangIntegration.dispose()
        loadedExtensions.clear()
        scope.cancel()
    }
}


data class LoadedExtensionInfo(
    val extension: FlowExtension,
    val metadata: ExtensionMetadata,
    val classLoader: URLClassLoader?
)