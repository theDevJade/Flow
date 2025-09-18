---
title: Extension System API Reference
description: Complete API Documentation for Flow Extension System
---

# Flow Extension System API Reference

## 📚 Complete API Documentation

This document provides a comprehensive reference for all classes, interfaces, and annotations in the Flow Extension System.

---

## 🏗️ Core Architecture

### ExtensionManager

The central coordinator for all extension functionality.

```kotlin
class ExtensionManager(
    private val flowCore: FlowCore,
    private val extensionsDirectory: File = File("extensions")
)
```

**Key Methods:**
- `suspend fun initialize()` - Initialize the extension manager
- `suspend fun loadExtension(jarFile: File): ExtensionLoadResult` - Load an extension from JAR
- `suspend fun reloadExtension(extensionName: String): ExtensionReloadResult` - Reload an extension
- `suspend fun unloadExtension(extensionName: String): Boolean` - Unload an extension
- `suspend fun executeGraph(graph: FlowGraph, inputs: Map<String, Any?>): GraphExecutionResult` - Execute a graph
- `fun getLoadedExtensions(): Map<String, LoadedExtensionInfo>` - Get all loaded extensions
- `fun isExtensionLoaded(name: String): Boolean` - Check if extension is loaded
- `fun dispose()` - Clean up resources

---

## 🎯 Graph Node System

### 🔍 Automatic Port Detection

The Flow Extension System uses **reflection-based port detection** to automatically generate JSON for the graph editor:

#### **Type Detection & Color Mapping**
| Kotlin Type | Detected Type | Color (Hex) | Description |
|-------------|---------------|-------------|-------------|
| `String` | `text` | Green (0xFF4CAF50) | Text input/output |
| `Int`, `Long`, `Double`, `Float` | `number` | Blue (0xFF2196F3) | Numeric values |
| `Boolean` | `boolean` | Orange (0xFFFF9800) | True/false values |
| `Map` | `object` | Purple (0xFF9C27B0) | Complex objects |
| `List` | `array` | Blue Grey (0xFF607D8B) | Arrays/lists |
| Other | `any` | Grey (0xFF757575) | Any type |

#### **Port Detection Process**
1. **Input Ports**: Detected from `execute` method parameters with `@Port` annotations
2. **Output Ports**: Detected from class properties with `@Output` annotations
3. **Type Mapping**: Automatic conversion from Kotlin types to frontend types
4. **JSON Generation**: Automatic generation of graph editor JSON

### TRIGGER Nodes

TRIGGER nodes can start graph execution.

#### TriggerNode Annotation

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TriggerNode(
    val name: String,
    val category: String = "Triggers",
    val description: String = "",
    val icon: String = "play_circle",
    val color: String = "#4CAF50"
)
```

#### SimpleTriggerNode Base Class

```kotlin
abstract class SimpleTriggerNode : SimpleExtension() {
    abstract suspend fun execute(): TriggerResult
}
```

#### TriggerResult Types

```kotlin
sealed class TriggerResult {
    object Success : TriggerResult()
    data class Error(val message: String) : TriggerResult()
    data class Skip(val reason: String) : TriggerResult()
}
```

### ACTION Nodes

ACTION nodes perform actions when executed.

#### ActionNode Annotation

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ActionNode(
    val name: String,
    val category: String = "Actions",
    val description: String = "",
    val icon: String = "settings",
    val color: String = "#2196F3"
)
```

#### SimpleActionNode Base Class

```kotlin
abstract class SimpleActionNode : SimpleExtension() {
    abstract suspend fun execute(inputs: Map<String, Any?>): ActionResult
}
```

#### ActionResult Types

```kotlin
sealed class ActionResult {
    data class Success(val outputs: Map<String, Any?>) : ActionResult()
    data class Error(val message: String) : ActionResult()
    data class Skip(val reason: String) : ActionResult()
}
```

---

## 🔧 FlowLang Integration

### FlowFunction Annotation

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowFunction(
    val name: String = "",
    val description: String = "",
    val category: String = "General"
)
```

### FlowEvent Annotation

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowEvent(
    val name: String,
    val description: String = "",
    val category: String = "Events"
)
```

### FlowType Annotation

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowType(
    val name: String,
    val description: String = "",
    val category: String = "Types"
)
```

### Base Classes

```kotlin
abstract class SimpleFlowFunction : SimpleExtension()
abstract class SimpleFlowEvent : SimpleExtension()
abstract class SimpleFlowType : SimpleExtension() {
    abstract fun convert(value: Any?): Any?
    abstract fun validate(value: Any?): Boolean
}
```

---

## ⌨️ Terminal Commands

### Command Annotation

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(
    val name: String,
    val description: String = "",
    val usage: String = "",
    val aliases: Array<String> = []
)
```

