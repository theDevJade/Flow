---
title: Developer Guide
description: Complete guide for extending and integrating FlowLang
---

# FlowLang Developer Guide

This comprehensive guide covers everything you need to know about extending and integrating FlowLang into your applications.

## Getting Started

### Prerequisites

- Kotlin 1.8+
- JVM 11+
- Gradle 7.0+

### Installation

Add FlowLang to your project:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.thedevjade.io:flowlang:1.0.0")
}
```

### Basic Usage

```kotlin
import com.thedevjade.io.flowlang.*

fun main() {
    // Initialize FlowLang
    FlowLang.start()
    
    // Get the engine instance
    val engine = FlowLangEngine.getInstance()
    
    // Execute a script
    val result = engine.execute("""
        print("Hello from FlowLang!")
        var x = 10
        var y = 20
        return x + y
    """.trimIndent())
    
    println("Result: $result")
    
    // Clean up
    FlowLang.stop()
}
```

## Architecture Overview

FlowLang follows a modular architecture with clear separation of concerns:

```
┌─────────────────┐
│   FlowLang      │ ← Main API entry point
├─────────────────┤
│ FlowLangEngine  │ ← Core engine (singleton)
├─────────────────┤
│   Preprocessor  │ ← Natural language processing
├─────────────────┤
│ FlowLangParser  │ ← Syntax parsing
├─────────────────┤
│ FlowLangLexer   │ ← Tokenization
├─────────────────┤
│ FlowLangExecutor│ ← Script execution
├─────────────────┤
│ Memory System   │ ← Variables, functions, types
├─────────────────┤
│   AST Nodes     │ ← Abstract syntax tree
└─────────────────┘
```

### Key Design Principles

1. **Modularity**: Each component has a single responsibility
2. **Extensibility**: Easy to add custom types and functions
3. **Thread Safety**: Concurrent access to shared resources
4. **Memory Efficiency**: Automatic garbage collection
5. **Error Resilience**: Graceful error handling

## Core Components

### FlowLang (Main API)

The main entry point for the FlowLang library:

```kotlin
object FlowLang {
    fun start(configuration: FlowLangConfiguration? = null)
    fun stop()
    val started: Boolean
    val configuration: FlowLangConfiguration?
    val watcher: FlowLangWatcher?
}
```

### FlowLangEngine

The core scripting engine that manages execution:

```kotlin
class FlowLangEngine {
    companion object {
        fun getInstance(): FlowLangEngine
    }
    
    // Script execution
    fun execute(script: String, context: FlowLangContext? = null): Any?
    fun execute(script: FlowLangScript, context: FlowLangContext): Any?
    
    // Function management
    fun registerFunction(function: FlowLangFunction)
    fun getFunction(name: String): FlowLangFunction?
    fun unregisterFunction(name: String)
    
    // Type management
    fun registerType(type: FlowLangType)
    fun getType(name: String): FlowLangType?
    fun unregisterType(name: String)
    
    // Event management
    fun registerEvent(event: FlowLangEvent)
    fun triggerEvent(name: String, vararg args: Any)
    fun getEvent(name: String): FlowLangEvent?
    
    // Variable management
    fun setGlobalVariable(name: String, value: Any?)
    fun getGlobalVariable(name: String): FlowLangVariable?
}
```

### Preprocessor

Converts natural language to FlowLang code:

```kotlin
class Preprocessor(private val registerDefaults: Boolean = true) {
    fun process(input: String): String
    
    // Custom phrase replacements
    fun registerPhraseReplacement(pattern: String, replacement: String)
    
    // Custom token replacements
    fun registerTokenReplacement(word: String, replacement: String)
    
    // Custom removable keywords
    fun registerRemovableKeyword(keyword: String)
}
```

## Adding Custom Types

### Step 1: Create Your Type Class

```kotlin
// Example: User type
data class User(
    val username: String,
    val displayName: String,
    val email: String,
    val isActive: Boolean = true
) {
    fun getFullInfo(): String = "$displayName ($username) - $email"
    
    fun activate() = copy(isActive = true)
    fun deactivate() = copy(isActive = false)
}
```

### Step 2: Register the Type

```kotlin
val engine = FlowLangEngine.getInstance()

// Register the type
engine.registerType(FlowLangType(
    name = "User",
    kotlinType = User::class.java,
    fromStringLiteral = { str ->
        // Custom parsing logic if needed
        val parts = str.split(",")
        if (parts.size >= 3) {
            User(parts[0], parts[1], parts[2])
        } else {
            throw Exception("Invalid User format: $str")
        }
    }
))
```

### Step 3: Use in Scripts

```flowlang
# Create user (via custom function)
var user = createUser("john_doe", "John Doe", "john@example.com")

