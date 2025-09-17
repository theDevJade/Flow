---
title: Language Reference
description: Complete FlowLang language syntax and features reference
---

# FlowLang Language Reference

This is the complete reference for FlowLang syntax, data types, operators, and language features.

## Overview

FlowLang is a lightweight, embeddable scripting language designed for rapid prototyping and automation. It features a simple syntax, natural language processing capabilities, and a comprehensive type system.

## Basic Syntax

FlowLang uses a C-style syntax with some unique features:

- **Statements** end with newlines (no semicolons required)
- **Case sensitive** language
- **Indentation** is significant for code blocks
- **Comments** start with `#`

## Data Types

FlowLang supports the following built-in data types:

### Primitive Types

| Type | Description | Example |
|------|-------------|---------|
| `number` | Double-precision floating point | `42`, `3.14`, `-10.5` |
| `text` | String of characters | `"Hello"`, `'World'` |
| `boolean` | True/false values | `true`, `false` |
| `null` | Null value | `null` |

### Complex Types

| Type | Description | Example |
|------|-------------|---------|
| `Vector3` | 3D vector with x, y, z components | `Vector3(1.0, 2.0, 3.0)` |
| `object` | Generic object type | Any custom type |

### Type Conversion

FlowLang automatically converts between compatible types:

```flowlang
var num = 42          # number
var str = "42"        # text
var bool = true       # boolean

# Automatic conversions
var result1 = num + str    # "42" + "42" = "4242"
var result2 = num + bool   # 42 + 1 = 43
var result3 = str + bool   # "42" + "true" = "42true"
```

## Variables

### Declaration and Assignment

```flowlang
# Declare and initialize
var name = "FlowLang"
var version = 1.0
var isActive = true

# Reassign values
name = "FlowLang Pro"
version = 2.0
isActive = false
```

### Variable Scope

Variables follow lexical scoping:

```flowlang
var globalVar = "I'm global"

function testScope() {
    var localVar = "I'm local"
    print(globalVar)  # Can access global
    print(localVar)   # Can access local
}

print(globalVar)  # Can access global
print(localVar)   # ERROR: localVar not in scope
```

## Operators

### Arithmetic Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `+` | Addition | `5 + 3` → `8` |
| `-` | Subtraction | `5 - 3` → `2` |
| `*` | Multiplication | `5 * 3` → `15` |
| `/` | Division | `15 / 3` → `5` |
| `%` | Modulo | `10 % 3` → `1` |

### Comparison Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equal to | `5 == 5` → `true` |
| `!=` | Not equal to | `5 != 3` → `true` |
| `<` | Less than | `3 < 5` → `true` |
| `>` | Greater than | `5 > 3` → `true` |
| `<=` | Less than or equal | `3 <= 3` → `true` |
| `>=` | Greater than or equal | `5 >= 3` → `true` |

### Logical Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `and` | Logical AND | `true and false` → `false` |
| `or` | Logical OR | `true or false` → `true` |
| `not` | Logical NOT | `not true` → `false` |

### String Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `+` | String concatenation | `"Hello" + "World"` → `"HelloWorld"` |

## Control Flow

### If Statements

```flowlang
# Basic if
if (condition) {
    print("Condition is true")
}

# If-else
if (age >= 18) {
    print("Adult")
} else {
    print("Minor")
}

# If-elseif-else
if (score >= 90) {
    print("A")
} else if (score >= 80) {
    print("B")
} else if (score >= 70) {
    print("C")
} else {
    print("F")
}
```

### While Loops

