---
title: Extension System Developer Guide
description: Comprehensive guide for creating Flow extensions
---

# Flow Extension Developer Guide

## 🚀 Super Simple Extension Development

Creating Flow extensions is incredibly easy! The framework handles all the complex stuff - you just focus on your logic.

## 📋 Quick Start

### 1. Create Your Extension Class

```kotlin
@FlowExtension(
    name = "MyAwesomeExtension",
    description = "Does awesome things!"
)
class MyExtension : SimpleExtension() {
    
    // That's it! The framework handles everything else
    // Override methods as needed:
    
    override fun onInitialize() {
        log("My extension is initializing!")
    }
    
    override fun onEnable() {
        log("My extension is enabled!")
    }
}
```

### 2. Build and Deploy

```bash
# Compile your extension
kotlinc -cp "path/to/flow-api.jar" -d build/classes MyExtension.kt

# Create JAR
jar cf MyExtension.jar -C build/classes .

# Drop it in the extensions/ folder
cp MyExtension.jar /path/to/flow/extensions/
```

### 3. Hot Reload!

Modify your JAR file and the extension automatically reloads. No restart needed!

## 🎯 Creating Graph Nodes

### TRIGGER Nodes (Start Execution)

TRIGGER nodes can start graph execution. Super simple:

```kotlin
@TriggerNode(
    name = "User Login Trigger",
    category = "Authentication",
    description = "Triggers when a user logs in",
    icon = "login",
    color = "#4CAF50"
)
class UserLoginTrigger : SimpleTriggerNode() {
    
    override suspend fun execute(): TriggerResult {
        // Your trigger logic here
        log("User logged in!")
        
        // Return success to continue execution
        return TriggerResult.Success
        
        // Or return error to stop execution
        // return TriggerResult.Error("Login failed")
        
        // Or skip this trigger
        // return TriggerResult.Skip("User already logged in")
    }
}
```

### ACTION Nodes (Perform Actions)

ACTION nodes perform actions when executed. The framework automatically detects your inputs/outputs using reflection:

```kotlin
@ActionNode(
    name = "Send Email",
    category = "Communication", 
    description = "Sends an email to a user",
    icon = "email",
    color = "#2196F3"
)
class SendEmailAction : SimpleActionNode() {
    
    // Output properties - automatically detected by reflection
    @Output(name = "messageId", type = "text", description = "ID of the sent message")
    val messageId: String = ""
    
    @Output(name = "sentAt", type = "number", description = "Timestamp when email was sent")
    val sentAt: Long = 0L
    
    @Output(name = "success", type = "boolean", description = "Whether the email was sent successfully")
    val success: Boolean = false
    
    override suspend fun execute(
        @Port(name = "recipient", type = "text", description = "Email recipient address")
        recipient: String,
        
        @Port(name = "subject", type = "text", description = "Email subject line")
        subject: String,
        
        @Port(name = "body", type = "text", description = "Email body content")
        body: String,
        
        @Port(name = "priority", type = "text", description = "Email priority", required = false)
        priority: String = "normal",
        
        @Port(name = "attachments", type = "array", description = "List of attachment paths", required = false)
        attachments: List<String> = emptyList()
    ): ActionResult {
        // Your action logic here
        log("Sending email to $recipient: $subject")
        
        // Send the email...
        val result = sendEmail(recipient, subject, body, priority, attachments)
        
        if (result.success) {
            return ActionResult.Success(mapOf(
                "messageId" to result.messageId,
                "sentAt" to result.sentAt,
                "success" to true
            ))
        } else {
            return ActionResult.Error("Failed to send email: ${result.error}")
        }
    }
    
    private fun sendEmail(recipient: String, subject: String, body: String, priority: String, attachments: List<String>): EmailResult {
        // Your email sending logic
        return EmailResult(true, "msg_12345", System.currentTimeMillis(), null)
    }
}

data class EmailResult(val success: Boolean, val messageId: String, val sentAt: Long, val error: String?)
```

**Automatic Port Detection:**
- ✅ **Input ports** detected from method parameters with `@Port` annotations
- ✅ **Output ports** detected from class properties with `@Output` annotations
- ✅ **Type mapping** from Kotlin types to frontend types with color coding
- ✅ **JSON generation** for the graph editor automatically

### Advanced ACTION Node with Port Documentation

For more complex nodes, you can document your ports with detailed annotations:

