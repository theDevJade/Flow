package com.thedevjade.flow.flowPlugin.builtinextensions.nodes

import com.thedevjade.flow.extension.api.*
import com.thedevjade.flow.extension.registry.ActionNodeHandler
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@ActionNode(
    name = "Send Message",
    category = "Player Actions",
    description = "Send a message to a player",
    icon = "message",
    color = "#2196F3"
)
class SendMessageActionNode : ActionNodeHandler {
    override val name: String = "Send Message"
    override val category: String = "Player Actions"
    override val description: String = "Send a message to a player"
    override val icon: String = "message"
    override val color: String = "#2196F3"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player to send the message to", true, ""),
        GraphPortDefinition("message", "string", "The message to send", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("success", "boolean", "Whether the message was sent successfully", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val player = inputs["player"] as? Player
        val message = inputs["message"] as? String

        if (player == null || message == null) {
            return ActionResult.Error("Player and message are required")
        }

        try {
            player.sendMessage(message)
            val outputs = mutableMapOf<String, Any?>()
            outputs["success"] = true
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Failed to send message: ${e.message}")
        }
    }
}

@ActionNode(
    name = "Teleport Player",
    category = "Player Actions",
    description = "Teleport a player to a location",
    icon = "flight",
    color = "#FF9800"
)
class TeleportPlayerActionNode : ActionNodeHandler {
    override val name: String = "Teleport Player"
    override val category: String = "Player Actions"
    override val description: String = "Teleport a player to a location"
    override val icon: String = "flight"
    override val color: String = "#FF9800"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player to teleport", true, ""),
        GraphPortDefinition("location", "Location", "The location to teleport to", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("success", "boolean", "Whether the teleport was successful", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val player = inputs["player"] as? Player
        val location = inputs["location"] as? Location

        if (player == null || location == null) {
            return ActionResult.Error("Player and location are required")
        }

        try {
            player.teleport(location)
            val outputs = mutableMapOf<String, Any?>()
            outputs["success"] = true
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Failed to teleport player: ${e.message}")
        }
    }
}

@ActionNode(
    name = "Give Item",
    category = "Player Actions",
    description = "Give an item to a player",
    icon = "inventory",
    color = "#4CAF50"
)
class GiveItemActionNode : ActionNodeHandler {
    override val name: String = "Give Item"
    override val category: String = "Player Actions"
    override val description: String = "Give an item to a player"
    override val icon: String = "inventory"
    override val color: String = "#4CAF50"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player to give the item to", true, ""),
        GraphPortDefinition("item", "ItemStack", "The item to give", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("success", "boolean", "Whether the item was given successfully", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val player = inputs["player"] as? Player
        val item = inputs["item"] as? ItemStack

        if (player == null || item == null) {
            return ActionResult.Error("Player and item are required")
        }

        try {
            player.inventory.addItem(item)
            val outputs = mutableMapOf<String, Any?>()
            outputs["success"] = true
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Failed to give item: ${e.message}")
        }
    }
}

@ActionNode(
    name = "Broadcast Message",
    category = "Server Actions",
    description = "Broadcast a message to all players",
    icon = "broadcast",
    color = "#9C27B0"
)
class BroadcastMessageActionNode : ActionNodeHandler {
    override val name: String = "Broadcast Message"
    override val category: String = "Server Actions"
    override val description: String = "Broadcast a message to all players"
    override val icon: String = "broadcast"
    override val color: String = "#9C27B0"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("message", "string", "The message to broadcast", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("success", "boolean", "Whether the broadcast was successful", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val message = inputs["message"] as? String

        if (message == null) {
            return ActionResult.Error("Message is required")
        }

        try {
            Bukkit.broadcastMessage(message)
            val outputs = mutableMapOf<String, Any?>()
            outputs["success"] = true
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Failed to broadcast message: ${e.message}")
        }
    }
}

@ActionNode(
    name = "Set Block",
    category = "Block Actions",
    description = "Set a block at a location",
    icon = "block",
    color = "#FF5722"
)
class SetBlockActionNode : ActionNodeHandler {
    override val name: String = "Set Block"
    override val category: String = "Block Actions"
    override val description: String = "Set a block at a location"
    override val icon: String = "block"
    override val color: String = "#FF5722"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("location", "Location", "The location to set the block", true, ""),
        GraphPortDefinition("material", "Material", "The material to set", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("success", "boolean", "Whether the block was set successfully", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val location = inputs["location"] as? Location
        val material = inputs["material"] as? Material

        if (location == null || material == null) {
            return ActionResult.Error("Location and material are required")
        }

        try {
            location.block.type = material
            val outputs = mutableMapOf<String, Any?>()
            outputs["success"] = true
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Failed to set block: ${e.message}")
        }
    }
}

@ActionNode(
    name = "Break Block",
    category = "Block Actions",
    description = "Break a block at a location",
    icon = "block_off",
    color = "#F44336"
)
class BreakBlockActionNode : ActionNodeHandler {
    override val name: String = "Break Block"
    override val category: String = "Block Actions"
    override val description: String = "Break a block at a location"
    override val icon: String = "block_off"
    override val color: String = "#F44336"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("location", "Location", "The location to break the block", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("success", "boolean", "Whether the block was broken successfully", true, ""),
        GraphPortDefinition("material", "Material", "The material that was broken", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val location = inputs["location"] as? Location

        if (location == null) {
            return ActionResult.Error("Location is required")
        }

        try {
            val material = location.block.type
            location.block.type = Material.AIR
            val outputs = mutableMapOf<String, Any?>()
            outputs["success"] = true
            outputs["material"] = material
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Failed to break block: ${e.message}")
        }
    }
}

@ActionNode(
    name = "Create Location",
    category = "Utility Actions",
    description = "Create a location from coordinates",
    icon = "place",
    color = "#607D8B"
)
class CreateLocationActionNode : ActionNodeHandler {
    override val name: String = "Create Location"
    override val category: String = "Utility Actions"
    override val description: String = "Create a location from coordinates"
    override val icon: String = "place"
    override val color: String = "#607D8B"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("world", "World", "The world for the location", true, ""),
        GraphPortDefinition("x", "number", "X coordinate", true, ""),
        GraphPortDefinition("y", "number", "Y coordinate", true, ""),
        GraphPortDefinition("z", "number", "Z coordinate", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("location", "Location", "The created location", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val world = inputs["world"] as? org.bukkit.World
        val x = (inputs["x"] as? Number)?.toDouble()
        val y = (inputs["y"] as? Number)?.toDouble()
        val z = (inputs["z"] as? Number)?.toDouble()

        if (world == null || x == null || y == null || z == null) {
            return ActionResult.Error("World and coordinates are required")
        }

        try {
            val location = Location(world, x, y, z)
            val outputs = mutableMapOf<String, Any?>()
            outputs["location"] = location
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Failed to create location: ${e.message}")
        }
    }
}

@ActionNode(
    name = "Get Block",
    category = "Block Actions",
    description = "Get block information at a location",
    icon = "search",
    color = "#795548"
)
class GetBlockActionNode : ActionNodeHandler {
    override val name: String = "Get Block"
    override val category: String = "Block Actions"
    override val description: String = "Get block information at a location"
    override val icon: String = "search"
    override val color: String = "#795548"
    override val nodeType: NodeType = NodeType.ACTION
    override val inputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("location", "Location", "The location to get block info from", true, "")
    )
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("material", "Material", "The block material", true, ""),
        GraphPortDefinition("block", "Block", "The block object", true, "")
    )

    override suspend fun execute(inputs: Map<String, Any?>): ActionResult {
        val location = inputs["location"] as? Location

        if (location == null) {
            return ActionResult.Error("Location is required")
        }

        try {
            val block = location.block
            val outputs = mutableMapOf<String, Any?>()
            outputs["material"] = block.type
            outputs["block"] = block
            return ActionResult.Success(outputs)
        } catch (e: Exception) {
            return ActionResult.Error("Failed to get block: ${e.message}")
        }
    }
}