# Access properties
print("Username: " + user.username)
print("Display Name: " + user.displayName)
print("Email: " + user.email)
print("Active: " + user.isActive)

# Call methods (via custom functions)
var info = user.getFullInfo()
print("Full Info: " + info)
```

### Step 4: Create Helper Functions

```kotlin
// Register helper functions for your type
engine.registerFunction(FlowLangFunction("createUser", { args ->
    User(
        username = args[0] as String,
        displayName = args[1] as String,
        email = args[2] as String
    )
}, arrayOf(
    FlowLangParameter("username", "text"),
    FlowLangParameter("displayName", "text"),
    FlowLangParameter("email", "text")
)))

engine.registerFunction(FlowLangFunction("getUserInfo", { args ->
    val user = args[0] as User
    user.getFullInfo()
}, arrayOf(
    FlowLangParameter("user", "User")
)))
```

## Adding Custom Functions

### Basic Function Registration

```kotlin
val engine = FlowLangEngine.getInstance()

// Simple function
engine.registerFunction(FlowLangFunction("multiply", { args ->
    val a = (args[0] as? Number)?.toDouble() ?: 0.0
    val b = (args[1] as? Number)?.toDouble() ?: 0.0
    a * b
}, arrayOf(
    FlowLangParameter("a", "number"),
    FlowLangParameter("b", "number")
)))
```

### Advanced Function with Error Handling

```kotlin
engine.registerFunction(FlowLangFunction("divide", { args ->
    val a = (args[0] as? Number)?.toDouble() ?: 0.0
    val b = (args[1] as? Number)?.toDouble() ?: 0.0
    
    if (b == 0.0) {
        throw Exception("Division by zero")
    }
    
    a / b
}, arrayOf(
    FlowLangParameter("a", "number"),
    FlowLangParameter("b", "number")
)))
```

### Function with Optional Parameters

```kotlin
engine.registerFunction(FlowLangFunction("formatNumber", { args ->
    val number = (args[0] as? Number)?.toDouble() ?: 0.0
    val decimals = (args[1] as? Number)?.toInt() ?: 2
    
    String.format("%.${decimals}f", number)
}, arrayOf(
    FlowLangParameter("number", "number"),
    FlowLangParameter("decimals", "number", isOptional = true, defaultValue = 2)
)))
```

### Function with Custom Types

```kotlin
engine.registerFunction(FlowLangFunction("createVector3", { args ->
    Vector3(
        x = (args[0] as? Number)?.toDouble() ?: 0.0,
        y = (args[1] as? Number)?.toDouble() ?: 0.0,
        z = (args[2] as? Number)?.toDouble() ?: 0.0
    )
}, arrayOf(
    FlowLangParameter("x", "number"),
    FlowLangParameter("y", "number"),
    FlowLangParameter("z", "number")
)))
```

## Event System

### Registering Events

```kotlin
val engine = FlowLangEngine.getInstance()

// Register an event
engine.registerEvent(FlowLangEvent("userLogin", arrayOf(
    FlowLangParameter("username", "text"),
    FlowLangParameter("timestamp", "number"),
    FlowLangParameter("ipAddress", "text")
)))
```

### Event Handlers in Scripts

```flowlang
# Define event handler
on userLogin {
    print("User " + username + " logged in")
    print("Time: " + timestamp)
    print("IP: " + ipAddress)
    
    # Log the event
    logEvent("login", username)
}
```

### Triggering Events

```kotlin
// Trigger event from Kotlin code
engine.triggerEvent("userLogin", "john_doe", System.currentTimeMillis(), "192.168.1.1")

// Trigger event from within script
engine.execute("""
    trigger userLogin("jane_doe", System.currentTimeMillis(), "192.168.1.2")
""".trimIndent())
```

### Complex Event System

```kotlin
// Multiple events
engine.registerEvent(FlowLangEvent("dataReceived", arrayOf(
    FlowLangParameter("data", "text"),
    FlowLangParameter("source", "text")
)))

engine.registerEvent(FlowLangEvent("errorOccurred", arrayOf(
    FlowLangParameter("error", "text"),
    FlowLangParameter("code", "number")
)))

// Event handler with multiple events
engine.execute("""
    on dataReceived {
        print("Data from " + source + ": " + data)
        processData(data)
    }
    
    on errorOccurred {
        print("Error " + code + ": " + error)
        handleError(error, code)
    }
""".trimIndent())
```

## Memory Management

### Context System

FlowLang uses a hierarchical context system for variable scoping:

```kotlin
// Create root context
val rootContext = FlowLangContext()

// Set global variables
rootContext.setVariable("globalVar", "I'm global")

// Create child context
val childContext = rootContext.createChildContext()
childContext.setVariable("localVar", "I'm local")

