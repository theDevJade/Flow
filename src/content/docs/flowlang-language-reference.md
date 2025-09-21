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

FlowLang supports both implicit and explicit type declarations with a comprehensive type system:

### Primitive Types

| Type | Description | Example |
|------|-------------|---------|
| `String` | Text data | `"Hello"`, `'World'` |
| `Number` | Numeric data (integers and decimals) | `42`, `3.14`, `-10.5` |
| `Boolean` | True/false values | `true`, `false` |
| `Object` | Generic objects | Any custom type |
| `List` | Collections of items | `[1, 2, 3]`, `["a", "b", "c"]` |
| `Array` | Indexed collections | `[0, 1, 2]` |

### Complex Types

| Type | Description | Example |
|------|-------------|---------|
| `Vector3` | 3D vector with x, y, z components | `Vector3(1.0, 2.0, 3.0)` |
| `object` | Generic object type | Any custom type |

### Type Declaration

FlowLang supports both implicit and explicit type declarations:

```flowlang
# Implicit type inference
var name = "Hello World"
var age = 25
var isActive = true

# Explicit type declarations
var name: String = "Hello World"
var age: Number = 25
var isActive: Boolean = true
```

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

### Function Overloading

FlowLang supports function overloading based on parameter count:

```flowlang
function greet() {
    return "Hello!"
}

function greet(name) {
    return "Hello, " + name + "!"
}

function greet(name, age) {
    return "Hello, " + name + "! You are " + age + " years old."
}

# Different calls resolve to different functions
print(greet())                    # "Hello!"
print(greet("Alice"))             # "Hello, Alice!"
print(greet("Bob", 25))           # "Hello, Bob! You are 25 years old."
```

### Default Parameter Values

```flowlang
function createUser(name, age = 18, isActive = true) {
    return "User: " + name + ", Age: " + age + ", Active: " + isActive
}

# All these calls work
print(createUser("Alice"))                    # "User: Alice, Age: 18, Active: true"
print(createUser("Bob", 25))                  # "User: Bob, Age: 25, Active: true"
print(createUser("Charlie", 30, false))       # "User: Charlie, Age: 30, Active: false"
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

## Classes

### Basic Class Definition

```flowlang
class Person {
    var name: String
    var age: Number = 0
    
    function init(n, a) {
        this.name = n
        this.age = a
    }
    
    function greet() {
        return "Hello, I'm " + this.name
    }
    
    function getInfo() {
        return "Name: " + this.name + ", Age: " + this.age
    }
}
```

### Class Inheritance

```flowlang
class Student extends Person {
    var studentId: String
    var grade: Number = 0
    
    function init(n, a, id) {
        super.init(n, a)
        this.studentId = id
    }
    
    function study() {
        return this.name + " is studying"
    }
    
    function getStudentInfo() {
        return this.getInfo() + ", Student ID: " + this.studentId
    }
}
```

### Object Instantiation

```flowlang
# Create instances
var person = new Person()
person.init("Alice", 25)
print(person.greet())  # "Hello, I'm Alice"

var student = new Student()
student.init("Bob", 20, "S12345")
print(student.study())  # "Bob is studying"
print(student.getStudentInfo())  # "Name: Bob, Age: 20, Student ID: S12345"
```

### Property Access

```flowlang
# Access and modify properties
var person = new Person()
person.name = "Charlie"
person.age = 30

print(person.name)  # "Charlie"
print(person.age)   # 30
```

### Class Discovery Functions

```flowlang
# List all available classes
var classes = listClasses()
print("Available classes: " + classes)

# Get detailed class information
var classInfo = getClassInfo("Person")
print("Class info: " + classInfo)

# Check if a class exists
var exists = hasClass("Student")
print("Student class exists: " + exists)
```

## Events

### Event Definition

Events can be defined with parameters and descriptions:

```flowlang
# Register an event with parameters
event playerAction(playerName: text, action: text, x: number, y: number, z: number) "Triggered when a player performs an action"
```

### Event Handlers

```flowlang
# Register an event handler
on playerAction {
    print("Player " + playerName + " performed " + action + " at (" + x + ", " + y + ", " + z + ")")
}
```

### Event Triggering

```flowlang
# Trigger an event with parameters
trigger playerAction("Alice", "jump", 10.0, 20.0, 30.0)
```

### Event Discovery Functions

```flowlang
# List all available events
var events = listEvents()
print("Available events: " + events)

# Get detailed event information
var eventInfo = getEventInfo("playerAction")
print("Event info: " + eventInfo)

# Get event parameters
var parameters = getEventParameters("playerAction")
print("Parameters: " + parameters)
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

