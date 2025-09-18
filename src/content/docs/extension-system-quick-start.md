---
title: Extension System Quick Start
description: Get up and running with Flow extensions in minutes!
---

# 🚀 Flow Extension Quick Start Guide

Get up and running with Flow extensions in minutes!

## 📋 Prerequisites

- Kotlin 2.2+ 
- Java 17+
- Flow API JAR file

## ⚡ 5-Minute Setup

### 1. Create Your First Extension

Create a new Kotlin file `MyFirstExtension.kt`:

```kotlin
@FlowExtension(
    name = "MyFirstExtension",
    description = "My very first Flow extension!"
)
class MyFirstExtension : SimpleExtension() {
    
    override fun onInitialize() {
        log("Hello from my first extension!")
    }
}
```

### 2. Add a TRIGGER Node

```kotlin
@TriggerNode(
    name = "Hello Trigger",
    category = "Examples",
    description = "Says hello when triggered"
)
class HelloTrigger : SimpleTriggerNode() {
    
    override suspend fun execute(): TriggerResult {
        log("Hello from a TRIGGER node!")
        return TriggerResult.Success
    }
}
```

### 3. Add an ACTION Node

```kotlin
@ActionNode(
    name = "Echo Action",
    category = "Examples", 
    description = "Echoes back the input message"
)
class EchoAction : SimpleActionNode() {
    
    // Output properties - automatically detected
    @Output(name = "echoedMessage", type = "text", description = "The echoed message")
    val echoedMessage: String = ""
    
    @Output(name = "length", type = "number", description = "Length of the echoed message")
    val length: Int = 0
    
    override suspend fun execute(
        @Port(name = "message", type = "text", description = "Message to echo")
        message: String,
        
        @Port(name = "prefix", type = "text", description = "Prefix to add", required = false)
        prefix: String = "Echo: "
    ): ActionResult {
        log("Echoing: $message")
        
        val echoed = "$prefix$message"
        
        return ActionResult.Success(mapOf(
            "echoedMessage" to echoed,
            "length" to echoed.length
        ))
    }
}
```

**Automatic Port Detection:**
- ✅ **Input ports** detected from method parameters with `@Port` annotations
- ✅ **Output ports** detected from class properties with `@Output` annotations
- ✅ **Type mapping** from Kotlin types to frontend types with color coding
- ✅ **JSON generation** for the graph editor automatically

### 4. Add a FlowLang Function

```kotlin
@FlowExtension(name = "MyFlowLangExtension")
class MyFlowLangExtension : SimpleFlowFunction() {
    
    @FlowFunction(
        name = "add_numbers",
        description = "Adds two numbers together"
    )
    fun addNumbers(a: Int, b: Int): Int {
        return a + b
    }
}
```

### 5. Build and Deploy

```bash
# Compile your extension
kotlinc -cp "path/to/flow-api.jar" -d build/classes MyFirstExtension.kt

# Create JAR
jar cf MyFirstExtension.jar -C build/classes .

# Drop it in the extensions/ folder
cp MyFirstExtension.jar /path/to/flow/extensions/
```

### 6. Hot Reload!

Modify your code, rebuild the JAR, and replace it in the extensions folder. The extension automatically reloads!

## 🎯 Complete Example

Here's a complete extension that demonstrates all the main features:

