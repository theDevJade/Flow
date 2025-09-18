---
title: Flow Extension System
description: Super Simple Extension Development for Flow
---

# Flow Extension System

## 🚀 Super Simple Extension Development for Flow

The Flow Extension System makes it incredibly easy to create powerful extensions. Just annotate your classes and the framework handles everything else!

## ✨ Key Features

- 🎯 **TRIGGER Nodes**: Start graph execution with simple annotations
- ⚡ **ACTION Nodes**: Perform actions with automatic input/output detection  
- 🔥 **Hot Reloading**: Automatic reloading when you modify JAR files
- 🎨 **FlowLang Integration**: Add functions, events, and types to FlowLang
- ⌨️ **Terminal Commands**: Add CLI commands with simple annotations
- 🛡️ **Type Safety**: Full Kotlin type safety throughout
- 📦 **JAR Support**: Load extensions from JAR files
- 🔧 **Auto-Discovery**: No manual registration required

## 🎯 Graph Nodes - The Main Focus

### 🔍 Automatic Port Detection & JSON Generation

The Flow Extension System uses **reflection-based port detection** to automatically generate JSON for the graph editor:

#### **Type Detection & Color Mapping**
| Kotlin Type | Frontend Type | Color (Hex) | Description |
|-------------|---------------|-------------|-------------|
| `String` | `text` | Green (0xFF4CAF50) | Text input/output |
| `Int`, `Long`, `Double`, `Float` | `number` | Blue (0xFF2196F3) | Numeric values |
| `Boolean` | `boolean` | Orange (0xFFFF9800) | True/false values |
| `Map` | `object` | Purple (0xFF9C27B0) | Complex objects |
| `List` | `array` | Blue Grey (0xFF607D8B) | Arrays/lists |
| Other | `any` | Grey (0xFF757575) | Any type |

#### **Generated JSON Example**
```json
{
  "id": "node_123",
  "name": "Send Email",
  "inputs": [
    {
      "id": "input_0",
      "name": "recipient",
      "isInput": true,
      "color": 4278255360
    },
    {
      "id": "input_1",
      "name": "subject", 
      "isInput": true,
      "color": 4278255360
    }
  ],
  "outputs": [
    {
      "id": "output_0",
      "name": "messageId",
      "isInput": false,
      "color": 4278255360
    }
  ],
  "color": 15658734,
  "templateId": "Send Email"
}
```

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
        return TriggerResult.Success
    }
}
```

### ACTION Nodes (Perform Actions)

ACTION nodes perform actions when executed. The framework automatically detects inputs/outputs using reflection:

```kotlin
@ActionNode(
    name = "Send Email",
    category = "Communication", 
    description = "Sends an email to a user",
    icon = "email",
    color = "#2196F3"
)
class SendEmailAction : SimpleActionNode() {
    
    // Output properties - automatically detected
    @Output(name = "messageId", type = "text", description = "ID of the sent message")
    val messageId: String = ""
    
    @Output(name = "sentAt", type = "number", description = "Timestamp when email was sent")
    val sentAt: Long = 0L
    
    override suspend fun execute(
        @Port(name = "recipient", type = "text", description = "Email recipient address")
        recipient: String,
        
        @Port(name = "subject", type = "text", description = "Email subject line")
        subject: String,
        
        @Port(name = "body", type = "text", description = "Email body content")
        body: String,
        
        @Port(name = "priority", type = "text", description = "Email priority", required = false)
        priority: String = "normal"
    ): ActionResult {
        // Your action logic here
        log("Sending email to $recipient: $subject")
        
        // Send the email...
        val success = sendEmail(recipient, subject, body, priority)
        
        if (success) {
            return ActionResult.Success(mapOf(
                "messageId" to "msg_12345",
                "sentAt" to System.currentTimeMillis()
            ))
        } else {
            return ActionResult.Error("Failed to send email")
        }
    }
}
```

**Automatic Port Detection:**
- ✅ **Input ports** detected from method parameters with `@Port` annotations
- ✅ **Output ports** detected from class properties with `@Output` annotations  
- ✅ **Type mapping** from Kotlin types to frontend types with color coding
- ✅ **JSON generation** for the graph editor automatically

## 🔧 FlowLang Integration

### Functions

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
}
```

### Events

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
    }
}
```

### Types

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
            // Your conversion logic
        }
        
        override fun validate(value: Any?): Boolean {
            // Your validation logic
        }
    }
}
```

## ⌨️ Terminal Commands

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
}
```

## 🚀 Getting Started

### 1. Create Your Extension

```kotlin
@FlowExtension(
    name = "MyAwesomeExtension",
    description = "Does awesome things!"
)
class MyExtension : SimpleExtension() {
    
    // That's it! The framework handles everything else
    override fun onInitialize() {
        log("My extension is initializing!")
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

## 📚 Complete Developer Guide

For comprehensive documentation on creating extensions, see:

**[📖 Complete Developer Guide](extension-system-developer-guide)**

The developer guide covers:
- Detailed examples for all extension types
- Configuration and lifecycle management
- Building and deployment
- Debugging and troubleshooting
- Best practices and advanced features

## 📚 Documentation

- 📋 [**Documentation Index**](extension-system-documentation-index) - Complete overview of all documentation
- 🚀 [**Quick Start Guide**](extension-system-quick-start) - Get up and running in 5 minutes
- 📖 [**Complete Developer Guide**](extension-system-developer-guide) - Detailed documentation for creating extensions
- 📚 [**API Reference**](extension-system-api-reference) - Complete API documentation

## 🏗️ Architecture

```
ExtensionManager
├── HotReloadExtensionLoader (JAR loading & hot reload)
├── SimpleExtensionRegistry (Auto-discovery)
├── GraphExecutor (Flow chart execution)
└── FlowLangIntegration (FlowLang integration)
```

## 🎉 That's It!

Creating Flow extensions is incredibly simple:

1. **Annotate your classes** - The framework discovers everything automatically
2. **Focus on your logic** - No boilerplate code needed
3. **Build and deploy** - Drop JAR in extensions folder
4. **Hot reload** - Changes apply instantly

The framework handles all the complex stuff - you just focus on your business logic!

- **TRIGGER nodes**: Start graph execution
- **ACTION nodes**: Perform actions with automatic input/output handling
- **FlowLang functions**: Add scripting capabilities
- **FlowLang events**: Handle events
- **FlowLang types**: Define custom types
- **Terminal commands**: Add CLI commands
- **Hot reloading**: Automatic reloading on changes

Happy coding! 🚀