```kotlin
@ActionNode(
    name = "Math Calculator",
    category = "Math",
    description = "Performs mathematical operations on two numbers"
)
class MathCalculator : SimpleActionNode() {
    
    // Output properties - automatically detected
    @Output(name = "result", type = "number", description = "The calculated result")
    val result: Double = 0.0
    
    @Output(name = "operation", type = "text", description = "The operation that was performed")
    val operation: String = ""
    
    @Output(name = "error", type = "text", description = "Error message if calculation failed")
    val error: String? = null
    
    override suspend fun execute(
        @Port(name = "a", type = "number", description = "First number")
        a: Double,
        
        @Port(name = "b", type = "number", description = "Second number")
        b: Double,
        
        @Port(name = "operation", type = "text", description = "Mathematical operation to perform")
        operation: String = "add",
        
        @Port(name = "precision", type = "number", description = "Decimal precision", required = false)
        precision: Int = 2
    ): ActionResult {
        val result = when (operation.lowercase()) {
            "add" -> a + b
            "subtract" -> a - b
            "multiply" -> a * b
            "divide" -> if (b != 0.0) a / b else return ActionResult.Error("Division by zero")
            "power" -> kotlin.math.pow(a, b)
            "modulo" -> a % b
            else -> return ActionResult.Error("Unknown operation: $operation")
        }
        
        val roundedResult = kotlin.math.round(result * kotlin.math.pow(10.0, precision.toDouble())) / kotlin.math.pow(10.0, precision.toDouble())
        
        return ActionResult.Success(mapOf(
            "result" to roundedResult,
            "operation" to operation,
            "error" to null
        ))
    }
}
```

**Generated JSON for Graph Editor:**
```json
{
  "name": "Math Calculator",
  "inputs": [
    {
      "id": "input_0",
      "name": "a",
      "isInput": true,
      "color": 4280391411,
      "type": "number"
    },
    {
      "id": "input_1", 
      "name": "b",
      "isInput": true,
      "color": 4280391411,
      "type": "number"
    },
    {
      "id": "input_2",
      "name": "operation",
      "isInput": true,
      "color": 4278255360,
      "type": "text"
    },
    {
      "id": "input_3",
      "name": "precision",
      "isInput": true,
      "color": 4280391411,
      "type": "number",
      "required": false
    }
  ],
  "outputs": [
    {
      "id": "output_0",
      "name": "result",
      "isInput": false,
      "color": 4280391411,
      "type": "number"
    },
    {
      "id": "output_1",
      "name": "operation",
      "isInput": false,
      "color": 4278255360,
      "type": "text"
    },
    {
      "id": "output_2",
      "name": "error",
      "isInput": false,
      "color": 4278255360,
      "type": "text"
    }
  ]
}
```

## 🔧 Creating FlowLang Functions

Super simple FlowLang function creation:

```kotlin
@FlowExtension(name = "MyFlowLangExtension")
class MyFlowLangExtension : SimpleFlowFunction() {
    
    @FlowFunction(
        name = "greet_user",
        description = "Greets a user with a custom message",
        category = "Utility"
    )
    fun greetUser(name: String): String {
        return "Hello, $name! Welcome to Flow!"
    }
    
    @FlowFunction(
        name = "calculate_age",
        description = "Calculates age from birth year",
        category = "Math"
    )
    fun calculateAge(birthYear: Int): Int {
        return java.time.LocalDate.now().year - birthYear
    }
    
    @FlowFunction(
        name = "random_color",
        description = "Generates a random color",
        category = "Utility"
    )
    fun randomColor(): String {
        val colors = listOf("red", "blue", "green", "yellow", "purple", "orange")
        return colors.random()
    }
}
```

## 📡 Creating FlowLang Events

Handle events in FlowLang:

```kotlin
@FlowExtension(name = "MyEventExtension")
class MyEventExtension : SimpleFlowEvent() {
    
    @FlowEvent(
        name = "user_registered",
        description = "Triggered when a new user registers",
        category = "User"
    )
    suspend fun onUserRegistered(userData: Map<String, Any?>) {
        val username = userData["username"] as? String ?: "Unknown"
        log("New user registered: $username")
        
        // Send welcome email, create profile, etc.
    }
    
    @FlowEvent(
        name = "file_uploaded",
        description = "Triggered when a file is uploaded",
        category = "File"
    )
    suspend fun onFileUploaded(fileData: Map<String, Any?>) {
        val filename = fileData["filename"] as? String ?: "Unknown"
        val size = fileData["size"] as? Number ?: 0
        log("File uploaded: $filename (${size} bytes)")
        
        // Process file, generate thumbnails, etc.
    }
}
```

## 🏷️ Creating FlowLang Types

Define custom types for FlowLang:

