---
title: Tutorial
description: Step-by-step tutorial for learning FlowLang
---

# FlowLang Tutorial

This comprehensive tutorial will guide you through learning FlowLang from the basics to advanced features.

## Getting Started

### Installation

Add FlowLang to your Kotlin project:

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.thedevjade.io:flowlang:1.0.0")
}
```

### Your First Script

```kotlin
import com.thedevjade.io.flowlang.*

fun main() {
    // Initialize FlowLang
    FlowLang.start()
    
    // Get the engine
    val engine = FlowLangEngine.getInstance()
    
    // Execute your first script
    engine.execute("""
        print("Hello, FlowLang!")
        var name = "World"
        print("Hello, " + name + "!")
    """.trimIndent())
    
    // Clean up
    FlowLang.stop()
}
```

**Output:**
```
Hello, FlowLang!
Hello, World!
```

## Basic Syntax

### Comments

```flowlang
# This is a single-line comment
var x = 10  # This is an inline comment

# Comments are ignored by the parser
# var y = 20  # This line is commented out
```

### Statements

FlowLang statements end with newlines (no semicolons needed):

```flowlang
var x = 10
var y = 20
print("x + y = " + (x + y))
```

### Case Sensitivity

FlowLang is case-sensitive:

```flowlang
var Name = "John"    # Different from 'name'
var name = "Jane"    # Different from 'Name'
var NAME = "Bob"     # Different from both above
```

## Variables and Data Types

### Variable Declaration

```flowlang
# Declare and initialize
var name = "FlowLang"
var version = 1.0
var isActive = true
var data = null

# Reassign values
name = "FlowLang Pro"
version = 2.0
isActive = false
```

### Data Types

FlowLang supports several built-in types:

#### Numbers

```flowlang
var integer = 42
var decimal = 3.14
var negative = -10.5
var scientific = 1.23e4  # 12300.0

# Math operations
var sum = 10 + 5        # 15
var product = 3 * 4     # 12
var quotient = 15 / 3   # 5
var remainder = 10 % 3  # 1
```

#### Strings

```flowlang
var single = 'Hello'
var double = "World"
var empty = ""

# String concatenation
var greeting = "Hello" + " " + "World"  # "Hello World"
var number = "The answer is " + 42      # "The answer is 42"
```

#### Booleans

```flowlang
var isTrue = true
var isFalse = false

# Boolean operations
var andResult = true and false    # false
var orResult = true or false      # true
var notResult = not true          # false
```

#### Null Values

```flowlang
var nothing = null

# Check for null (in practice, you'd use functions)
var hasValue = (nothing != null)  # false
```

### Type Conversion

FlowLang automatically converts between compatible types:

```flowlang
var num = 42
var str = "42"
var bool = true

# Automatic conversions
var result1 = num + str    # "42" + "42" = "4242"
var result2 = num + bool   # 42 + 1 = 43
var result3 = str + bool   # "42" + "true" = "42true"
```

## Control Flow

### If Statements

```flowlang
# Basic if
var age = 18
if (age >= 18) {
    print("You are an adult")
}

# If-else
if (age >= 18) {
    print("Adult")
} else {
    print("Minor")
}

# If-elseif-else
var score = 85
if (score >= 90) {
    print("Grade: A")
} else if (score >= 80) {
    print("Grade: B")
} else if (score >= 70) {
    print("Grade: C")
} else {
    print("Grade: F")
}
```

### While Loops

```flowlang
# Basic while loop
var i = 0
while (i < 5) {
    print("i = " + i)
    i = i + 1
}

# While with condition
var count = 0
var maxCount = 3
while (count < maxCount) {
    print("Count: " + count)
    count = count + 1
}
```

### For Loops

```flowlang
# C-style for loop
for (var i = 0; i < 10; i = i + 1) {
    print("i = " + i)
}

# For loop with different increment
for (var j = 10; j > 0; j = j - 1) {
    print("j = " + j)
}

# For loop with step
for (var k = 0; k < 20; k = k + 2) {
    print("k = " + k)
}
```

## Functions

### Function Definition

```flowlang
# Basic function
function greet(name) {
    return "Hello, " + name + "!"
}

# Function with multiple parameters
function add(a, b) {
    return a + b
}

# Function with no parameters
function getCurrentTime() {
    return System.currentTimeMillis()
}

