---
title: API Reference
description: Complete FlowLang API documentation
---

# FlowLang API Reference

Complete documentation for all FlowLang classes, methods, and properties.

## Core API

### FlowLang

Main entry point for the FlowLang library.

```kotlin
object FlowLang {
    fun start(configuration: FlowLangConfiguration? = null)
    fun stop()
    val started: Boolean
    val configuration: FlowLangConfiguration?
    val watcher: FlowLangWatcher?
}
```

#### Methods

##### `start(configuration: FlowLangConfiguration? = null)`

Initializes the FlowLang system.

**Parameters:**
- `configuration`: Optional configuration object

**Example:**
```kotlin
FlowLang.start()
// or with configuration
FlowLang.start(FlowLangConfiguration(debugMode = true))
```

##### `stop()`

Stops the FlowLang system and cleans up resources.

**Example:**
```kotlin
FlowLang.stop()
```

#### Properties

##### `started: Boolean`

Returns `true` if FlowLang is currently running.

##### `configuration: FlowLangConfiguration?`

Returns the current configuration, or `null` if not started.

##### `watcher: FlowLangWatcher?`

Returns the file watcher instance, or `null` if not started.

### FlowLangConfiguration

Configuration options for FlowLang.

```kotlin
data class FlowLangConfiguration(
    val debugMode: Boolean = false,
    val enableFileWatching: Boolean = false,
    val watchDirectory: String? = null,
    val logLevel: LogLevel = LogLevel.INFO
)
```

#### Properties

- `debugMode`: Enable debug output
- `enableFileWatching`: Enable file watching for hot reloading
- `watchDirectory`: Directory to watch for changes
- `logLevel`: Logging level

## FlowLangEngine

