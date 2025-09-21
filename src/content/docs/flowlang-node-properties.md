---
title: Node Properties
description: FlowLang node properties system for visual graph editing
---

# FlowLang Node Properties

The Node Properties system in FlowLang provides a powerful way to define and manage properties for visual graph nodes, enabling dynamic behavior and data flow in graph-based applications.

## Overview

Node Properties allow you to:
- Define custom properties for graph nodes
- Set property types and validation rules
- Create dynamic connections between nodes
- Implement data flow and transformations
- Build interactive visual programming interfaces

## Basic Property Definition

### Simple Properties

```flowlang
# Define a basic property
property nodeName: String = "DefaultNode"

# Define a numeric property with default value
property nodeValue: Number = 0

# Define a boolean property
property isActive: Boolean = true
```

### Property Types

FlowLang supports various property types for nodes:

| Type | Description | Example |
|------|-------------|---------|
| `String` | Text data | `"Hello World"` |
| `Number` | Numeric values | `42`, `3.14` |
| `Boolean` | True/false values | `true`, `false` |
| `Vector3` | 3D coordinates | `Vector3(1, 2, 3)` |
| `Color` | Color values | `Color(255, 0, 0)` |
| `Array` | List of values | `[1, 2, 3]` |
| `Object` | Custom objects | `{x: 1, y: 2}` |

## Advanced Property Features

### Property Validation

```flowlang
# Property with validation
property nodeValue: Number = 0 {
    min: 0
    max: 100
    step: 1
}

# String property with constraints
property nodeName: String = "Node" {
    minLength: 1
    maxLength: 50
    pattern: "^[A-Za-z][A-Za-z0-9_]*$"
}
```

### Property Groups

```flowlang
# Group related properties
group "Transform" {
    property position: Vector3 = Vector3(0, 0, 0)
    property rotation: Vector3 = Vector3(0, 0, 0)
    property scale: Vector3 = Vector3(1, 1, 1)
}

group "Appearance" {
    property color: Color = Color(255, 255, 255)
    property opacity: Number = 1.0 {
        min: 0
        max: 1
    }
}
```

### Computed Properties

```flowlang
# Property that depends on other properties
property area: Number = 0 {
    computed: true
    depends: ["width", "height"]
    formula: "width * height"
}

property perimeter: Number = 0 {
    computed: true
    depends: ["width", "height"]
    formula: "2 * (width + height)"
}
```

## Property Events

### Property Change Events

```flowlang
# Listen for property changes
on propertyChanged(propertyName, oldValue, newValue) {
    print("Property " + propertyName + " changed from " + oldValue + " to " + newValue)
    
    # Update dependent properties
    if (propertyName == "width" or propertyName == "height") {
        updateComputedProperties()
    }
}

# Property validation events
on propertyValidationFailed(propertyName, value, error) {
    print("Validation failed for " + propertyName + ": " + error)
    # Handle validation error
}
```

### Custom Property Events

```flowlang
# Define custom property events
event propertyUpdated(nodeId: text, propertyName: text, value: any) "Property was updated"

# Trigger custom events
function updateProperty(name, value) {
    var oldValue = getProperty(name)
    setProperty(name, value)
    trigger propertyUpdated(nodeId, name, value)
}
```

## Property Binding

### One-Way Binding

```flowlang
# Bind property to another node's property
property sourceValue: Number = 0
property targetValue: Number = 0 {
    bind: "sourceNode.sourceValue"
    mode: "oneWay"
}
```

### Two-Way Binding

```flowlang
# Two-way binding between properties
property inputValue: Number = 0 {
    bind: "outputNode.outputValue"
    mode: "twoWay"
}
```

### Expression Binding

```flowlang
# Bind to expression result
property result: Number = 0 {
    bind: "input1 + input2 * multiplier"
    mode: "expression"
}
```

## Property Serialization

### Save/Load Properties

```flowlang
# Save node properties to JSON
function saveNodeProperties(nodeId) {
    var properties = getNodeProperties(nodeId)
    var json = toJSON(properties)
    return json
}

# Load node properties from JSON
function loadNodeProperties(nodeId, jsonData) {
    var properties = fromJSON(jsonData)
    setNodeProperties(nodeId, properties)
}
```

### Property Templates

```flowlang
# Define property templates
template "BasicNode" {
    property name: String = "Node"
    property position: Vector3 = Vector3(0, 0, 0)
    property isActive: Boolean = true
}

template "DataNode" {
    property data: Array = []
    property dataType: String = "any"
    property maxItems: Number = 100
}

# Apply template to node
applyTemplate(nodeId, "BasicNode")
```

