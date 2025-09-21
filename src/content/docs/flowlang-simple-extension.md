---
title: Simple Extension System
description: FlowLang simple extension system for built-in plugin support
---

# FlowLang Simple Extension System

The Simple Extension system provides built-in support for extending FlowLang functionality through a plugin architecture. This system allows developers to create extensions that integrate seamlessly with the FlowLang environment.

## Overview

The Simple Extension system provides:
- **Built-in plugin support** without external dependencies
- **Type-safe extensions** with FlowLang type system integration
- **Hot-reloading** of extensions during development
- **API integration** with FlowLang core functionality
- **Event system integration** for extension communication

## Extension Architecture

### Extension Structure

```flowlang
# Define extension metadata
extension "MyExtension" {
    version: "1.0.0"
    author: "Developer Name"
    description: "A simple FlowLang extension"
    dependencies: ["Core", "Events"]
}
```

### Extension Entry Point

```flowlang
# Extension initialization
function initialize() {
    print("MyExtension initialized")
    
    # Register custom types
    registerCustomTypes()
    
    # Register custom functions
    registerCustomFunctions()
    
    # Set up event handlers
    setupEventHandlers()
}

# Extension cleanup
function cleanup() {
    print("MyExtension cleaned up")
    
    # Unregister types and functions
    unregisterCustomTypes()
    unregisterCustomFunctions()
}
```

## Custom Types

### Basic Type Registration

```flowlang
# Define custom type
class CustomData {
    var id: String
    var value: Number
    var metadata: Object
    
    function init(id, value) {
        this.id = id
        this.value = value
        this.metadata = {}
    }
    
    function getInfo() {
        return "ID: " + this.id + ", Value: " + this.value
    }
}

# Register type with FlowLang
function registerCustomTypes() {
    registerType("CustomData", CustomData, {
        fromString: parseCustomDataFromString,
        toString: convertCustomDataToString
    })
}
```

### Type Conversion Functions

```flowlang
# Parse from string
function parseCustomDataFromString(str) {
    var parts = str.split(":")
    if (parts.length >= 2) {
        return new CustomData(parts[0], parseNumber(parts[1]))
    }
    return null
}

# Convert to string
function convertCustomDataToString(data) {
    return data.id + ":" + data.value
}
```

### Advanced Type Features

```flowlang
# Type with validation
class ValidatedData {
    var email: String
    var age: Number
    
    function init(email, age) {
        if (not isValidEmail(email)) {
            throw "Invalid email format"
        }
        if (age < 0 or age > 150) {
            throw "Invalid age range"
        }
        
        this.email = email
        this.age = age
    }
    
    function isValidEmail(email) {
        return email.contains("@") and email.contains(".")
    }
}

# Register with validation
function registerCustomTypes() {
    registerType("ValidatedData", ValidatedData, {
        fromString: parseValidatedData,
        toString: convertValidatedData,
        validate: validateValidatedData
    })
}
```

## Custom Functions

### Basic Function Registration

```flowlang
# Define custom function
function customCalculation(a, b, operation = "add") {
    if (operation == "add") {
        return a + b
    } else if (operation == "multiply") {
        return a * b
    } else if (operation == "power") {
        return power(a, b)
    } else {
        return 0
    }
}

# Register function
function registerCustomFunctions() {
    registerFunction("customCalc", customCalculation, {
        parameters: [
            {name: "a", type: "Number"},
            {name: "b", type: "Number"},
            {name: "operation", type: "String", optional: true, default: "add"}
        ],
        returnType: "Number",
        description: "Performs custom calculations"
    })
}
```

### Function with Error Handling

```flowlang
# Function with comprehensive error handling
function safeDivide(a, b) {
    try {
        if (b == 0) {
            throw "Division by zero"
        }
        return a / b
    } catch (error) {
        logError("safeDivide error: " + error)
        return 0
    }
}

# Register with error handling
function registerCustomFunctions() {
    registerFunction("safeDivide", safeDivide, {
        parameters: [
            {name: "a", type: "Number"},
            {name: "b", type: "Number"}
        ],
        returnType: "Number",
        description: "Safely divides two numbers",
        errorHandling: true
    })
}
```

### Async Functions