```flowlang
# Basic while loop
var i = 0
while (i < 10) {
    print("i = " + i)
    i = i + 1
}

# While with condition
var count = 0
while (count < 5) {
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

print("Factorial of 5: " + factorial(5))
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

### Triggering Events

Events are typically triggered from the host application, but can also be triggered from within scripts:

```flowlang
# Trigger event (usually done by host application)
trigger userLogin("john_doe", System.currentTimeMillis())
```

## Natural Language Processing

FlowLang includes a powerful preprocessor that converts natural language to code:

### Variable Assignment

| Natural Language | FlowLang Code |
|------------------|---------------|
| `set total to price plus tax` | `var total = price + tax` |
| `set result to a times b` | `var result = a * b` |
| `set difference to x minus y` | `var difference = x - y` |
| `set quotient to a divided by b` | `var quotient = a / b` |
| `set remainder to x modulo y` | `var remainder = x % y` |

### Comparison Operations

| Natural Language | FlowLang Code |
|------------------|---------------|
| `if score is greater than 50` | `if (score > 50)` |
| `if age is less than 18` | `if (age < 18)` |
| `if value is equal to zero` | `if (value == 0)` |
| `if name equals "admin"` | `if (name == "admin")` |
| `if status is not equal to active` | `if (status != "active")` |
| `if count is greater than or equal to 10` | `if (count >= 10)` |
| `if size is less than or equal to 100` | `if (size <= 100)` |

### Control Structures

| Natural Language | FlowLang Code |
|------------------|---------------|
| `if condition then action end if` | `if (condition) { action }` |
| `while condition then loop end while` | `while (condition) { loop }` |

### Noise Words

The preprocessor automatically removes noise words:

- `then` - removed from if/while statements
- `end` - removed from control structure endings

## Comments

```flowlang
# This is a single-line comment

# Comments can be used to explain code
var x = 10  # Initialize x to 10

# Comments are ignored by the parser
# var y = 20  # This line is commented out
```

## Error Handling

FlowLang provides basic error handling through exceptions:

```flowlang
# Division by zero
var result = 10 / 0  # Throws exception

# Undefined variable
print(undefinedVar)  # Throws exception

# Type conversion errors
var num = "not a number"
var result = num + 5  # May cause type conversion issues
```

### Common Error Types

- **SyntaxError**: Invalid syntax
- **TypeError**: Type conversion errors
- **RuntimeError**: Runtime execution errors
- **UndefinedVariableError**: Accessing undefined variables
- **FunctionNotFoundError**: Calling undefined functions

## Built-in Functions

FlowLang provides several built-in functions:

### Output Functions

```flowlang
print("Hello, World!")           # Print to console
print("Value: " + value)         # Print with concatenation
```

### Math Functions

```flowlang
# Basic math operations are built-in operators
var sum = a + b
var product = x * y
var quotient = a / b
var remainder = a % b
```

### String Functions

```flowlang
# String concatenation
var fullName = firstName + " " + lastName

# String conversion
var str = "The answer is " + 42
```

## Advanced Features

### Vector3 Operations

```flowlang
# Create Vector3
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

### Custom Types

FlowLang supports custom types registered by the host application:

```flowlang
# Use custom types (registered by host)
var user = User("john_doe", "John Doe")
var config = Config("production", true)

# Access properties and methods
print("Username: " + user.username)
print("Display name: " + user.displayName)
```

## Best Practices

1. **Use meaningful variable names**: `userCount` instead of `uc`
2. **Initialize variables**: Always give variables initial values
3. **Use comments**: Explain complex logic
4. **Handle errors**: Check for null values and type compatibility
5. **Use functions**: Break down complex logic into functions
6. **Follow naming conventions**: Use camelCase for variables and functions

## Examples

### Simple Calculator

```flowlang
function calculate(operation, a, b) {
    if (operation == "add") {
        return a + b
    } else if (operation == "subtract") {
        return a - b
    } else if (operation == "multiply") {
        return a * b
    } else if (operation == "divide") {
        return a / b
    } else {
        return 0
    }
}

var result = calculate("add", 5, 3)
print("5 + 3 = " + result)
```

### Number Guessing Game

```flowlang
var secretNumber = 42
var guess = 0
var attempts = 0

while (guess != secretNumber) {
    # In a real application, this would get input from user
    guess = 42  # Simulated guess
    attempts = attempts + 1
    
    if (guess < secretNumber) {
        print("Too low!")
    } else if (guess > secretNumber) {
        print("Too high!")
    } else {
        print("Correct! You took " + attempts + " attempts.")
    }
}
```

### Event-Driven System

```flowlang
# Event handlers
on buttonClick {
    print("Button clicked: " + buttonId)
    handleButtonClick(buttonId)
}

on dataUpdate {
    print("Data updated: " + data)
    refreshDisplay()
}

# Helper functions
function handleButtonClick(id) {
    if (id == "submit") {
        submitForm()
    } else if (id == "cancel") {
        cancelForm()
    }
}

function refreshDisplay() {
    print("Display refreshed with new data")
}
```