```kotlin
@FlowExtension(name = "MyTypeExtension")
class MyTypeExtension : SimpleFlowType() {
    
    @FlowType(
        name = "Vector2D",
        description = "A 2D vector with x and y components",
        category = "Math"
    )
    class Vector2DType {
        override fun convert(value: Any?): Any? {
            return when (value) {
                is Vector2D -> value
                is Map<*, *> -> {
                    val x = (value["x"] as? Number)?.toDouble() ?: 0.0
                    val y = (value["y"] as? Number)?.toDouble() ?: 0.0
                    Vector2D(x, y)
                }
                is List<*> -> {
                    if (value.size >= 2) {
                        val x = (value[0] as? Number)?.toDouble() ?: 0.0
                        val y = (value[1] as? Number)?.toDouble() ?: 0.0
                        Vector2D(x, y)
                    } else null
                }
                else -> null
            }
        }
        
        override fun validate(value: Any?): Boolean {
            return value is Vector2D
        }
    }
}

data class Vector2D(val x: Double, val y: Double) {
    operator fun plus(other: Vector2D): Vector2D = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D): Vector2D = Vector2D(x - other.x, y - other.y)
    val magnitude: Double get() = kotlin.math.sqrt(x * x + y * y)
}
```

## ⌨️ Creating Terminal Commands

Add terminal commands:

```kotlin
@FlowExtension(name = "MyCommandExtension")
class MyCommandExtension : SimpleExtension() {
    
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
    
    @Command(
        name = "weather",
        description = "Gets weather information",
        usage = "/weather [city]"
    )
    suspend fun weatherCommand(context: CommandContext, args: List<String>): CommandResult {
        val city = args.getOrNull(0) ?: "New York"
        val weather = getWeather(city)
        context.sender.sendMessage("Weather in $city: $weather")
        return CommandResult.Success
    }
    
    private fun getWeather(city: String): String {
        // Your weather API call here
        return "Sunny, 72°F"
    }
}
```

## ⚙️ Configuration

Add configuration to your extensions:

```kotlin
@FlowExtension(name = "MyConfigExtension")
class MyConfigExtension : SimpleExtension() {
    
    @Config(
        key = "api.key",
        defaultValue = "",
        description = "API key for external service"
    )
    private var apiKey: String = ""
    
    @Config(
        key = "max.retries",
        defaultValue = "3",
        description = "Maximum number of retries"
    )
    private var maxRetries: Int = 3
    
    @Config(
        key = "debug.mode",
        defaultValue = "false",
        description = "Enable debug mode"
    )
    private var debugMode: Boolean = false
    
    override fun onInitialize() {
        // Configuration is automatically loaded
        log("API Key: ${if (apiKey.isNotEmpty()) "***" else "Not set"}")
        log("Max Retries: $maxRetries")
        log("Debug Mode: $debugMode")
    }
}
```

## 🔄 Lifecycle Management

The framework automatically manages your extension lifecycle:

```kotlin
@FlowExtension(name = "MyLifecycleExtension")
class MyLifecycleExtension : SimpleExtension() {
    
    override fun onInitialize() {
        // Called when extension is first loaded
        log("Extension initializing...")
        // Initialize resources, load config, etc.
    }
    
    override fun onEnable() {
        // Called when extension is enabled
        log("Extension enabled!")
        // Start services, register listeners, etc.
    }
    
    override fun onDisable() {
        // Called when extension is disabled
        log("Extension disabled!")
        // Stop services, unregister listeners, etc.
    }
    
    override fun onDestroy() {
        // Called when extension is unloaded
        log("Extension destroyed!")
        // Clean up resources, close connections, etc.
    }
}
```

## 🔍 Port Detection & JSON Generation

### How Automatic Port Detection Works

The Flow Extension System uses **reflection-based port detection** to automatically generate JSON for the graph editor:

#### **1. Input Port Detection**
The system analyzes the `execute` method parameters using Kotlin reflection:

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

**The system automatically:**
- Scans the `execute` method parameters
- Extracts `@Port` annotations for customization
- Detects Kotlin types and maps them to frontend types
- Creates `GraphPortDefinition` objects

#### **2. Output Port Detection**
The system looks for class properties with `@Output` annotations:

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

#### **3. Type Detection & Color Mapping**

| Kotlin Type | Detected Type | Color (Hex) | Description |
|-------------|---------------|-------------|-------------|
| `String` | `text` | Green (0xFF4CAF50) | Text input/output |
| `Int`, `Long`, `Double`, `Float` | `number` | Blue (0xFF2196F3) | Numeric values |
| `Boolean` | `boolean` | Orange (0xFFFF9800) | True/false values |
| `Map` | `object` | Purple (0xFF9C27B0) | Complex objects |
| `List` | `array` | Blue Grey (0xFF607D8B) | Arrays/lists |
| Other | `any` | Grey (0xFF757575) | Any type |

#### **4. Generated JSON Structure**

The system converts the detected ports to JSON format for the frontend:

