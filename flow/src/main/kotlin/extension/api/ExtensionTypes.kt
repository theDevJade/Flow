package com.thedevjade.flow.extension.api

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import java.util.concurrent.CompletableFuture


interface FlowExtension {
    val name: String
    val version: String
    val description: String
    val author: String
    val dependencies: List<String>

    fun initialize(context: ExtensionContext)
    fun enable()
    fun disable()
    fun destroy()
}


interface ExtensionContext {
    val flowCore: com.thedevjade.flow.api.FlowCore
    val config: ExtensionConfig
    val logger: ExtensionLogger
    val scheduler: ExtensionScheduler
    val eventBus: ExtensionEventBus
    val dependencyInjector: DependencyInjector
}


interface ExtensionConfig {
    fun getString(key: String, defaultValue: String = ""): String
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun getDouble(key: String, defaultValue: Double = 0.0): Double
    fun getList(key: String, defaultValue: List<String> = emptyList()): List<String>
    fun set(key: String, value: Any)
}


interface ExtensionLogger {
    fun debug(message: String, vararg args: Any?)
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
    fun error(message: String, throwable: Throwable, vararg args: Any?)
}


interface ExtensionScheduler {
    fun scheduleSync(task: () -> Unit)
    fun scheduleAsync(task: suspend () -> Unit): Job
    fun scheduleDelayed(delayMs: Long, task: () -> Unit)
    fun scheduleRepeating(intervalMs: Long, task: () -> Unit): Job
}


interface ExtensionEventBus {
    fun <T : Any> subscribe(eventType: Class<T>, handler: (T) -> Unit)
    fun <T : Any> unsubscribe(eventType: Class<T>, handler: (T) -> Unit)
    fun <T : Any> publish(event: T)
}


interface DependencyInjector {
    fun <T : Any> getInstance(type: Class<T>): T?
    fun <T : Any> getInstance(type: Class<T>, name: String): T?
    fun <T : Any> registerInstance(type: Class<T>, instance: T)
    fun <T : Any> registerInstance(type: Class<T>, name: String, instance: T)
}


interface TerminalCommandHandler {
    val name: String
    val description: String
    val usage: String
    val aliases: List<String>
    val permission: String
    val async: Boolean

    suspend fun execute(context: CommandContext, args: List<String>): CommandResult
}


interface CommandContext {
    val sender: CommandSender
    val arguments: List<String>
    val flags: Map<String, String>
    val workingDirectory: String
}


interface CommandSender {
    val name: String
    val id: String
    val permissions: Set<String>

    fun sendMessage(message: String)
    fun sendError(message: String)
    fun hasPermission(permission: String): Boolean
}


sealed class CommandResult {
    object Success : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class Usage(val message: String) : CommandResult()
}


interface GraphNodeHandler {
    val name: String
    val category: String
    val description: String
    val icon: String
    val color: String
    val inputs: List<GraphPortDefinition>
    val outputs: List<GraphPortDefinition>

    suspend fun execute(context: GraphNodeContext): GraphNodeResult
}


data class GraphPortDefinition(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean,
    val defaultValue: String
)


interface GraphNodeContext {
    val nodeId: String
    val inputs: Map<String, Any?>
    val outputs: MutableMap<String, Any?>
    val properties: Map<String, Any?>
    val graphId: String
    val executionId: String
}


sealed class GraphNodeResult {
    object Success : GraphNodeResult()
    data class Error(val message: String) : GraphNodeResult()
    data class Skip(val reason: String) : GraphNodeResult()
}


interface FlowLangFunctionHandler {
    val name: String
    val description: String
    val category: String
    val async: Boolean
    val parameters: List<FlowLangParameterDefinition>

    suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any?
}


data class FlowLangParameterDefinition(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean,
    val defaultValue: String
)


interface FlowLangContext {
    val variables: MutableMap<String, Any?>
    val functions: MutableMap<String, FlowLangFunctionHandler>
    val types: MutableMap<String, Class<*>>
    val executionId: String
}


interface FlowLangEventHandler {
    val name: String
    val description: String
    val category: String

    suspend fun handle(context: FlowLangContext, event: Any)
}


interface FlowLangSyntaxHandler {
    val name: String
    val pattern: String
    val description: String
    val priority: Int

    fun parse(input: String): FlowLangSyntaxResult?
}


data class FlowLangSyntaxResult(
    val success: Boolean,
    val value: Any? = null,
    val remainingInput: String = "",
    val error: String? = null
)


interface FlowLangTypeHandler {
    val name: String
    val description: String
    val category: String
    val javaType: Class<*>

    fun convert(value: Any?): Any?
    fun validate(value: Any?): Boolean
}


data class ExtensionMetadata(
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val dependencies: List<String>,
    val mainClass: String,
    val jarPath: String,
    val loadTime: Long,
    val lastModified: Long
)


sealed class ExtensionLoadResult {
    data class Success(val extension: FlowExtension, val metadata: ExtensionMetadata) : ExtensionLoadResult()
    data class Failure(val error: String, val cause: Throwable? = null) : ExtensionLoadResult()
}


sealed class ExtensionReloadResult {
    data class Success(val extension: FlowExtension, val metadata: ExtensionMetadata) : ExtensionReloadResult()
    data class Failure(val error: String, val cause: Throwable? = null) : ExtensionReloadResult()
    data class NoChanges(val message: String) : ExtensionReloadResult()
}