# Function with no return value
function printMessage(msg) {
    print("Message: " + msg)
}
```

### Function Calls

```flowlang
# Call functions
var greeting = greet("World")
var sum = add(5, 3)
var time = getCurrentTime()
printMessage("Hello from FlowLang!")

print(greeting)  # "Hello, World!"
print("Sum: " + sum)  # "Sum: 8"
```

### Recursive Functions

```flowlang
# Factorial function
function factorial(n) {
    if (n <= 1) {
        return 1
    } else {
        return n * factorial(n - 1)
    }
}

print("Factorial of 5: " + factorial(5))  # 120

# Fibonacci function
function fibonacci(n) {
    if (n <= 1) {
        return n
    } else {
        return fibonacci(n - 1) + fibonacci(n - 2)
    }
}

print("Fibonacci(10): " + fibonacci(10))  # 55
```

### Function Scope

```flowlang
var globalVar = "I'm global"

function testScope() {
    var localVar = "I'm local"
    print(globalVar)  # Can access global
    print(localVar)   # Can access local
}

testScope()
print(globalVar)  # Can access global
# print(localVar)  # ERROR: localVar not in scope
```

## Events

### Event Handlers

```flowlang
# Define event handler
on userLogin {
    print("User " + username + " logged in")
    print("Login time: " + timestamp)
}

# Define event handler with parameters
on dataReceived {
    print("Received data: " + data)
    print("From: " + source)
}
```

### Event System in Kotlin

```kotlin
// Register events in Kotlin
val engine = FlowLangEngine.getInstance()

engine.registerEvent(FlowLangEvent("userLogin", arrayOf(
    FlowLangParameter("username", "text"),
    FlowLangParameter("timestamp", "number")
)))

// Set up event handler
engine.execute("""
    on userLogin {
        print("User " + username + " logged in at " + timestamp)
    }
""".trimIndent())

// Trigger the event
engine.triggerEvent("userLogin", "john_doe", System.currentTimeMillis())
```

## Natural Language Processing

FlowLang includes a powerful preprocessor that converts natural language to code:

### Variable Assignment

```flowlang
# Natural language input
set total to price plus tax
set result to a times b
set difference to x minus y
set quotient to a divided by b
set remainder to x modulo y

# Gets converted to:
var total = price + tax
var result = a * b
var difference = x - y
var quotient = a / b
var remainder = x % y
```

### Comparison Operations

```flowlang
# Natural language
if score is greater than 50 then
    print("High score!")
end if

if age is less than 18 then
    print("Minor")
end if

if value is equal to zero then
    print("Zero value")
end if

# Gets converted to:
if (score > 50) {
    print("High score!")
}

if (age < 18) {
    print("Minor")
}

if (value == 0) {
    print("Zero value")
}
```

### Complex Natural Language

```flowlang
# Complex natural language script
set total to 100 plus 50
if total is greater than 100 then
    print("Total is high!")
    set result to 10 times 5
    print("Result: " + result)
end if

# Gets converted to:
var total = 100 + 50
if (total > 100) {
    print("Total is high!")
    var result = 10 * 5
    print("Result: " + result)
}
```

## Advanced Features

### Vector3 Operations

```flowlang
# Create Vector3 (via custom function)
var position = Vector3(1.0, 2.0, 3.0)

# Vector3 arithmetic
var offset = Vector3(0.5, 0.5, 0.5)
var newPosition = position + offset

# Scalar multiplication
var scaled = position * 2.0

# Access components
print("X: " + position.x)
print("Y: " + position.y)
print("Z: " + position.z)
```

### Custom Functions in Kotlin

```kotlin
// Register custom functions
val engine = FlowLangEngine.getInstance()

engine.registerFunction(FlowLangFunction("multiply", { args ->
    val a = (args[0] as? Number)?.toDouble() ?: 0.0
    val b = (args[1] as? Number)?.toDouble() ?: 0.0
    a * b
}, arrayOf(
    FlowLangParameter("a", "number"),
    FlowLangParameter("b", "number")
)))

// Use in script
engine.execute("""
    var result = multiply(5, 3)
    print("5 * 3 = " + result)
""".trimIndent())
```

### Custom Types

```kotlin
// Define custom type
data class User(
    val username: String,
    val displayName: String,
    val email: String
)

// Register type
engine.registerType(FlowLangType(
    name = "User",
    kotlinType = User::class.java
))

