package com.thedevjade.flow.flowPlugin.builtinextensions.nodes

import com.thedevjade.flow.extension.api.ActionNode
import com.thedevjade.flow.extension.api.ActionResult
import com.thedevjade.flow.extension.api.GraphPortDefinition
import com.thedevjade.flow.extension.api.NodeType
import com.thedevjade.flow.extension.registry.ActionNodeHandler
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

@ActionNode(
    name = "Log Message",
    category = "Utility",
    description = "Log a message to the console",
    icon = "description",
    color = "#795548"
)
class LogMessageActionNode : ActionNodeHandler {
    override val name: String = "Log Message"
    override val category: String = "Utility"
    override val description: String = "Log a message to the console"
    override val icon: String = "description"
    override val color: String = "#795548"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("message", "string", "The message to log", true, ""),
        GraphPortDefinition("level", "string", "Log level (info, warn, error, debug)", false, "info")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("success", "boolean", "Whether the message was logged successfully", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val message = inputs["message"] as? String
        val level = inputs["level"] as? String ?: "info"

        if (message == null) {
            return ActionResult.Error("Message input is required")
        }

        when (level.lowercase()) {
            "info" -> Bukkit.getLogger().info(message)
            "warn" -> Bukkit.getLogger().warning(message)
            "error" -> Bukkit.getLogger().severe(message)
            "debug" -> Bukkit.getLogger().info("[DEBUG] $message")
            else -> Bukkit.getLogger().info(message)
        }

        val outputs = mutableMapOf<String, Any?>()
        outputs["success"] = true
        return ActionResult.Success(outputs)
    }
}