```flowlang
# Async function for external API calls
async function fetchData(url) {
    try {
        var response = await httpGet(url)
        return parseJSON(response)
    } catch (error) {
        logError("fetchData error: " + error)
        return null
    }
}

# Register async function
function registerCustomFunctions() {
    registerAsyncFunction("fetchData", fetchData, {
        parameters: [
            {name: "url", type: "String"}
        ],
        returnType: "Object",
        description: "Fetches data from URL"
    })
}
```

## Event System Integration

### Custom Events

```flowlang
# Define custom events
function setupEventHandlers() {
    # Register custom event
    registerEvent("dataProcessed", {
        parameters: [
            {name: "dataId", type: "String"},
            {name: "result", type: "Object"},
            {name: "timestamp", type: "Number"}
        ],
        description: "Triggered when data processing completes"
    })
    
    # Register event handler
    on dataProcessed {
        print("Data " + dataId + " processed at " + timestamp)
        updateUI(result)
    }
}

# Trigger custom events
function processData(data) {
    var result = performProcessing(data)
    var timestamp = getCurrentTime()
    
    trigger dataProcessed(data.id, result, timestamp)
}
```

### Event Filtering

```flowlang
# Event with filtering
function setupEventHandlers() {
    # Register filtered event
    registerEvent("userAction", {
        parameters: [
            {name: "userId", type: "String"},
            {name: "action", type: "String"},
            {name: "data", type: "Object"}
        ],
        filters: [
            {parameter: "userId", condition: "not null"},
            {parameter: "action", condition: "in ['click', 'hover', 'scroll']"}
        ]
    })
    
    # Handle filtered events
    on userAction {
        if (action == "click") {
            handleClick(userId, data)
        } else if (action == "hover") {
            handleHover(userId, data)
        }
    }
}
```

## Configuration System

### Extension Configuration

```flowlang
# Extension configuration
var extensionConfig = {
    name: "MyExtension",
    version: "1.0.0",
    settings: {
        debugMode: false,
        logLevel: "info",
        maxRetries: 3,
        timeout: 5000
    },
    features: {
        customTypes: true,
        customFunctions: true,
        eventHandling: true,
        asyncSupport: true
    }
}

# Load configuration
function loadConfiguration() {
    var config = loadExtensionConfig("MyExtension")
    if (config != null) {
        extensionConfig = mergeConfig(extensionConfig, config)
    }
}
```

### Runtime Configuration

```flowlang
# Runtime configuration updates
function updateConfiguration(newSettings) {
    extensionConfig.settings = mergeConfig(extensionConfig.settings, newSettings)
    saveExtensionConfig("MyExtension", extensionConfig)
    
    # Apply configuration changes
    applyConfigurationChanges(newSettings)
}

# Apply configuration changes
function applyConfigurationChanges(settings) {
    if (settings.debugMode != null) {
        setDebugMode(settings.debugMode)
    }
    
    if (settings.logLevel != null) {
        setLogLevel(settings.logLevel)
    }
}
```

## Examples

### Data Processing Extension

```flowlang
extension "DataProcessor" {
    version: "1.0.0"
    author: "Data Team"
    description: "Advanced data processing capabilities"
}

# Custom data types
class DataPoint {
    var timestamp: Number
    var value: Number
    var category: String
    
    function init(timestamp, value, category = "default") {
        this.timestamp = timestamp
        this.value = value
        this.category = category
    }
}

class DataSet {
    var points: Array = []
    var metadata: Object = {}
    
    function addPoint(point) {
        this.points = this.points + [point]
    }
    
    function getAverage() {
        if (this.points.length == 0) return 0
        
        var sum = 0
        var i = 0
        while (i < this.points.length) {
            sum = sum + this.points[i].value
            i = i + 1
        }
        
        return sum / this.points.length
    }
}

# Custom functions
function processData(dataSet, operation = "average") {
    if (operation == "average") {
        return dataSet.getAverage()
    } else if (operation == "sum") {
        var sum = 0
        var i = 0
        while (i < dataSet.points.length) {
            sum = sum + dataSet.points[i].value
            i = i + 1
        }
        return sum
    } else if (operation == "count") {
        return dataSet.points.length
    }
    
    return 0
}

function filterData(dataSet, category) {
    var filtered = new DataSet()
    var i = 0
    
    while (i < dataSet.points.length) {
        if (dataSet.points[i].category == category) {
            filtered.addPoint(dataSet.points[i])
        }
        i = i + 1
    }
    
    return filtered
}

# Extension initialization
function initialize() {
    # Register types
    registerType("DataPoint", DataPoint)
    registerType("DataSet", DataSet)
    
    # Register functions
    registerFunction("processData", processData, {
        parameters: [
            {name: "dataSet", type: "DataSet"},
            {name: "operation", type: "String", optional: true, default: "average"}
        ],
        returnType: "Number"
    })
    
    registerFunction("filterData", filterData, {
        parameters: [
            {name: "dataSet", type: "DataSet"},
            {name: "category", type: "String"}
        ],
        returnType: "DataSet"
    })
    
    # Set up events
    registerEvent("dataProcessed", {
        parameters: [
            {name: "dataSet", type: "DataSet"},
            {name: "result", type: "Number"}
        ]
    })
}
```