## Examples

### Transform Node

```flowlang
class TransformNode {
    # Position properties
    property x: Number = 0 {
        min: -1000
        max: 1000
    }
    property y: Number = 0 {
        min: -1000
        max: 1000
    }
    property z: Number = 0 {
        min: -1000
        max: 1000
    }
    
    # Rotation properties
    property rotationX: Number = 0 {
        min: 0
        max: 360
    }
    property rotationY: Number = 0 {
        min: 0
        max: 360
    }
    property rotationZ: Number = 0 {
        min: 0
        max: 360
    }
    
    # Scale properties
    property scaleX: Number = 1 {
        min: 0.1
        max: 10
    }
    property scaleY: Number = 1 {
        min: 0.1
        max: 10
    }
    property scaleZ: Number = 1 {
        min: 0.1
        max: 10
    }
    
    # Computed properties
    property position: Vector3 = Vector3(0, 0, 0) {
        computed: true
        depends: ["x", "y", "z"]
        formula: "Vector3(x, y, z)"
    }
    
    property rotation: Vector3 = Vector3(0, 0, 0) {
        computed: true
        depends: ["rotationX", "rotationY", "rotationZ"]
        formula: "Vector3(rotationX, rotationY, rotationZ)"
    }
    
    property scale: Vector3 = Vector3(1, 1, 1) {
        computed: true
        depends: ["scaleX", "scaleY", "scaleZ"]
        formula: "Vector3(scaleX, scaleY, scaleZ)"
    }
}
```

### Data Processing Node

```flowlang
class DataProcessingNode {
    # Input properties
    property inputData: Array = []
    property dataType: String = "number"
    property processingMode: String = "sum"
    
    # Processing parameters
    property multiplier: Number = 1 {
        min: 0.1
        max: 100
    }
    property offset: Number = 0 {
        min: -1000
        max: 1000
    }
    
    # Output properties
    property outputData: Array = [] {
        computed: true
        depends: ["inputData", "processingMode", "multiplier", "offset"]
        formula: "processData(inputData, processingMode, multiplier, offset)"
    }
    
    property resultCount: Number = 0 {
        computed: true
        depends: ["outputData"]
        formula: "outputData.length"
    }
    
    # Processing function
    function processData(data, mode, mult, off) {
        var result = []
        var i = 0
        
        while (i < data.length) {
            var value = data[i]
            
            if (mode == "sum") {
                value = value + off
            } else if (mode == "multiply") {
                value = value * mult
            } else if (mode == "both") {
                value = (value * mult) + off
            }
            
            result = result + [value]
            i = i + 1
        }
        
        return result
    }
}
```

### UI Control Node

```flowlang
class UIControlNode {
    # Basic properties
    property label: String = "Control"
    property isVisible: Boolean = true
    property isEnabled: Boolean = true
    
    # Position and size
    property x: Number = 0
    property y: Number = 0
    property width: Number = 100 {
        min: 10
        max: 1000
    }
    property height: Number = 30 {
        min: 10
        max: 1000
    }
    
    # Style properties
    property backgroundColor: Color = Color(240, 240, 240)
    property textColor: Color = Color(0, 0, 0)
    property fontSize: Number = 12 {
        min: 8
        max: 72
    }
    
    # Computed properties
    property bounds: Object = {} {
        computed: true
        depends: ["x", "y", "width", "height"]
        formula: "{x: x, y: y, width: width, height: height}"
    }
    
    # Event properties
    property onClick: String = ""
    property onHover: String = ""
    property onValueChange: String = ""
}
```

## Best Practices

1. **Use descriptive property names**: Choose clear, meaningful names for properties
2. **Set appropriate validation rules**: Define min/max values and constraints
3. **Group related properties**: Use property groups to organize related settings
4. **Use computed properties**: For values that depend on other properties
5. **Handle property events**: Listen for changes and update dependent properties
6. **Document property purposes**: Add comments explaining what each property does
7. **Use templates**: Create reusable property templates for common node types
8. **Validate input**: Always validate property values before setting them

## Integration with Graph Editor

Node Properties integrate seamlessly with the FlowLang graph editor:

- **Property panels** automatically generated from property definitions
- **Real-time updates** when properties change
- **Visual feedback** for validation errors
- **Undo/redo support** for property changes
- **Copy/paste** of property values between nodes
- **Search and filter** properties across nodes

This system provides a powerful foundation for building interactive, data-driven visual programming interfaces with FlowLang.