Core scripting engine that manages execution.

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
    fun getAllFunctions(): Map<String, FlowLangFunction>
    
    // Type management
    fun registerType(type: FlowLangType)
    fun getType(name: String): FlowLangType?
    fun unregisterType(name: String)
    fun getAllTypes(): Map<String, FlowLangType>
    
    // Event management
    fun registerEvent(event: FlowLangEvent)
    fun triggerEvent(name: String, vararg args: Any)
    fun getEvent(name: String): FlowLangEvent?
    fun unregisterEvent(name: String)
    fun getAllEvents(): Map<String, FlowLangEvent>
    
    // Variable management
    fun setGlobalVariable(name: String, value: Any?)
    fun getGlobalVariable(name: String): FlowLangVariable?
    fun unregisterGlobalVariable(name: String)
    fun getAllGlobalVariables(): Map<String, FlowLangVariable>
}
```

### Methods

#### Script Execution

##### `execute(script: String, context: FlowLangContext? = null): Any?`

Executes a FlowLang script string.

**Parameters:**
- `script`: The script to execute
- `context`: Optional execution context

**Returns:** The result of script execution

**Example:**
```kotlin
val engine = FlowLangEngine.getInstance()
val result = engine.execute("""
    var x = 10
    var y = 20
    return x + y
""".trimIndent())
```

##### `execute(script: FlowLangScript, context: FlowLangContext): Any?`

Executes a pre-parsed FlowLang script.

**Parameters:**
- `script`: The parsed script AST
- `context`: Execution context

**Returns:** The result of script execution

#### Function Management

##### `registerFunction(function: FlowLangFunction)`

Registers a custom function.

**Parameters:**
- `function`: The function to register

**Example:**
```kotlin
engine.registerFunction(FlowLangFunction("multiply", { args ->
    (args[0] as Double) * (args[1] as Double)
}, arrayOf(
    FlowLangParameter("a", "number"),
    FlowLangParameter("b", "number")
)))
```

##### `getFunction(name: String): FlowLangFunction?`

Gets a registered function by name.

**Parameters:**
- `name`: Function name

**Returns:** The function or `null` if not found

##### `unregisterFunction(name: String)`

Unregisters a function.

**Parameters:**
- `name`: Function name to unregister

#### Type Management

##### `registerType(type: FlowLangType)`

Registers a custom type.

**Parameters:**
- `type`: The type to register

**Example:**
```kotlin
engine.registerType(FlowLangType(
    name = "User",
    kotlinType = User::class.java
))
```

##### `getType(name: String): FlowLangType?`

Gets a registered type by name.

**Parameters:**
- `name`: Type name

**Returns:** The type or `null` if not found

#### Event Management

##### `registerEvent(event: FlowLangEvent)`

Registers an event.

**Parameters:**
- `event`: The event to register

**Example:**
```kotlin
engine.registerEvent(FlowLangEvent("userLogin", arrayOf(
    FlowLangParameter("username", "text"),
    FlowLangParameter("timestamp", "number")
)))
```

##### `triggerEvent(name: String, vararg args: Any)`

Triggers an event.

**Parameters:**
- `name`: Event name
- `args`: Event arguments

**Example:**
```kotlin
engine.triggerEvent("userLogin", "john_doe", System.currentTimeMillis())
```

## Memory System

### FlowLangContext

Execution context for variable scoping.

```kotlin
class FlowLangContext(private val parent: FlowLangContext? = null) {
    fun getVariable(name: String): FlowLangVariable?
    fun setVariable(name: String, value: Any?)
    fun createChildContext(): FlowLangContext
    fun getAllVariables(): Map<String, FlowLangVariable>
}
```

#### Methods

##### `getVariable(name: String): FlowLangVariable?`

Gets a variable by name.

**Parameters:**
- `name`: Variable name

**Returns:** The variable or `null` if not found

##### `setVariable(name: String, value: Any?)`

Sets a variable value.

**Parameters:**
- `name`: Variable name
- `value`: Variable value

##### `createChildContext(): FlowLangContext`

Creates a child context with this as parent.

**Returns:** New child context

### FlowLangVariable

Represents a variable in memory.

```kotlin
class FlowLangVariable(val name: String, var value: Any?)
```

#### Properties

- `name`: Variable name
- `value`: Variable value (mutable)

### FlowLangFunction

Represents a custom function.

```kotlin
class FlowLangFunction(
    val name: String,
    val implementation: (Array<Any?>) -> Any?,
    val parameters: Array<FlowLangParameter>
) {
    fun invoke(args: Array<Any?>): Any?
}
```

#### Properties

- `name`: Function name
- `implementation`: Function implementation
- `parameters`: Function parameters

#### Methods

##### `invoke(args: Array<Any?>): Any?`

Invokes the function with arguments.

**Parameters:**
- `args`: Function arguments

**Returns:** Function result

### FlowLangParameter

Represents a function parameter.

```kotlin
class FlowLangParameter(
    val name: String,
    val typeName: String,
    val isOptional: Boolean = false,
    val defaultValue: Any? = null
)
```

#### Properties

- `name`: Parameter name
- `typeName`: Parameter type name
- `isOptional`: Whether parameter is optional
- `defaultValue`: Default value for optional parameters

### FlowLangType

Represents a custom type.

```kotlin
class FlowLangType(
    val name: String,
    val kotlinType: Class<*>,
    private val fromStringLiteral: ((String) -> Any?)? = null
) {
    fun convert(value: Any?): Any?
}
```

#### Properties

- `name`: Type name
- `kotlinType`: Kotlin type class

#### Methods

##### `convert(value: Any?): Any?`

Converts a value to this type.

**Parameters:**
- `value`: Value to convert

**Returns:** Converted value

## Parser and Lexer

### FlowLangParser

Parses FlowLang source code into AST.

```kotlin
class FlowLangParser {
    fun parse(sourceUnprocessed: String): FlowLangScript
}
```

#### Methods

##### `parse(sourceUnprocessed: String): FlowLangScript`

Parses source code into an AST.

**Parameters:**
- `sourceUnprocessed`: Raw source code

**Returns:** Parsed script AST

### FlowLangLexer

Tokenizes FlowLang source code.

```kotlin
class FlowLangLexer {
    fun tokenize(source: String): List<Token>
}
```

#### Methods

##### `tokenize(source: String): List<Token>`

Tokenizes source code.

**Parameters:**
- `source`: Source code to tokenize

**Returns:** List of tokens

### Token

Represents a lexical token.

```kotlin
data class Token(
    val type: TokenType,
    val value: String,
    val line: Int,
    val column: Int
)
```

#### Properties

- `type`: Token type
- `value`: Token value
- `line`: Line number
- `column`: Column number

### TokenType

Enumeration of token types.

```kotlin
enum class TokenType {
    // Literals
    NUMBER, STRING, BOOLEAN, NULL,
    