```json
{
  "id": "node_123",
  "name": "Process Data",
  "inputs": [
    {
      "id": "input_0",
      "name": "data",
      "isInput": true,
      "color": 4278255360,
      "type": "text"
    },
    {
      "id": "input_1", 
      "name": "operation",
      "isInput": true,
      "color": 4278255360,
      "type": "text"
    },
    {
      "id": "input_2",
      "name": "caseSensitive",
      "isInput": true,
      "color": 4294967040,
      "type": "boolean",
      "required": false
    }
  ],
  "outputs": [
    {
      "id": "output_0",
      "name": "result",
      "isInput": false,
      "color": 4278255360,
      "type": "text"
    },
    {
      "id": "output_1",
      "name": "length",
      "isInput": false,
      "color": 4280391411,
      "type": "number"
    }
  ],
  "color": 15658734,
  "position": {
    "x": 100.0,
    "y": 200.0
  },
  "templateId": "Process Data"
}
```

#### **5. Frontend Integration**

The generated JSON is consumed by the frontend to:

- **Render Node Templates** - Show available nodes in the palette
- **Create Node Instances** - Instantiate nodes in the graph
- **Handle Connections** - Connect inputs to outputs with type validation
- **Display Ports** - Show input/output ports with correct colors
- **Validate Types** - Ensure type compatibility between connections

## 🎨 Styling Your Nodes

Customize the appearance of your graph nodes:

```kotlin
@TriggerNode(
    name = "Database Query",
    category = "Database",
    description = "Executes a database query",
    icon = "database",           // Icon name (Material Icons)
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

## 📦 Building and Deploying

### 1. Create Your Project Structure

```
MyExtension/
├── src/
│   └── main/
│       └── kotlin/
│           └── MyExtension.kt
├── build.gradle.kts
└── README.md
```

### 2. Build Configuration

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.2.20"
}

dependencies {
    implementation("com.thedevjade.flow:flow-api:1.0.0")
}

kotlin {
    jvmToolchain(23)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "MyExtensionKt",
            "Flow-Extension" to "true"
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
```

### 3. Build and Deploy

```bash
# Build your extension
./gradlew build

# Copy to Flow extensions directory
cp build/libs/MyExtension.jar /path/to/flow/extensions/

# Or use the Flow CLI (if available)
flow extension install MyExtension.jar
```

## 🔥 Hot Reloading

The framework automatically watches for changes to your JAR files:

1. **Modify your code**
2. **Rebuild your JAR**
3. **Replace the JAR in extensions/ folder**
4. **Extension automatically reloads!**

No restart needed!

## 🐛 Debugging

### Enable Debug Logging

```kotlin
@FlowExtension(name = "MyDebugExtension")
class MyDebugExtension : SimpleExtension() {
    
    override fun onInitialize() {
        // Use the built-in logger
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

## 📚 Best Practices

1. **Keep it simple**: The framework handles complexity, focus on your logic
2. **Use meaningful names**: Make your nodes and functions self-documenting
3. **Handle errors gracefully**: Always return appropriate error results
4. **Document your ports**: Use descriptive names for inputs/outputs
5. **Test thoroughly**: Test your extension before deploying
6. **Use configuration**: Make your extension configurable
7. **Clean up resources**: Implement proper cleanup in `onDestroy()`

## 🚀 Advanced Features

### Custom Port Types

```kotlin
@ActionNode(name = "Advanced Node")
class AdvancedNode : SimpleActionNode() {
    
    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        // The framework automatically provides typed inputs
        val user: User? = inputs["user"] as? User
        val settings: Map<String, Any?> = inputs["settings"] as? Map<String, Any?> ?: emptyMap()
        
        // Your logic here
        return ActionResult.Success(mapOf("result" to "success"))
    }
}
```

### Async Operations

```kotlin
@ActionNode(name = "Async Node")
class AsyncNode : SimpleActionNode() {
    
    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        // Use coroutines for async operations
        val result = async {
            // Long-running operation
            performLongOperation()
        }.await()
        
        return ActionResult.Success(mapOf("result" to result))
    }
    
    private suspend fun performLongOperation(): String {
        delay(1000) // Simulate long operation
        return "Operation completed"
    }
}
```

### Event Publishing

```kotlin
@FlowExtension(name = "MyEventExtension")
class MyEventExtension : SimpleExtension() {
    
    fun publishCustomEvent(data: Map<String, Any?>) {
        // Publish events that other extensions can listen to
        context.eventBus.publish(CustomEvent(data))
    }
}

data class CustomEvent(val data: Map<String, Any?>)
```

## 🎉 That's It!

Creating Flow extensions is incredibly simple. The framework handles all the complex stuff - you just focus on your business logic!

- **TRIGGER nodes**: Start graph execution
- **ACTION nodes**: Perform actions with automatic input/output handling
- **FlowLang functions**: Add scripting capabilities
- **FlowLang events**: Handle events
- **FlowLang types**: Define custom types
- **Terminal commands**: Add CLI commands
- **Configuration**: Make extensions configurable
- **Hot reloading**: Automatic reloading on changes

Happy coding! 🚀
