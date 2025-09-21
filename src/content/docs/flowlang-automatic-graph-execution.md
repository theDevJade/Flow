---
title: Automatic Graph Execution
description: FlowLang automatic graph execution system for real-time processing
---

# FlowLang Automatic Graph Execution

The Automatic Graph Execution system in FlowLang enables real-time, event-driven processing of visual graphs without manual intervention. This system automatically executes graph nodes based on data changes, events, and dependencies.

## Overview

Automatic Graph Execution provides:
- **Real-time processing** of graph changes
- **Event-driven execution** based on data flow
- **Dependency resolution** for proper execution order
- **Performance optimization** with selective execution
- **Error handling** and recovery mechanisms

## Execution Modes

### Continuous Execution

```flowlang
# Enable continuous execution for a graph
setExecutionMode(graphId, "continuous")

# Set execution interval (milliseconds)
setExecutionInterval(graphId, 16)  # ~60 FPS

# Start automatic execution
startAutomaticExecution(graphId)
```

### Event-Driven Execution

```flowlang
# Execute only when specific events occur
setExecutionMode(graphId, "eventDriven")

# Register execution triggers
addExecutionTrigger(graphId, "dataChanged")
addExecutionTrigger(graphId, "propertyUpdated")
addExecutionTrigger(graphId, "userInput")
```

### On-Demand Execution

```flowlang
# Execute only when explicitly requested
setExecutionMode(graphId, "onDemand")

# Manual execution
executeGraph(graphId)

# Execute specific nodes
executeNodes(graphId, ["node1", "node2", "node3"])
```

## Execution Dependencies

### Node Dependencies

```flowlang
# Define node dependencies
addNodeDependency(sourceNodeId, targetNodeId)

# Define multiple dependencies
addNodeDependencies(nodeId, ["dep1", "dep2", "dep3"])

# Remove dependency
removeNodeDependency(sourceNodeId, targetNodeId)
```

### Data Dependencies

```flowlang
# Node depends on specific data properties
addDataDependency(nodeId, "inputData", "sourceNode.outputData")

# Property dependencies
addPropertyDependency(nodeId, "result", "input1.value")
addPropertyDependency(nodeId, "result", "input2.value")
```

### Conditional Dependencies

```flowlang
# Conditional execution based on data
addConditionalDependency(nodeId, "inputValue > 0", "processNode")

# Complex conditions
addConditionalDependency(nodeId, "inputValue > threshold and isEnabled", "executeNode")
```

## Execution Scheduling

### Priority-Based Execution

```flowlang
# Set execution priority for nodes
setNodePriority(nodeId, 10)  # Higher number = higher priority

# Priority levels
setNodePriority(nodeId, "high")    # Critical nodes
setNodePriority(nodeId, "normal")  # Standard nodes
setNodePriority(nodeId, "low")     # Background processing
```

### Batch Execution

```flowlang
# Group nodes for batch execution
createExecutionBatch("dataProcessing", ["node1", "node2", "node3"])

# Execute batch
executeBatch("dataProcessing")

# Set batch execution order
setBatchExecutionOrder("dataProcessing", ["node1", "node2", "node3"])
```

### Parallel Execution

```flowlang
# Enable parallel execution for independent nodes
enableParallelExecution(graphId, true)

# Set maximum parallel threads
setMaxParallelThreads(graphId, 4)

# Mark nodes as parallel-safe
setNodeParallelSafe(nodeId, true)
```

## Execution Events

### Execution Lifecycle Events

```flowlang
# Graph execution started
on graphExecutionStarted(graphId) {
    print("Graph " + graphId + " execution started")
    setExecutionStatus(graphId, "running")
}

# Graph execution completed
on graphExecutionCompleted(graphId, duration) {
    print("Graph " + graphId + " completed in " + duration + "ms")
    setExecutionStatus(graphId, "completed")
}

# Graph execution failed
on graphExecutionFailed(graphId, error) {
    print("Graph " + graphId + " failed: " + error)
    setExecutionStatus(graphId, "failed")
    handleExecutionError(graphId, error)
}
```

### Node Execution Events

```flowlang
# Node execution started
on nodeExecutionStarted(nodeId) {
    print("Node " + nodeId + " execution started")
    setNodeStatus(nodeId, "running")
}

# Node execution completed
on nodeExecutionCompleted(nodeId, result) {
    print("Node " + nodeId + " completed with result: " + result)
    setNodeStatus(nodeId, "completed")
    updateNodeOutput(nodeId, result)
}

# Node execution failed
on nodeExecutionFailed(nodeId, error) {
    print("Node " + nodeId + " failed: " + error)
    setNodeStatus(nodeId, "failed")
    handleNodeError(nodeId, error)
}
```

## Performance Optimization

### Execution Caching

```flowlang
# Enable execution caching
enableExecutionCache(graphId, true)

# Cache node results
cacheNodeResult(nodeId, inputHash, result)

# Check if result is cached
var cachedResult = getCachedResult(nodeId, inputHash)
if (cachedResult != null) {
    return cachedResult
}
```

### Selective Execution

```flowlang
# Only execute changed nodes
enableSelectiveExecution(graphId, true)

# Mark node as changed
markNodeChanged(nodeId)

# Check if node needs execution
if (nodeNeedsExecution(nodeId)) {
    executeNode(nodeId)
}
```

### Execution Throttling

```flowlang
# Throttle execution frequency
setExecutionThrottle(graphId, 100)  # Max 10 executions per second

# Throttle specific nodes
setNodeExecutionThrottle(nodeId, 50)  # Max 20 executions per second
```