// Child can access parent's variables
val globalValue = childContext.getVariable("globalVar")  // Returns global variable

// Parent cannot access child's variables
val localValue = rootContext.getVariable("localVar")  // Returns null
```

### Variable Lifecycle

```kotlin
// Variables are automatically garbage collected
val context = FlowLangContext()

// Set variable
context.setVariable("temp", "temporary value")

// Variable exists
val value = context.getVariable("temp")  // Returns "temporary value"

// Variable is automatically cleaned up when context goes out of scope
```

### Memory Best Practices

1. **Use appropriate context levels**: Don't create unnecessary child contexts
2. **Clean up large objects**: Set variables to null when done
3. **Avoid memory leaks**: Don't hold references to contexts longer than needed
4. **Use concurrent collections**: FlowLang uses thread-safe collections internally

## Parser and Lexer

### Lexer (Tokenization)

The lexer converts source code into tokens:

```kotlin
val lexer = FlowLangLexer()
val tokens = lexer.tokenize("""
    var x = 10
    print("Hello")
""".trimIndent())

// Tokens: [VAR, IDENTIFIER(x), ASSIGN, NUMBER(10), NEWLINE, IDENTIFIER(print), LEFT_PAREN, STRING("Hello"), RIGHT_PAREN]
```

### Parser (AST Generation)

The parser converts tokens into an Abstract Syntax Tree:

```kotlin
val parser = FlowLangParser()
val ast = parser.parse("""
    var x = 10
    if (x > 5) {
        print("x is greater than 5")
    }
""".trimIndent())

// AST: FlowLangScript containing BlockNode with VariableDeclaration and IfNode
```

### Custom Token Types

To add custom token types, extend the lexer:

```kotlin
class CustomFlowLangLexer : FlowLangLexer() {
    override fun tokenize(source: String): List<Token> {
        // Add custom tokenization logic
        val tokens = super.tokenize(source)
        
        // Process custom tokens
        return processCustomTokens(tokens)
    }
    
    private fun processCustomTokens(tokens: List<Token>): List<Token> {
        // Custom token processing
        return tokens
    }
}
```

## AST Nodes

### Node Hierarchy

```
FlowLangNode (abstract)
├── LiteralNode
├── VariableNode
├── AssignmentNode
├── FunctionCallNode
├── BinaryOpNode
│   ├── IfNode
│   ├── WhileNode
│   ├── ForNode
│   ├── FunctionDefNode
│   ├── ReturnNode
│   ├── BlockNode
│   └── FlowLangScript
└── UnaryOpNode
```

### Creating Custom Nodes

```kotlin
class CustomNode(
    private val value: String
) : FlowLangNode() {
    override fun execute(context: FlowLangContext): Any? {
        // Custom execution logic
        return "Custom: $value"
    }
}
```

### Extending Existing Nodes

```kotlin
class EnhancedIfNode(
    condition: FlowLangNode,
    thenBranch: FlowLangNode,
    elseBranch: FlowLangNode? = null,
    private val debugInfo: String? = null
) : BinaryOpNode.IfNode(condition, thenBranch, elseBranch) {
    
    override fun execute(context: FlowLangContext): Any? {
        if (debugInfo != null) {
            println("Debug: $debugInfo")
        }
        return super.execute(context)
    }
}
```

## Error Handling

### Exception Types

FlowLang defines several exception types:

```kotlin
// Syntax errors
class SyntaxException(message: String) : Exception(message)

// Type errors
class TypeError(message: String) : Exception(message)

// Runtime errors
class RuntimeError(message: String) : Exception(message)

// Variable errors
class UndefinedVariableError(variableName: String) : Exception("Variable '$variableName' is not defined")

// Function errors
class FunctionNotFoundError(functionName: String) : Exception("Function '$functionName' not found")
```

### Error Handling in Custom Functions

```kotlin
engine.registerFunction(FlowLangFunction("safeDivide", { args ->
    try {
        val a = (args[0] as? Number)?.toDouble() ?: 0.0
        val b = (args[1] as? Number)?.toDouble() ?: 0.0
        
        if (b == 0.0) {
            throw Exception("Division by zero")
        }
        
        a / b
    } catch (e: Exception) {
        // Log error and return default value
        println("Error in safeDivide: ${e.message}")
        0.0
    }
}, arrayOf(
    FlowLangParameter("a", "number"),
    FlowLangParameter("b", "number")
)))
```

### Global Error Handling

```kotlin
// Set up global error handler
GlobalHooks.loggingHook = object : LoggingHook {
    override fun error(message: String) {
        println("ERROR: $message")
        // Log to file, send to monitoring service, etc.
    }
    
    override fun warning(message: String) {
        println("WARNING: $message")
    }
    
    override fun info(message: String) {
        println("INFO: $message")
    }
}
```

## Testing

### Unit Testing

```kotlin
class FlowLangTests {
    private lateinit var engine: FlowLangEngine
    
