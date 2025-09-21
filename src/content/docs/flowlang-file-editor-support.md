---
title: .flowlang File Editor Support
description: Integrated support for .flowlang files in the file editor
---

# .flowlang File Editor Support

FlowLang provides comprehensive integrated support for `.flowlang` files in the file editor, including syntax highlighting, IntelliSense, error detection, and real-time execution capabilities.

## Overview

The `.flowlang` file editor support includes:
- **Syntax highlighting** for FlowLang code
- **IntelliSense** and code completion
- **Real-time error detection** and validation
- **Code formatting** and auto-completion
- **Integrated execution** and debugging
- **File management** and project support

## File Recognition

### File Extension Support

The editor automatically recognizes `.flowlang` files and applies appropriate syntax highlighting and language features:

```
project/
├── scripts/
│   ├── main.flowlang
│   ├── utils.flowlang
│   └── config.flowlang
├── modules/
│   ├── math.flowlang
│   └── graphics.flowlang
└── tests/
    └── test_runner.flowlang
```

### Language Detection

```flowlang
# .flowlang files are automatically detected
# No special headers or markers required

# Standard FlowLang syntax
var message = "Hello, FlowLang!"
print(message)

# Classes and functions work normally
class MyClass {
    var value: Number = 0
    
    function getValue() {
        return this.value
    }
}
```

## Syntax Highlighting

### Keyword Highlighting

The editor provides syntax highlighting for all FlowLang keywords:

```flowlang
# Keywords highlighted in different colors
var name = "FlowLang"           # 'var' in keyword color
function greet(name) {          # 'function' in keyword color
    if (name != null) {         # 'if' in keyword color
        return "Hello, " + name
    } else {                    # 'else' in keyword color
        return "Hello, World!"
    }
}

# Control structures
while (condition) {             # 'while' in keyword color
    # Loop body
}

for (var i = 0; i < 10; i++) {  # 'for' in keyword color
    # Loop body
}
```

### Type Highlighting

```flowlang
# Type annotations highlighted
var name: String = "Alice"      # 'String' in type color
var age: Number = 25            # 'Number' in type color
var active: Boolean = true      # 'Boolean' in type color
var position: Vector3 = Vector3(1, 2, 3)  # 'Vector3' in type color
```

### String and Comment Highlighting

```flowlang
# Strings highlighted
var message = "Hello, World!"   # String in string color
var path = 'C:\\Program Files'  # String in string color

# Comments highlighted
# This is a single-line comment
var value = 42  # Inline comment

/*
 * Multi-line comments
 * are also supported
 */
```

## IntelliSense and Code Completion

### Auto-completion

The editor provides intelligent code completion for:

- **Keywords**: `var`, `function`, `class`, `if`, `while`, etc.
- **Built-in functions**: `print()`, `listEvents()`, `getClassInfo()`, etc.
- **Type names**: `String`, `Number`, `Boolean`, `Vector3`, etc.
- **Variable names**: Previously declared variables
- **Function names**: Defined functions and methods
- **Class members**: Properties and methods of classes

```flowlang
# Auto-completion examples
var name = "Alice"
var age = 25

# Typing 'na' will suggest 'name'
print(na|)  # Cursor position shows completion

# Typing 'ag' will suggest 'age'
if (ag| > 18) {  # Cursor position shows completion
    # Code here
}

# Function completion
function calculate(a, b) {
    return a + b
}

# Typing 'cal' will suggest 'calculate'
var result = cal|(5, 3)  # Cursor position shows completion
```

### Parameter Hints

```flowlang
# Parameter hints for functions
function createUser(name, age, email) {
    # Function body
}

# When calling the function, hints show parameter names
createUser(|)  # Shows: name, age, email
createUser("Alice", |)  # Shows: age, email
createUser("Alice", 25, |)  # Shows: email
```

### Type Information

```flowlang
# Hover over variables to see type information
var name: String = "Alice"  # Hover shows: String
var age: Number = 25        # Hover shows: Number

# Hover over function calls to see signature
print(|)  # Hover shows: print(message: String) -> Void
```

## Error Detection and Validation

### Real-time Error Highlighting

The editor provides real-time error detection and highlighting:

```flowlang
# Syntax errors highlighted in red
var message = "Hello World  # Missing closing quote
print(message)

# Type errors highlighted
var name: String = 42  # Type mismatch error

# Undefined variable errors
print(undefinedVariable)  # Variable not defined error

# Missing semicolon errors (if enabled)
var x = 10  # Missing semicolon warning
```

### Error Messages

```flowlang
# Detailed error messages in tooltips
var result = 10 / 0  # Error: Division by zero
print(undefinedVar)  # Error: Variable 'undefinedVar' not found

# Function call errors
print(tooMany, arguments, here)  # Error: Too many arguments for function 'print'
```

### Warning Indicators

```flowlang
# Warnings shown with yellow underlines
var unusedVariable = 42  # Warning: Variable declared but not used

# Unreachable code warnings
function test() {
    return 42
    print("This will never execute")  # Warning: Unreachable code
}
```