@ActionNode(
    name = "Delay",
    category = "Utility",
    description = "Delay execution for a specified number of ticks",
    icon = "schedule",
    color = "#FFC107"
)
class DelayActionNode : ActionNodeHandler {
    override val name: String = "Delay"
    override val category: String = "Utility"
    override val description: String = "Delay execution for a specified number of ticks"
    override val icon: String = "schedule"
    override val color: String = "#FFC107"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("ticks", "number", "Number of ticks to delay", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("completed", "boolean", "Whether the delay completed successfully", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val ticks = (inputs["ticks"] as? Number)?.toLong()

        if (ticks == null) {
            return ActionResult.Error("Ticks input is required")
        }

        if (ticks < 0) {
            return ActionResult.Error("Ticks must be a positive number")
        }


        val delayMs = ticks * 50L

        try {

            kotlinx.coroutines.delay(delayMs)
            val outputs = mutableMapOf<String, Any?>()
            outputs["completed"] = true
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Error during delay: ${e.message}")
        }
    }
}

@ActionNode(
    name = "Get Server Info",
    category = "Server",
    description = "Get information about the server",
    icon = "info",
    color = "#2196F3"
)
class GetServerInfoActionNode : ActionNodeHandler {
    override val name: String = "Get Server Info"
    override val category: String = "Server"
    override val description: String = "Get information about the server"
    override val icon: String = "info"
    override val color: String = "#2196F3"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("serverName", "string", "The name of the server", true, ""),
        GraphPortDefinition("version", "string", "The server version", true, ""),
        GraphPortDefinition("maxPlayers", "number", "Maximum number of players", true, ""),
        GraphPortDefinition("onlinePlayers", "number", "Number of online players", true, ""),
        GraphPortDefinition("worlds", "array", "List of world names", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val server = Bukkit.getServer()

        val outputs = mutableMapOf<String, Any?>()
        outputs["serverName"] = server.name
        outputs["version"] = server.version
        outputs["maxPlayers"] = server.maxPlayers
        outputs["onlinePlayers"] = server.onlinePlayers.size
        outputs["worlds"] = server.worlds.map { it.name }

        return ActionResult.Success(outputs)
    }
}

@ActionNode(
    name = "Create Item",
    category = "Items",
    description = "Create an item stack",
    icon = "inventory_2",
    color = "#4CAF50"
)
class CreateItemActionNode : ActionNodeHandler {
    override val name: String = "Create Item"
    override val category: String = "Items"
    override val description: String = "Create an item stack"
    override val icon: String = "inventory_2"
    override val color: String = "#4CAF50"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("material", "Material", "The material for the item", true, ""),
        GraphPortDefinition("amount", "number", "The amount of items", false, "1")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("item", "ItemStack", "The created item stack", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val material = inputs["material"] as? Material
        val amount = (inputs["amount"] as? Number)?.toInt() ?: 1

        if (material == null) {
            return ActionResult.Error("Material input is required")
        }

        if (amount <= 0) {
            return ActionResult.Error("Amount must be greater than 0")
        }

        val item = ItemStack(material, amount)
        val outputs = mutableMapOf<String, Any?>()
        outputs["item"] = item

        return ActionResult.Success(outputs)
    }
}

@ActionNode(
    name = "Get Material",
    category = "Items",
    description = "Get a material by name",
    icon = "category",
    color = "#FF9800"
)
class GetMaterialActionNode : ActionNodeHandler {
    override val name: String = "Get Material"
    override val category: String = "Items"
    override val description: String = "Get a material by name"
    override val icon: String = "category"
    override val color: String = "#FF9800"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("name", "string", "The name of the material", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("material", "Material", "The material", true, ""),
        GraphPortDefinition("found", "boolean", "Whether the material was found", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val name = inputs["name"] as? String

        if (name == null) {
            return ActionResult.Error("Name input is required")
        }

        val outputs = mutableMapOf<String, Any?>()
        try {
            val material = Material.valueOf(name.uppercase())
            outputs["material"] = material
            outputs["found"] = true
        } catch (e: IllegalArgumentException) {
            outputs["material"] = null
            outputs["found"] = false
        }

        return ActionResult.Success(outputs)
    }
}

@ActionNode(
    name = "Get World",
    category = "World",
    description = "Get a world by name",
    icon = "public",
    color = "#3F51B5"
)
class GetWorldActionNode : ActionNodeHandler {
    override val name: String = "Get World"
    override val category: String = "World"
    override val description: String = "Get a world by name"
    override val icon: String = "public"
    override val color: String = "#3F51B5"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("name", "string", "The name of the world", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("world", "World", "The world", true, ""),
        GraphPortDefinition("found", "boolean", "Whether the world was found", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val name = inputs["name"] as? String

        if (name == null) {
            return ActionResult.Error("Name input is required")
        }

        val world = Bukkit.getWorld(name)
        val outputs = mutableMapOf<String, Any?>()
        outputs["world"] = world
        outputs["found"] = world != null

        return ActionResult.Success(outputs)
    }
}

@ActionNode(
    name = "Conditional",
    category = "Logic",
    description = "Execute different paths based on a condition",
    icon = "call_split",
    color = "#9E9E9E"
)
class ConditionalActionNode : ActionNodeHandler {
    override val name: String = "Conditional"
    override val category: String = "Logic"
    override val description: String = "Execute different paths based on a condition"
    override val icon: String = "call_split"
    override val color: String = "#9E9E9E"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("condition", "boolean", "The condition to evaluate", true, ""),
        GraphPortDefinition("trueValue", "any", "Value to output if condition is true", false, ""),
        GraphPortDefinition("falseValue", "any", "Value to output if condition is false", false, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("result", "any", "The selected value", true, ""),
        GraphPortDefinition("isTrue", "boolean", "Whether the condition was true", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val condition = inputs["condition"] as? Boolean
        val trueValue = inputs["trueValue"]
        val falseValue = inputs["falseValue"]

        if (condition == null) {
            return ActionResult.Error("Condition input is required")
        }

        val outputs = mutableMapOf<String, Any?>()
        outputs["isTrue"] = condition
        outputs["result"] = if (condition) trueValue else falseValue

        return ActionResult.Success(outputs)
    }
}

@ActionNode(
    name = "Output Properties",
    category = "Utility",
    description = "Output all input properties as outputs",
    icon = "output",
    color = "#FF5722"
)
class OutputPropertiesActionNode : ActionNodeHandler {
    override val name: String = "Output Properties"
    override val category: String = "Utility"
    override val description: String = "Output all input properties as outputs"
    override val icon: String = "output"
    override val color: String = "#FF5722"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("input", "any", "Any input value to output", false, ""),
        GraphPortDefinition("stringInput", "string", "String input to output", false, ""),
        GraphPortDefinition("numberInput", "number", "Number input to output", false, ""),
        GraphPortDefinition("booleanInput", "boolean", "Boolean input to output", false, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("output", "any", "The input value", true, ""),
        GraphPortDefinition("stringOutput", "string", "The string input value", true, ""),
        GraphPortDefinition("numberOutput", "number", "The number input value", true, ""),
        GraphPortDefinition("booleanOutput", "boolean", "The boolean input value", true, ""),
        GraphPortDefinition("allInputs", "object", "All input values as an object", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val outputs = mutableMapOf<String, Any?>()


        outputs["output"] = inputs["input"]
        outputs["stringOutput"] = inputs["stringInput"]
        outputs["numberOutput"] = inputs["numberInput"]
        outputs["booleanOutput"] = inputs["booleanInput"]


        outputs["allInputs"] = inputs.toMap()

        return ActionResult.Success(outputs)
    }
}

@ActionNode(
    name = "String Value",
    category = "Utility",
    description = "Output a string value",
    icon = "text_fields",
    color = "#4CAF50"
)
class StringValueActionNode : ActionNodeHandler {
    override val name: String = "String Value"
    override val category: String = "Utility"
    override val description: String = "Output a string value"
    override val icon: String = "text_fields"
    override val color: String = "#4CAF50"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("value", "string", "The string value to output", true, "Hello World")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("output", "string", "The string value", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val value = inputs["value"] as? String ?: "Hello World"

        val outputs = mutableMapOf<String, Any?>()
        outputs["output"] = value

        return ActionResult.Success(outputs)
    }
}