    @BeforeEach
    fun setUp() {
        FlowLang.start()
        engine = FlowLangEngine.getInstance()
    }
    
    @AfterEach
    fun tearDown() {
        FlowLang.stop()
    }
    
    @Test
    fun testBasicExecution() {
        val result = engine.execute("""
            var x = 10
            var y = 20
            return x + y
        """.trimIndent())
        
        assertEquals(30.0, result)
    }
    
    @Test
    fun testCustomFunction() {
        engine.registerFunction(FlowLangFunction("multiply", { args ->
            (args[0] as Double) * (args[1] as Double)
        }, arrayOf(
            FlowLangParameter("a", "number"),
            FlowLangParameter("b", "number")
        )))
        
        val result = engine.execute("return multiply(5, 3)")
        assertEquals(15.0, result)
    }
}
```

### Integration Testing

```kotlin
class FlowLangIntegrationTests {
    @Test
    fun testFullWorkflow() {
        FlowLang.start()
        val engine = FlowLangEngine.getInstance()
        
        // Register custom types and functions
        setupCustomTypes(engine)
        setupCustomFunctions(engine)
        
        // Execute complex script
        val script = loadScript("complex_workflow.flowlang")
        val result = engine.execute(script)
        
        // Verify results
        assertNotNull(result)
        assertEquals(expectedResult, result)
        
        FlowLang.stop()
    }
}
```

## Performance Considerations

### Memory Usage

1. **Context Management**: Use appropriate context levels
2. **Variable Cleanup**: Set large variables to null when done
3. **Function Caching**: Cache frequently used functions
4. **Type Registration**: Register types once at startup

### Execution Performance

1. **Script Compilation**: Parse scripts once, execute many times
2. **Function Calls**: Minimize deep function call stacks
3. **String Operations**: Use StringBuilder for complex string operations
4. **Type Conversions**: Cache type conversion results

### Best Practices

```kotlin
// Good: Parse once, execute many times
val script = parser.parse(sourceCode)
val context = FlowLangContext()

repeat(1000) {
    executor.executeScript(script, context)
}

// Bad: Parse every time
repeat(1000) {
    engine.execute(sourceCode)  // Parses every time
}
```

## Debugging

### Debug Output

```kotlin
// Enable debug mode
FlowLang.start(FlowLangConfiguration(debugMode = true))

// Debug output will show:
// - Tokenization results
// - AST structure
// - Execution steps
// - Variable assignments
```

### Logging

```kotlin
// Custom logging
GlobalHooks.loggingHook = object : LoggingHook {
    override fun error(message: String) {
        logger.error("FlowLang Error: $message")
    }
    
    override fun warning(message: String) {
        logger.warn("FlowLang Warning: $message")
    }
    
    override fun info(message: String) {
        logger.info("FlowLang Info: $message")
    }
}
```

### Debugging Tools

```kotlin
// Inspect AST
val ast = parser.parse(script)
println("AST: $ast")

// Inspect variables
val context = FlowLangContext()
context.setVariable("test", "value")
println("Variables: ${context.getAllVariables()}")

// Step-by-step execution
val executor = FlowLangExecutor()
executor.setDebugMode(true)
executor.executeScript(script, context)
```

## Advanced Topics

### Custom Preprocessor Rules

```kotlin
val preprocessor = Preprocessor()

// Add custom phrase replacement
preprocessor.registerPhraseReplacement(
    "\\btwice\\s+as\\s+big\\b", 
    "* 2"
)

// Add custom token replacement
preprocessor.registerTokenReplacement("plus", "+")

// Add custom removable keyword
preprocessor.registerRemovableKeyword("please")
```

### Plugin System

```kotlin
interface FlowLangPlugin {
    fun initialize(engine: FlowLangEngine)
    fun cleanup()
}

class MyPlugin : FlowLangPlugin {
    override fun initialize(engine: FlowLangEngine) {
        // Register custom types and functions
        engine.registerType(MyCustomType())
        engine.registerFunction(MyCustomFunction())
    }
    
    override fun cleanup() {
        // Clean up resources
    }
}

// Use plugin
val plugin = MyPlugin()
plugin.initialize(engine)
```

### Thread Safety

FlowLang is designed to be thread-safe:

```kotlin
// Safe to use from multiple threads
val engine = FlowLangEngine.getInstance()

// Each thread should use its own context
val context = FlowLangContext()

// Execute script safely
val result = engine.execute(script, context)
```

This completes the comprehensive developer guide for FlowLang. The guide covers all major aspects of extending and working with the FlowLang scripting language.