### CommandContext Interface

```kotlin
interface CommandContext {
    val sender: CommandSender
    val args: List<String>
    val extension: FlowExtension
}
```

### CommandResult Types

```kotlin
sealed class CommandResult {
    object Success : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class Usage(val usage: String) : CommandResult()
}
```

---

## 🏷️ Core Annotations

### FlowExtension Annotation

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FlowExtension(
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val author: String = ""
)
```

### Port Annotation

Used to define input ports for ACTION nodes. The system automatically detects these from method parameters.

```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Port(
    val name: String = "",
    val description: String = "",
    val type: String = "any",
    val required: Boolean = true,
    val defaultValue: String = ""
)
```

**Usage Example:**
```kotlin
override suspend fun execute(
    @Port(name = "data", type = "text", description = "Input data to process")
    data: String,
    
    @Port(name = "operation", type = "text", description = "Operation to perform")
    operation: String,
    
    @Port(name = "caseSensitive", type = "boolean", required = false)
    caseSensitive: Boolean = false
): ActionResult {
    // Your logic here
}
```

### Output Annotation

Used to define output ports for ACTION nodes. The system automatically detects these from class properties.

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Output(
    val name: String = "",
    val description: String = "",
    val type: String = "any"
)
```

**Usage Example:**
```kotlin
@ActionNode(name = "Process Data")
class ProcessDataAction : SimpleActionNode() {
    
    @Output(name = "result", type = "text", description = "Processed result")
    val result: String = ""
    
    @Output(name = "length", type = "number", description = "Length of processed data")
    val length: Int = 0
    
    // ... execute method
}
```

### Config Annotation

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Config(
    val key: String,
    val defaultValue: String = "",
    val description: String = ""
)
```

---

## 🏗️ Base Classes

### SimpleExtension

The main base class for all extensions.

```kotlin
abstract class SimpleExtension : FlowExtension {
    protected lateinit var context: ExtensionContext
    protected val scope: CoroutineScope
    
    // Lifecycle methods
    protected open fun onInitialize() {}
    protected open fun onEnable() {}
    protected open fun onDisable() {}
    protected open fun onDestroy() {}
    