    // Identifiers
    IDENTIFIER, KEYWORD,
    
    // Operators
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
    EQUAL, NOT_EQUAL, LESS_THAN, GREATER_THAN,
    LESS_EQUAL, GREATER_EQUAL,
    AND, OR, NOT,
    
    // Delimiters
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, SEMICOLON, DOT,
    
    // Assignment
    ASSIGN,
    
    // Control
    IF, ELSE, WHILE, FOR, FUNCTION, RETURN,
    VAR, EVENT, ON, TRIGGER,
    
    // Special
    END_OF_LINE, END_OF_FILE
}
```

## AST Nodes

### FlowLangNode

Base class for all AST nodes.

```kotlin
abstract class FlowLangNode {
    abstract fun execute(context: FlowLangContext): Any?
}
```

### LiteralNode

Represents a literal value.

```kotlin
class LiteralNode(private val value: Any?) : FlowLangNode()
```

### VariableNode

Represents a variable reference.

```kotlin
class VariableNode(val name: String) : FlowLangNode()
```

### AssignmentNode

Represents a variable assignment.

```kotlin
class AssignmentNode(val name: String, val value: FlowLangNode) : FlowLangNode()
```

### FunctionCallNode

Represents a function call.

```kotlin
class FunctionCallNode(val name: String, val arguments: List<FlowLangNode>) : FlowLangNode()
```

### BinaryOpNode

Represents binary operations and control structures.

```kotlin
class BinaryOpNode(val left: FlowLangNode, val operator: String, val right: FlowLangNode) : FlowLangNode()
```

#### Nested Classes

##### `IfNode`

Represents an if statement.

```kotlin
class IfNode(
    val condition: FlowLangNode,
    val thenBranch: FlowLangNode,
    val elseBranch: FlowLangNode? = null
) : FlowLangNode()
```

##### `WhileNode`

Represents a while loop.

```kotlin
class WhileNode(
    val condition: FlowLangNode,
    val body: FlowLangNode
) : FlowLangNode()
```

##### `ForNode`

Represents a for loop.

```kotlin
class ForNode(
    val initialization: FlowLangNode,
    val condition: FlowLangNode,
    val increment: FlowLangNode,
    val body: FlowLangNode
) : FlowLangNode()
```

##### `FunctionDefNode`

Represents a function definition.

```kotlin
class FunctionDefNode(
    val name: String,
    val parameters: List<String>,
    val body: FlowLangNode
) : FlowLangNode()
```

##### `ReturnNode`

Represents a return statement.

```kotlin
class ReturnNode(val value: FlowLangNode) : FlowLangNode()
```

##### `BlockNode`

Represents a block of statements.

```kotlin
class BlockNode(val statements: List<FlowLangNode>) : FlowLangNode()
```

##### `FlowLangScript`

Represents a complete script.

```kotlin
class FlowLangScript(val root: BlockNode) : FlowLangNode()
```

### UnaryOpNode

Represents unary operations.

```kotlin
class UnaryOpNode(
    val operator: String,
    val operand: FlowLangNode
) : FlowLangNode()
```

## Event System

### FlowLangEvent

Represents an event.

```kotlin
class FlowLangEvent(
    val name: String,
    val parameters: Array<FlowLangParameter>
) {
    fun registerHandler(script: FlowLangScript, context: FlowLangContext)
    fun unregisterHandler(script: FlowLangScript, context: FlowLangContext)
    fun clearHandlers()
    fun trigger(args: Array<out Any>)
}
```

#### Methods

##### `registerHandler(script: FlowLangScript, context: FlowLangContext)`

Registers an event handler.

**Parameters:**
- `script`: Handler script
- `context`: Handler context

##### `unregisterHandler(script: FlowLangScript, context: FlowLangContext)`

Unregisters an event handler.

**Parameters:**
- `script`: Handler script
- `context`: Handler context

##### `trigger(args: Array<out Any>)`

Triggers the event.

**Parameters:**
- `args`: Event arguments

## Preprocessor

### Preprocessor

Converts natural language to FlowLang code.

```kotlin
class Preprocessor(private val registerDefaults: Boolean = true) {
    fun process(input: String): String
    fun registerPhraseReplacement(pattern: String, replacement: String, options: Int = Pattern.CASE_INSENSITIVE)
    fun registerTokenReplacement(word: String, replacement: String)
    fun registerRemovableKeyword(keyword: String)
}
```

#### Methods

##### `process(input: String): String`

Processes natural language input.

**Parameters:**
- `input`: Natural language input

**Returns:** Processed FlowLang code

##### `registerPhraseReplacement(pattern: String, replacement: String, options: Int = Pattern.CASE_INSENSITIVE)`

Registers a phrase replacement rule.

**Parameters:**
- `pattern`: Regex pattern
- `replacement`: Replacement string
- `options`: Regex options

##### `registerTokenReplacement(word: String, replacement: String)`

Registers a token replacement rule.

**Parameters:**
- `word`: Word to replace
- `replacement`: Replacement string

##### `registerRemovableKeyword(keyword: String)`

Registers a removable keyword.

**Parameters:**
- `keyword`: Keyword to remove

## Configuration

### FlowLangConfiguration

Configuration options for FlowLang.

```kotlin
data class FlowLangConfiguration(
    val debugMode: Boolean = false,
    val enableFileWatching: Boolean = false,
    val watchDirectory: String? = null,
    val logLevel: LogLevel = LogLevel.INFO
)
```

### LogLevel

Logging levels.

```kotlin
enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}
```

## Utilities

### GlobalHooks

Global hooks for logging and other services.

```kotlin
object GlobalHooks {
    var loggingHook: LoggingHook = DefaultLoggingHook()
}
```

### LoggingHook

Interface for logging.

```kotlin
interface LoggingHook {
    fun error(message: String)
    fun warning(message: String)
    fun info(message: String)
    fun debug(message: String)
}
```

### DefaultLoggingHook

Default logging implementation.

```kotlin
class DefaultLoggingHook : LoggingHook {
    override fun error(message: String) { println("ERROR: $message") }
    override fun warning(message: String) { println("WARNING: $message") }
    override fun info(message: String) { println("INFO: $message") }
    override fun debug(message: String) { println("DEBUG: $message") }
}
```

### FlowLangWatcher

File watching for hot reloading.

```kotlin
class FlowLangWatcher(
    private val watchDirectory: String,
    private val onFileChanged: (String) -> Unit
) {
    fun start()
    fun stop()
    fun isWatching(): Boolean
}
```

#### Methods

##### `start()`

Starts file watching.

##### `stop()`

Stops file watching.

##### `isWatching(): Boolean`

Returns `true` if currently watching files.

## Built-in Types

### Vector3

3D vector type.

```kotlin
data class Vector3(
    var x: Double,
    var y: Double,
    var z: Double
) {
    operator fun plus(other: Vector3): Vector3
    operator fun minus(other: Vector3): Vector3
    operator fun times(scalar: Double): Vector3
    operator fun div(scalar: Double): Vector3
}
```

#### Methods

##### `plus(other: Vector3): Vector3`

Vector addition.

##### `minus(other: Vector3): Vector3`

Vector subtraction.

##### `times(scalar: Double): Vector3`

Scalar multiplication.

##### `div(scalar: Double): Vector3`

Scalar division.

## Error Types

### SyntaxException

Syntax error in script.

```kotlin
class SyntaxException(message: String) : Exception(message)
```

### TypeError

Type conversion error.

```kotlin
class TypeError(message: String) : Exception(message)
```

### RuntimeError

Runtime execution error.

```kotlin
class RuntimeError(message: String) : Exception(message)
```

### UndefinedVariableError

Variable not found error.

```kotlin
class UndefinedVariableError(variableName: String) : Exception("Variable '$variableName' is not defined")
```

### FunctionNotFoundError

Function not found error.

```kotlin
class FunctionNotFoundError(functionName: String) : Exception("Function '$functionName' not found")
```

This API reference provides comprehensive documentation for all FlowLang classes, methods, and properties. Use this as a reference when developing with FlowLang.
