package com.thedevjade.flow.flowPlugin.builtinextensions.nodes

import com.thedevjade.flow.extension.api.ActionNode
import com.thedevjade.flow.extension.api.ActionResult
import com.thedevjade.flow.extension.api.GraphPortDefinition
import com.thedevjade.flow.extension.api.NodeType
import com.thedevjade.flow.extension.registry.ActionNodeHandler

@ActionNode(
    name = "String Output",
    category = "OUTPUT",
    description = "Output a string value from properties",
    icon = "text_fields",
    color = "#2196F3"
)
class StringOutputActionNode : ActionNodeHandler {
    override val name: String = "String Output"
    override val category: String = "OUTPUT"
    override val description: String = "Output a string value from properties"
    override val icon: String = "text_fields"
    override val color: String = "#2196F3"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("trigger", "trigger", "Trigger input", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("output", "flow", "Flow output", true, ""),
        GraphPortDefinition("value", "string", "The string value", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>, properties: Map<String, Any?>): ActionResult {
        val outputs = mutableMapOf<String, Any?>()
        outputs["output"] = inputs["trigger"] // Pass through trigger
        
        // Get the string value from properties
        val stringValue = properties["value"] as? String ?: "Hello World"
        outputs["value"] = stringValue
        
        return ActionResult.Success(outputs)
    }
}

@ActionNode(
    name = "Number Output",
    category = "OUTPUT", 
    description = "Output a number value from properties",
    icon = "numbers",
    color = "#E91E63"
)
class NumberOutputActionNode : ActionNodeHandler {
    override val name: String = "Number Output"
    override val category: String = "OUTPUT"
    override val description: String = "Output a number value from properties"
    override val icon: String = "numbers"
    override val color: String = "#E91E63"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("trigger", "trigger", "Trigger input", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("output", "flow", "Flow output", true, ""),
        GraphPortDefinition("value", "number", "The number value", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>, properties: Map<String, Any?>): ActionResult {
        val outputs = mutableMapOf<String, Any?>()
        outputs["output"] = inputs["trigger"] // Pass through trigger
        
        // Get the number value from properties
        val numberValue = when (val propValue = properties["value"]) {
            is Number -> propValue
            is String -> propValue.toDoubleOrNull() ?: 42.0
            else -> 42.0
        }
        outputs["value"] = numberValue
        
        return ActionResult.Success(outputs)
    }
}