    // Helper methods
    protected fun log(message: String)
    protected fun logError(message: String, throwable: Throwable? = null)
    protected fun schedule(task: suspend () -> Unit)
    protected fun config(key: String, defaultValue: String = ""): String
    protected fun configInt(key: String, defaultValue: Int = 0): Int
    protected fun configBoolean(key: String, defaultValue: Boolean = false): Boolean
}
```

---

## 🔧 Core Interfaces

### FlowExtension Interface

```kotlin
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
```

### ExtensionContext Interface

```kotlin
interface ExtensionContext {
    val flowCore: FlowCore
    val config: ExtensionConfig
    val logger: ExtensionLogger
    val scheduler: ExtensionScheduler
    val eventBus: ExtensionEventBus
    val dependencyInjector: DependencyInjector
}
```

### ExtensionConfig Interface

```kotlin
interface ExtensionConfig {
    fun getString(key: String, defaultValue: String = ""): String
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun getDouble(key: String, defaultValue: Double = 0.0): Double
    fun getList(key: String, defaultValue: List<String> = emptyList()): List<String>
    fun set(key: String, value: Any)
}
```

### ExtensionLogger Interface

```kotlin
interface ExtensionLogger {
    fun debug(message: String, vararg args: Any?)
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
    fun error(message: String, throwable: Throwable, vararg args: Any?)
}
```

### ExtensionScheduler Interface

```kotlin
interface ExtensionScheduler {
    fun scheduleSync(task: () -> Unit)
    fun scheduleAsync(task: suspend () -> Unit): Job
    fun scheduleDelayed(delayMs: Long, task: () -> Unit)
    fun scheduleRepeating(intervalMs: Long, task: () -> Unit): Job
}
```

### ExtensionEventBus Interface

```kotlin
interface ExtensionEventBus {
    fun <T : Any> subscribe(eventType: Class<T>, handler: (T) -> Unit)
    fun <T : Any> unsubscribe(eventType: Class<T>, handler: (T) -> Unit)
    fun <T : Any> publish(event: T)
}
```

### DependencyInjector Interface

```kotlin
interface DependencyInjector {
    fun <T : Any> getInstance(type: Class<T>): T?
    fun <T : Any> getInstance(type: Class<T>, name: String): T?
    fun <T : Any> registerInstance(type: Class<T>, instance: T)
    fun <T : Any> registerInstance(type: Class<T>, name: String, instance: T)
}
```

---

## 🎯 Graph Execution

### GraphExecutor

```kotlin
class GraphExecutor(
    private val extensionRegistry: SimpleExtensionRegistry
) {
    suspend fun executeGraph(
        graph: FlowGraph,
        inputs: Map<String, Any?> = emptyMap(),
        context: GraphExecutionContext? = null
    ): GraphExecutionResult
    
    suspend fun executeNode(
        graph: FlowGraph,
        nodeId: String,
        inputs: Map<String, Any?> = emptyMap(),
        context: GraphExecutionContext? = null
    ): NodeExecutionResult
    
    fun getActiveExecutions(): Map<String, GraphExecution>
    fun cancelExecution(executionId: String): Boolean
    fun dispose()
}
```

### GraphExecutionResult Types

```kotlin
sealed class GraphExecutionResult {
    data class Success(val outputs: Map<String, Any?>) : GraphExecutionResult()
    data class Failure(val error: String) : GraphExecutionResult()
    data class Cancelled(val reason: String) : GraphExecutionResult()
}
```

### NodeExecutionResult Types

```kotlin
sealed class NodeExecutionResult {
    data class Success(val nodeId: String, val outputs: Map<String, Any?>) : NodeExecutionResult()
    data class Failure(val error: String) : NodeExecutionResult()
    data class Skipped(val reason: String) : NodeExecutionResult()
}
```

---

## 🔥 Hot Reloading

### HotReloadExtensionLoader

```kotlin
class HotReloadExtensionLoader(
    private val flowCore: FlowCore,
    private val extensionsDirectory: File = File("extensions"),
    private val watchInterval: Long = 1000L
) {
    fun startWatching()
    fun stopWatching()
    suspend fun loadExtension(jarFile: File): ExtensionLoadResult
    suspend fun reloadExtension(extensionName: String): ExtensionReloadResult
    suspend fun unloadExtension(extensionName: String): Boolean
    fun getLoadedExtensions(): Map<String, LoadedExtension>
    fun getLoadedExtension(name: String): LoadedExtension?
    fun isExtensionLoaded(name: String): Boolean
    fun dispose()
}
```

### ExtensionLoadResult Types

```kotlin
sealed class ExtensionLoadResult {
    data class Success(val extension: FlowExtension, val metadata: ExtensionMetadata) : ExtensionLoadResult()
    data class Failure(val error: String, val cause: Throwable? = null) : ExtensionLoadResult()
}
```

### ExtensionReloadResult Types

```kotlin
sealed class ExtensionReloadResult {
    data class Success(val extension: FlowExtension, val metadata: ExtensionMetadata) : ExtensionReloadResult()
    data class Failure(val error: String, val cause: Throwable? = null) : ExtensionReloadResult()
    data class NoChanges(val message: String) : ExtensionReloadResult()
}
```

---

## 📦 Data Classes

### ExtensionMetadata

```kotlin
data class ExtensionMetadata(
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val mainClass: String,
    val dependencies: List<String>,
    val jarPath: String,
    val loadTime: Long,
    val lastModified: Long
)
```

### LoadedExtensionInfo

```kotlin
data class LoadedExtensionInfo(
    val extension: FlowExtension,
    val metadata: ExtensionMetadata,
    val classLoader: URLClassLoader?
)
```

### GraphPortDefinition

```kotlin
data class GraphPortDefinition(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean,
    val defaultValue: String
)
```

---

## 🎨 Node Types

### NodeType Enum

```kotlin
enum class NodeType {
    TRIGGER,  // Can be triggered to start execution
    ACTION    // Performs an action when executed
}
```

---

## 🔧 Registry System

### SimpleExtensionRegistry

```kotlin
class SimpleExtensionRegistry(
    private val flowCore: FlowCore
) {
    fun registerExtension(extensionClass: KClass<*>, instance: Any)
    
    // Getters for registered handlers
    fun getTriggerNode(name: String): TriggerNodeHandler?
    fun getActionNode(name: String): ActionNodeHandler?
    fun getFlowLangFunction(name: String): FlowLangFunctionHandler?
    fun getFlowLangEvent(name: String): FlowLangEventHandler?
    fun getFlowLangType(name: String): FlowLangTypeHandler?
    fun getTerminalCommand(name: String): TerminalCommandHandler?
    
    // Bulk getters
    fun getAllTriggerNodes(): Map<String, TriggerNodeHandler>
    fun getAllActionNodes(): Map<String, ActionNodeHandler>
    fun getAllFlowLangFunctions(): Map<String, FlowLangFunctionHandler>
    fun getAllFlowLangEvents(): Map<String, FlowLangEventHandler>
    fun getAllFlowLangTypes(): Map<String, FlowLangTypeHandler>
    fun getAllTerminalCommands(): Map<String, TerminalCommandHandler>
    
    fun dispose()
}
```

---

## 🎯 Handler Interfaces

### TriggerNodeHandler

```kotlin
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
```

### ActionNodeHandler

```kotlin
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
```

### FlowLangFunctionHandler

```kotlin
interface FlowLangFunctionHandler {
    val name: String
    val description: String
    val category: String
    val async: Boolean
    val parameters: List<FlowLangParameterDefinition>
    
    suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any?
}
```

### FlowLangEventHandler

```kotlin
interface FlowLangEventHandler {
    val name: String
    val description: String
    val category: String
    
    suspend fun handle(context: FlowLangContext, event: Any)
}
```

### FlowLangTypeHandler

```kotlin
interface FlowLangTypeHandler {
    val name: String
    val description: String
    val category: String
    val javaType: Class<*>
    
    fun convert(value: Any?): Any?
    fun validate(value: Any?): Boolean
}
```

### TerminalCommandHandler

```kotlin
interface TerminalCommandHandler {
    val name: String
    val description: String
    val usage: String
    val aliases: List<String>
    val permission: String
    val async: Boolean
    
    suspend fun execute(context: CommandContext, args: List<String>): CommandResult
}
```

---

## 🎉 Usage Examples

### Complete Extension Example

```kotlin
@FlowExtension(
    name = "MyAwesomeExtension",
    version = "1.0.0",
    description = "Does awesome things!",
    author = "Your Name"
)
class MyAwesomeExtension : SimpleExtension() {
    
    // TRIGGER Node
    @TriggerNode(
        name = "User Login Trigger",
        category = "Authentication",
        description = "Triggers when a user logs in",
        icon = "login",
        color = "#4CAF50"
    )
    class UserLoginTrigger : SimpleTriggerNode() {
        override suspend fun execute(): TriggerResult {
            log("User logged in!")
            return TriggerResult.Success
        }
    }
    
    // ACTION Node
    @ActionNode(
        name = "Send Email",
        category = "Communication",
        description = "Sends an email to a user",
        icon = "email",
        color = "#2196F3"
    )
    class SendEmailAction : SimpleActionNode() {
        override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
            val recipient = inputs["recipient"] as? String ?: return ActionResult.Error("No recipient")
            val subject = inputs["subject"] as? String ?: return ActionResult.Error("No subject")
            val body = inputs["body"] as? String ?: return ActionResult.Error("No body")
            
            // Send email logic...
            log("Sending email to $recipient: $subject")
            
            return ActionResult.Success(mapOf(
                "messageId" to "msg_12345",
                "sentAt" to System.currentTimeMillis()
            ))
        }
    }
    
    // FlowLang Function
    @FlowFunction(
        name = "greet_user",
        description = "Greets a user with a custom message",
        category = "Utility"
    )
    fun greetUser(name: String): String {
        return "Hello, $name! Welcome to Flow!"
    }
    
    // FlowLang Event
    @FlowEvent(
        name = "user_registered",
        description = "Triggered when a new user registers",
        category = "User"
    )
    suspend fun onUserRegistered(userData: Map<String, Any?>) {
        val username = userData["username"] as? String ?: "Unknown"
        log("New user registered: $username")
    }
    
    // Terminal Command
    @Command(
        name = "hello",
        description = "Says hello to the user",
        usage = "/hello [name]",
        aliases = ["hi", "greet"]
    )
    suspend fun helloCommand(context: CommandContext, args: List<String>): CommandResult {
        val name = args.getOrNull(0) ?: "World"
        context.sender.sendMessage("Hello, $name!")
        return CommandResult.Success
    }
}
```

---

## 🚀 That's It!

This API reference covers all the classes, interfaces, and annotations you need to create powerful Flow extensions. The system is designed to be simple yet powerful - just annotate your classes and the framework handles everything else!

For more detailed examples and tutorials, see the [Developer Guide](extension-system-developer-guide).