FlowLang provides a comprehensive error handling system with detailed error reporting, exact code locations, and helpful suggestions for fixing common issues.

### Enhanced Error System

The FlowLang parser includes an advanced error resolution system that provides:

- **Detailed error reporting** with exact line and column numbers
- **Source code context** showing the actual line causing the error
- **Intelligent suggestions** for fixing common issues
- **Error categorization** for better handling and debugging

### Error Types

The system categorizes errors into specific types:

- **SYNTAX_ERROR**: General syntax issues
- **LEXICAL_ERROR**: Invalid characters or tokens
- **RUNTIME_ERROR**: Errors during execution
- **TYPE_ERROR**: Type mismatch issues
- **UNDEFINED_VARIABLE**: Reference to non-existent variables
- **UNDEFINED_FUNCTION**: Call to non-existent functions
- **UNDEFINED_CLASS**: Reference to non-existent classes
- **ARGUMENT_MISMATCH**: Incorrect function arguments
- **UNTERMINATED_STRING**: Missing string delimiters
- **UNEXPECTED_TOKEN**: Unexpected token in context
- **MISSING_TOKEN**: Missing required token
- **INVALID_OPERATION**: Invalid operations
- **DIVISION_BY_ZERO**: Division by zero errors
- **INDEX_OUT_OF_BOUNDS**: Array/list access errors
- **NULL_REFERENCE**: Null pointer errors
- **INVALID_CAST**: Type casting errors
- **RECURSION_LIMIT_EXCEEDED**: Stack overflow from recursion
- **MEMORY_LIMIT_EXCEEDED**: Memory usage exceeded

### Error Context

Each error includes:

- **Exact location**: Line and column numbers
- **Source code**: The actual line causing the error
- **Error message**: Clear description of the issue
- **Suggestions**: Helpful tips for fixing the error
- **Context**: Additional information about the error

### Suggestion Engine

The system provides intelligent suggestions based on error type:

#### Syntax Errors
- Missing parentheses: "Check for missing opening/closing parenthesis"
- Missing braces: "Ensure all code blocks have matching braces"
- Missing brackets: "Ensure all array/list access has matching brackets"

#### Runtime Errors
- Division by zero: "Add a check to ensure the divisor is not zero"
- Null reference: "Check if the variable is initialized before use"
- Index out of bounds: "Check the array/list size before accessing elements"

#### Undefined References
- Variables: "Declare the variable with 'var variableName'"
- Functions: "Define the function with 'function functionName(...)'"
- Classes: "Define the class with 'class className'"

### Error Examples

#### 1. Unterminated String
```flowlang
var message = "Hello World
print(message)
```

**Error**: `UNTERMINATED_STRING: Unterminated string literal at 1:15`
**Suggestions**:
- Add a closing quote (") to terminate the string
- Check for escaped quotes within the string

#### 2. Missing Parenthesis
```flowlang
if x > 3 {
    print("x is greater than 3")
}
```

**Error**: `UNEXPECTED_TOKEN: Expected LEFT_PAREN, found 'x' at 1:4`
**Suggestions**:
- Check for missing opening parenthesis (
- Ensure all function calls have matching parentheses

#### 3. Undefined Variable
```flowlang
print(undefinedVariable)
```

**Error**: `UNDEFINED_VARIABLE: Variable 'undefinedVariable' not found`
**Suggestions**:
- Declare the variable with 'var undefinedVariable'
- Check for typos in the variable name
- Ensure the variable is in scope

#### 4. Division by Zero
```flowlang
var x = 5
var y = 0
var result = x / y
```

**Error**: `DIVISION_BY_ZERO: Division by zero`
**Suggestions**:
- Add a check to ensure the divisor is not zero
- Use conditional logic: if (divisor != 0) { ... }

## Built-in Functions

FlowLang provides several built-in functions:

### Output Functions

```flowlang
print("Hello, World!")           # Print to console
print("Value: " + value)         # Print with concatenation
```

### Event Functions

```flowlang
listEvents()                   # List all available events
getEventInfo("eventName")      # Get detailed event information
getEventParameters("eventName") # Get event parameters
```

### Class Functions

```flowlang
listClasses()                  # List all available classes
getClassInfo("ClassName")      # Get detailed class information
hasClass("ClassName")          # Check if class exists
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

### Type Conversion

FlowLang automatically handles type conversions where possible:

```flowlang
var number = "42" + 8          # "428" (string concatenation)
var sum = 42 + "8"             # 50 (numeric addition)
var text = "Value: " + 42      # "Value: 42" (string conversion)
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