### UI Extension

```flowlang
extension "UIComponents" {
    version: "1.0.0"
    author: "UI Team"
    description: "UI component library"
}

# UI Component types
class Button {
    var text: String
    var position: Vector3
    var size: Vector3
    var onClick: String
    
    function init(text, position, size) {
        this.text = text
        this.position = position
        this.size = size
        this.onClick = ""
    }
    
    function click() {
        if (this.onClick != "") {
            trigger buttonClicked(this.text, this.position)
        }
    }
}

class TextField {
    var placeholder: String
    var value: String
    var position: Vector3
    var size: Vector3
    
    function init(placeholder, position, size) {
        this.placeholder = placeholder
        this.value = ""
        this.position = position
        this.size = size
    }
    
    function setValue(value) {
        this.value = value
        trigger textChanged(this.value)
    }
}

# UI Functions
function createButton(text, x, y, width, height) {
    var button = new Button(text, Vector3(x, y, 0), Vector3(width, height, 0))
    return button
}

function createTextField(placeholder, x, y, width, height) {
    var textField = new TextField(placeholder, Vector3(x, y, 0), Vector3(width, height, 0))
    return textField
}

function layoutComponents(components, direction = "vertical", spacing = 10) {
    var currentY = 0
    var i = 0
    
    while (i < components.length) {
        var component = components[i]
        component.position.y = currentY
        currentY = currentY + component.size.y + spacing
        i = i + 1
    }
}

# Extension initialization
function initialize() {
    # Register types
    registerType("Button", Button)
    registerType("TextField", TextField)
    
    # Register functions
    registerFunction("createButton", createButton, {
        parameters: [
            {name: "text", type: "String"},
            {name: "x", type: "Number"},
            {name: "y", type: "Number"},
            {name: "width", type: "Number"},
            {name: "height", type: "Number"}
        ],
        returnType: "Button"
    })
    
    registerFunction("createTextField", createTextField, {
        parameters: [
            {name: "placeholder", type: "String"},
            {name: "x", type: "Number"},
            {name: "y", type: "Number"},
            {name: "width", type: "Number"},
            {name: "height", type: "Number"}
        ],
        returnType: "TextField"
    })
    
    registerFunction("layoutComponents", layoutComponents, {
        parameters: [
            {name: "components", type: "Array"},
            {name: "direction", type: "String", optional: true, default: "vertical"},
            {name: "spacing", type: "Number", optional: true, default: 10}
        ],
        returnType: "Void"
    })
    
    # Set up events
    registerEvent("buttonClicked", {
        parameters: [
            {name: "text", type: "String"},
            {name: "position", type: "Vector3"}
        ]
    })
    
    registerEvent("textChanged", {
        parameters: [
            {name: "value", type: "String"}
        ]
    })
}
```

## Best Practices

1. **Use descriptive names**: Choose clear, meaningful names for types and functions
2. **Handle errors gracefully**: Implement proper error handling and logging
3. **Document everything**: Add comprehensive documentation for all public APIs
4. **Use type safety**: Leverage FlowLang's type system for better reliability
5. **Test thoroughly**: Create comprehensive tests for all extension functionality
6. **Version your extensions**: Use semantic versioning for extension updates
7. **Handle configuration**: Provide flexible configuration options
8. **Clean up resources**: Properly dispose of resources in cleanup functions

This Simple Extension system enables developers to extend FlowLang functionality while maintaining type safety and integration with the core system.