## Error Handling

### Execution Error Recovery

```flowlang
# Set error recovery strategy
setErrorRecoveryStrategy(graphId, "retry")

# Retry failed nodes
retryFailedNodes(graphId)

# Skip failed nodes and continue
setErrorRecoveryStrategy(graphId, "skip")

# Stop execution on error
setErrorRecoveryStrategy(graphId, "stop")
```

### Error Logging

```flowlang
# Enable detailed error logging
enableExecutionLogging(graphId, true)

# Log execution errors
on executionError(graphId, nodeId, error) {
    logError("Graph: " + graphId + ", Node: " + nodeId + ", Error: " + error)
    saveErrorToFile(error)
}
```

## Examples

### Real-Time Data Processing Graph

```flowlang
# Create data processing graph
var graphId = createGraph("dataProcessing")

# Add input node
var inputNode = addNode(graphId, "input", {
    dataSource: "sensorData",
    updateFrequency: 100
})

# Add processing nodes
var filterNode = addNode(graphId, "filter", {
    filterType: "lowpass",
    cutoffFrequency: 0.5
})

var transformNode = addNode(graphId, "transform", {
    operation: "normalize",
    range: [0, 1]
})

var outputNode = addNode(graphId, "output", {
    destination: "display",
    format: "json"
})

# Set up dependencies
addNodeDependency(inputNode, filterNode)
addNodeDependency(filterNode, transformNode)
addNodeDependency(transformNode, outputNode)

# Configure automatic execution
setExecutionMode(graphId, "continuous")
setExecutionInterval(graphId, 16)  # 60 FPS
enableParallelExecution(graphId, true)

# Start execution
startAutomaticExecution(graphId)
```

### Event-Driven UI Graph

```flowlang
# Create UI interaction graph
var uiGraphId = createGraph("uiInteractions")

# Add UI nodes
var buttonNode = addNode(uiGraphId, "button", {
    label: "Click Me",
    position: Vector3(100, 100, 0)
})

var textNode = addNode(uiGraphId, "text", {
    content: "Button not clicked",
    position: Vector3(100, 150, 0)
})

var counterNode = addNode(uiGraphId, "counter", {
    value: 0,
    increment: 1
})

# Set up event-driven execution
setExecutionMode(uiGraphId, "eventDriven")
addExecutionTrigger(uiGraphId, "buttonClicked")

# Event handlers
on buttonClicked(buttonId) {
    if (buttonId == buttonNode) {
        # Update counter
        var currentValue = getNodeProperty(counterNode, "value")
        setNodeProperty(counterNode, "value", currentValue + 1)
        
        # Update text
        var newText = "Button clicked " + (currentValue + 1) + " times"
        setNodeProperty(textNode, "content", newText)
        
        # Trigger execution
        executeGraph(uiGraphId)
    }
}
```

### Batch Processing Graph

```flowlang
# Create batch processing graph
var batchGraphId = createGraph("batchProcessing")

# Add batch nodes
var dataLoaderNode = addNode(batchGraphId, "dataLoader", {
    source: "database",
    batchSize: 1000
})

var processorNode = addNode(batchGraphId, "processor", {
    algorithm: "machineLearning",
    model: "classification"
})

var resultNode = addNode(batchGraphId, "result", {
    destination: "file",
    format: "csv"
})

# Create execution batch
createExecutionBatch("dataProcessing", [dataLoaderNode, processorNode, resultNode])
setBatchExecutionOrder("dataProcessing", [dataLoaderNode, processorNode, resultNode])

# Set up on-demand execution
setExecutionMode(batchGraphId, "onDemand")

# Execute batch when data is ready
on dataReady(dataSource) {
    if (dataSource == "database") {
        executeBatch("dataProcessing")
    }
}
```

## Configuration

### Execution Settings

```flowlang
# Global execution settings
setGlobalExecutionSettings({
    maxConcurrentGraphs: 10,
    defaultExecutionMode: "eventDriven",
    defaultExecutionInterval: 100,
    enableLogging: true,
    enableCaching: true
})

# Graph-specific settings
setGraphExecutionSettings(graphId, {
    executionMode: "continuous",
    executionInterval: 16,
    enableParallelExecution: true,
    maxParallelThreads: 4,
    enableCaching: true,
    errorRecoveryStrategy: "retry"
})
```

### Performance Monitoring

```flowlang
# Enable performance monitoring
enablePerformanceMonitoring(graphId, true)

# Monitor execution metrics
on executionMetrics(graphId, metrics) {
    print("Graph " + graphId + " metrics:")
    print("  Execution time: " + metrics.executionTime + "ms")
    print("  Node count: " + metrics.nodeCount)
    print("  Memory usage: " + metrics.memoryUsage + "MB")
    print("  Cache hit rate: " + metrics.cacheHitRate + "%")
}
```

## Best Practices

1. **Choose appropriate execution mode**: Use continuous for real-time, event-driven for interactions
2. **Optimize dependencies**: Minimize unnecessary dependencies to improve performance
3. **Use caching**: Enable caching for expensive computations
4. **Handle errors gracefully**: Implement proper error recovery strategies
5. **Monitor performance**: Track execution metrics and optimize as needed
6. **Use parallel execution**: Enable parallel processing for independent nodes
7. **Throttle execution**: Prevent excessive execution frequency
8. **Clean up resources**: Properly dispose of graphs and nodes when done

This automatic execution system enables FlowLang to power real-time, interactive applications with minimal manual intervention.