// Register helper functions
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
```

```flowlang
# Use custom type in script
var user = createUser("john_doe", "John Doe", "john@example.com")
print("Username: " + user.username)
print("Display Name: " + user.displayName)
print("Email: " + user.email)
```

## Best Practices

### 1. Use Meaningful Variable Names

```flowlang
# Good
var userCount = 0
var maxRetries = 3
var isConnectionActive = true

# Bad
var uc = 0
var mr = 3
var ica = true
```

### 2. Initialize Variables

```flowlang
# Good
var sum = 0
var name = ""
var isReady = false

# Bad
var sum
var name
var isReady
```

### 3. Use Comments for Complex Logic

```flowlang
# Calculate compound interest
var principal = 1000
var rate = 0.05
var time = 10
var compoundInterest = principal * (1 + rate) ^ time
print("Compound interest: " + compoundInterest)
```

### 4. Break Down Complex Logic into Functions

```flowlang
# Good: Break down into functions
function calculateTax(income, rate) {
    return income * rate
}

function calculateNetIncome(income, tax) {
    return income - tax
}

var income = 50000
var taxRate = 0.25
var tax = calculateTax(income, taxRate)
var netIncome = calculateNetIncome(income, tax)

# Bad: Everything in one place
var income = 50000
var taxRate = 0.25
var tax = income * taxRate
var netIncome = income - tax
```

### 5. Handle Edge Cases

```flowlang
function safeDivide(a, b) {
    if (b == 0) {
        print("Error: Division by zero")
        return 0
    }
    return a / b
}

var result = safeDivide(10, 0)  # Returns 0 instead of crashing
```

## Common Patterns

### 1. Counter Pattern

```flowlang
var count = 0
while (count < 10) {
    print("Count: " + count)
    count = count + 1
}
```

### 2. Accumulator Pattern

```flowlang
var sum = 0
var i = 1
while (i <= 10) {
    sum = sum + i
    i = i + 1
}
print("Sum of 1 to 10: " + sum)
```

### 3. Flag Pattern

```flowlang
var found = false
var searchValue = 42
var i = 0
var numbers = [1, 5, 42, 10, 3]

while (i < numbers.length and not found) {
    if (numbers[i] == searchValue) {
        found = true
        print("Found at index: " + i)
    }
    i = i + 1
}

if (not found) {
    print("Value not found")
}
```

### 4. State Machine Pattern

```flowlang
var state = "idle"
var input = "start"

if (state == "idle" and input == "start") {
    state = "running"
    print("Started")
} else if (state == "running" and input == "stop") {
    state = "stopped"
    print("Stopped")
} else if (state == "stopped" and input == "reset") {
    state = "idle"
    print("Reset")
}
```

### 5. Event-Driven Pattern

```flowlang
# Event handlers
on buttonClick {
    if (buttonId == "submit") {
        submitForm()
    } else if (buttonId == "cancel") {
        cancelForm()
    }
}

on dataUpdate {
    refreshDisplay()
    logUpdate(data)
}

# Helper functions
function submitForm() {
    print("Form submitted")
}

function cancelForm() {
    print("Form cancelled")
}

function refreshDisplay() {
    print("Display refreshed")
}

function logUpdate(data) {
    print("Update logged: " + data)
}
```

## Complete Example: Calculator

Here's a complete example that demonstrates many FlowLang features:

```flowlang
# Simple Calculator
function calculate(operation, a, b) {
    if (operation == "add") {
        return a + b
    } else if (operation == "subtract") {
        return a - b
    } else if (operation == "multiply") {
        return a * b
    } else if (operation == "divide") {
        if (b == 0) {
            print("Error: Division by zero")
            return 0
        }
        return a / b
    } else {
        print("Error: Unknown operation")
        return 0
    }
}

# Test the calculator
var result1 = calculate("add", 5, 3)
var result2 = calculate("subtract", 10, 4)
var result3 = calculate("multiply", 6, 7)
var result4 = calculate("divide", 15, 3)
var result5 = calculate("divide", 10, 0)

print("5 + 3 = " + result1)
print("10 - 4 = " + result2)
print("6 * 7 = " + result3)
print("15 / 3 = " + result4)
print("10 / 0 = " + result5)
```

**Output:**
```
5 + 3 = 8
10 - 4 = 6
6 * 7 = 42
15 / 3 = 5
Error: Division by zero
10 / 0 = 0
```

This tutorial covers the essential features of FlowLang. Practice with these examples and experiment with your own scripts to become proficient with the language!