## Code Formatting

### Auto-formatting

The editor can automatically format FlowLang code:

```flowlang
# Before formatting
var name="Alice";var age=25;if(age>18){print("Adult")}else{print("Minor")}

# After auto-formatting
var name = "Alice"
var age = 25
if (age > 18) {
    print("Adult")
} else {
    print("Minor")
}
```

### Indentation

```flowlang
# Automatic indentation for code blocks
function processData(data) {
    var result = []
    var i = 0
    
    while (i < data.length) {
        if (data[i] > 0) {
            result = result + [data[i]]
        }
        i = i + 1
    }
    
    return result
}
```

## Integrated Execution

### Run Code in Editor

```flowlang
# Right-click or use keyboard shortcut to run code
var message = "Hello from FlowLang!"
print(message)

# Results appear in integrated console
# Output: Hello from FlowLang!
```

### Debug Mode

```flowlang
# Set breakpoints by clicking in the gutter
function calculate(a, b) {
    var sum = a + b        # Breakpoint here
    var product = a * b    # Breakpoint here
    return sum + product
}

# Step through code line by line
var result = calculate(5, 3)
print(result)
```

### Variable Inspection

```flowlang
# Hover over variables to see current values
var name = "Alice"
var age = 25
var isActive = true

# In debug mode, variables panel shows all current values
```

## File Management

### Project Structure

```
my-project/
├── .flowlang/
│   ├── config.json
│   └── extensions/
├── src/
│   ├── main.flowlang
│   ├── utils.flowlang
│   └── modules/
│       ├── math.flowlang
│       └── graphics.flowlang
├── tests/
│   └── test_runner.flowlang
└── README.md
```

### File Templates

```flowlang
# New .flowlang file template
# FlowLang Script
# Created: [Date]

# Main script entry point
function main() {
    print("Hello, FlowLang!")
}

# Run main function
main()
```

### Import/Export

```flowlang
# Import other .flowlang files
import "utils.flowlang"
import "modules/math.flowlang"

# Use imported functions
var result = add(5, 3)  # From utils.flowlang
var power = square(4)   # From modules/math.flowlang
```

## Configuration

### Editor Settings

```json
{
    "flowlang.editor": {
        "syntaxHighlighting": true,
        "intelliSense": true,
        "errorDetection": true,
        "autoFormatting": true,
        "indentation": "spaces",
        "indentSize": 4,
        "lineNumbers": true,
        "wordWrap": false,
        "showWhitespace": false
    }
}
```

### Language Server Settings

```json
{
    "flowlang.languageServer": {
        "enabled": true,
        "diagnostics": true,
        "completion": true,
        "hover": true,
        "signatureHelp": true,
        "formatting": true,
        "codeActions": true
    }
}
```

## Keyboard Shortcuts

### Common Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Space` | Trigger IntelliSense |
| `Ctrl+Shift+P` | Command palette |
| `F5` | Run current file |
| `F9` | Toggle breakpoint |
| `F10` | Step over |
| `F11` | Step into |
| `Shift+F11` | Step out |
| `Ctrl+K Ctrl+F` | Format document |
| `Ctrl+/` | Toggle comment |
| `Ctrl+D` | Select next occurrence |
| `Ctrl+Shift+K` | Delete line |

### Custom Shortcuts

```json
{
    "key": "ctrl+alt+r",
    "command": "flowlang.runFile",
    "when": "editorLangId == flowlang"
},
{
    "key": "ctrl+alt+d",
    "command": "flowlang.debugFile",
    "when": "editorLangId == flowlang"
}
```

## Extensions and Plugins

### Language Extension

```json
{
    "name": "flowlang-language",
    "displayName": "FlowLang Language Support",
    "description": "Provides FlowLang language support",
    "version": "1.0.0",
    "engines": {
        "vscode": "^1.60.0"
    },
    "contributes": {
        "languages": [
            {
                "id": "flowlang",
                "aliases": ["FlowLang", "flowlang"],
                "extensions": [".flowlang"],
                "configuration": "./language-configuration.json"
            }
        ],
        "grammars": [
            {
                "language": "flowlang",
                "scopeName": "source.flowlang",
                "path": "./syntaxes/flowlang.tmGrammar.json"
            }
        ]
    }
}
```

## Best Practices

1. **Use descriptive file names**: Choose clear, meaningful names for `.flowlang` files
2. **Organize code logically**: Group related functions and classes together
3. **Add comments**: Document complex logic and important functions
4. **Use consistent formatting**: Follow the auto-formatter or establish style guidelines
5. **Handle errors**: Use proper error handling and validation
6. **Test frequently**: Run code often to catch errors early
7. **Use version control**: Track changes to `.flowlang` files
8. **Keep files focused**: Each file should have a single, clear purpose

This integrated file editor support makes FlowLang development efficient and enjoyable, with all the features developers expect from modern code editors.