```kotlin
@FlowExtension(
    name = "CompleteExample",
    version = "1.0.0",
    description = "A complete example extension",
    author = "Your Name"
)
class CompleteExample : SimpleExtension() {
    
    // Configuration
    @Config(
        key = "greeting.message",
        defaultValue = "Hello",
        description = "The greeting message to use"
    )
    private var greetingMessage: String = ""
    
    override fun onInitialize() {
        log("Complete example extension initializing...")
        log("Greeting message: $greetingMessage")
    }
    
    override fun onEnable() {
        log("Complete example extension enabled!")
    }
    
    override fun onDisable() {
        log("Complete example extension disabled!")
    }
    
    override fun onDestroy() {
        log("Complete example extension destroyed!")
    }
}

// TRIGGER Node
@TriggerNode(
    name = "Timer Trigger",
    category = "Timers",
    description = "Triggers every 5 seconds",
    icon = "timer",
    color = "#FF9800"
)
class TimerTrigger : SimpleTriggerNode() {
    
    override suspend fun execute(): TriggerResult {
        log("Timer triggered at ${System.currentTimeMillis()}")
        return TriggerResult.Success
    }
}

// ACTION Node
@ActionNode(
    name = "Process Data",
    category = "Data",
    description = "Processes input data and returns results",
    icon = "data_usage",
    color = "#9C27B0"
)
class ProcessDataAction : SimpleActionNode() {
    
    // Output properties - automatically detected
    @Output(name = "result", type = "text", description = "Processed result")
    val result: String = ""
    
    @Output(name = "originalLength", type = "number", description = "Length of original data")
    val originalLength: Int = 0
    
    @Output(name = "processedAt", type = "number", description = "Timestamp when processed")
    val processedAt: Long = 0L
    
    override suspend fun execute(
        @Port(name = "data", type = "text", description = "Input data to process")
        data: String,
        
        @Port(name = "operation", type = "text", description = "Operation to perform")
        operation: String = "uppercase",
        
        @Port(name = "caseSensitive", type = "boolean", description = "Whether to be case sensitive", required = false)
        caseSensitive: Boolean = true
    ): ActionResult {
        val result = when (operation.lowercase()) {
            "uppercase" -> data.uppercase()
            "lowercase" -> data.lowercase()
            "reverse" -> data.reversed()
            "length" -> data.length.toString()
            else -> return ActionResult.Error("Unknown operation: $operation")
        }
        
        return ActionResult.Success(mapOf(
            "result" to result,
            "originalLength" to data.length,
            "processedAt" to System.currentTimeMillis()
        ))
    }
}

// FlowLang Functions
@FlowExtension(name = "MathFunctions")
class MathFunctions : SimpleFlowFunction() {
    
    @FlowFunction(
        name = "multiply",
        description = "Multiplies two numbers",
        category = "Math"
    )
    fun multiply(a: Double, b: Double): Double {
        return a * b
    }
    
    @FlowFunction(
        name = "is_even",
        description = "Checks if a number is even",
        category = "Math"
    )
    fun isEven(number: Int): Boolean {
        return number % 2 == 0
    }
    
    @FlowFunction(
        name = "random_string",
        description = "Generates a random string of specified length",
        category = "Utility"
    )
    fun randomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}

// FlowLang Events
@FlowExtension(name = "EventHandlers")
class EventHandlers : SimpleFlowEvent() {
    
    @FlowEvent(
        name = "data_processed",
        description = "Triggered when data is processed",
        category = "Data"
    )
    suspend fun onDataProcessed(eventData: Map<String, Any?>) {
        val result = eventData["result"] as? String ?: "Unknown"
        val timestamp = eventData["processedAt"] as? Long ?: 0L
        log("Data processed: $result at $timestamp")
    }
    
    @FlowEvent(
        name = "error_occurred",
        description = "Triggered when an error occurs",
        category = "Error"
    )
    suspend fun onErrorOccurred(errorData: Map<String, Any?>) {
        val error = errorData["error"] as? String ?: "Unknown error"
        val source = errorData["source"] as? String ?: "Unknown source"
        log("Error occurred in $source: $error")
    }
}

// Terminal Commands
@FlowExtension(name = "Commands")
class Commands : SimpleExtension() {
    
    @Command(
        name = "status",
        description = "Shows the status of the extension system",
        usage = "/status [extension]"
    )
    suspend fun statusCommand(context: CommandContext, args: List<String>): CommandResult {
        val extensionName = args.getOrNull(0)
        
        if (extensionName != null) {
            // Show status of specific extension
            context.sender.sendMessage("Status of $extensionName: Active")
        } else {
            // Show status of all extensions
            context.sender.sendMessage("Extension system status: Running")
            context.sender.sendMessage("Loaded extensions: 3")
        }
        
        return CommandResult.Success
    }
    
    @Command(
        name = "reload",
        description = "Reloads an extension",
        usage = "/reload <extension>",
        aliases = ["r"]
    )
    suspend fun reloadCommand(context: CommandContext, args: List<String>): CommandResult {
        val extensionName = args.getOrNull(0) ?: return CommandResult.Usage("Usage: /reload <extension>")
        
        // Reload logic would go here
        context.sender.sendMessage("Reloading extension: $extensionName")
        
        return CommandResult.Success
    }
}
```

## 🔧 Building with Gradle

Create a `build.gradle.kts` file:

```kotlin
plugins {
    kotlin("jvm") version "2.2.20"
}

dependencies {
    implementation("com.thedevjade.flow:flow-api:1.0.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "YourExtensionKt",
            "Flow-Extension" to "true"
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
```

Build with:
```bash
./gradlew build
```

## 🎨 Styling Your Nodes

Customize the appearance of your graph nodes:

```kotlin
@TriggerNode(
    name = "Database Query",
    category = "Database",
    description = "Executes a database query",
    icon = "database",           // Material Icons name
    color = "#FF5722"            // Hex color code
)
class DatabaseQueryTrigger : SimpleTriggerNode() {
    // Your implementation
}

@ActionNode(
    name = "Send Notification",
    category = "Communication",
    description = "Sends a push notification",
    icon = "notifications",
    color = "#9C27B0"
)
class SendNotificationAction : SimpleActionNode() {
    // Your implementation
}
```

## 🔥 Hot Reloading

The framework automatically watches for changes to your JAR files:

1. **Modify your code**
2. **Rebuild your JAR**: `./gradlew build`
3. **Replace the JAR** in `extensions/` folder
4. **Extension automatically reloads!**

No restart needed!

## 🐛 Debugging

### Enable Debug Logging

```kotlin
@FlowExtension(name = "MyDebugExtension")
class MyDebugExtension : SimpleExtension() {
    
    override fun onInitialize() {
        log("Debug: Extension initializing")
        logError("Error: Something went wrong", exception)
    }
}
```

### Common Issues

1. **Extension not loading**: Check that your JAR is in the `extensions/` folder
2. **Node not appearing**: Ensure your class has the correct annotation
3. **Function not working**: Check that your method has the `@FlowFunction` annotation
4. **Hot reload not working**: Make sure you're replacing the entire JAR file

## 📚 Next Steps

- 📖 [**Complete Developer Guide**](extension-system-developer-guide) - Detailed documentation
- 📚 [**API Reference**](extension-system-api-reference) - Complete API documentation
- 🎯 [**Extension System Introduction**](extension-system-introduction) - System overview

## 🎉 That's It!

You now have a working Flow extension! The framework handles all the complex stuff - you just focus on your business logic.

**Key Points:**
- ✅ Just annotate your classes
- ✅ Framework handles everything else
- ✅ Hot reloading works automatically
- ✅ Type-safe throughout
- ✅ Super simple to use

Happy coding! 🚀
