package com.thedevjade.flow.flowPlugin.builtinextensions.nodes

import com.thedevjade.flow.extension.api.*
import com.thedevjade.flow.extension.registry.TriggerNodeHandler

@TriggerNode(
    name = "Player Join",
    category = "Player Events",
    description = "Triggered when a player joins the server",
    icon = "person_add",
    color = "#4CAF50"
)
class PlayerJoinTriggerNode : GraphNodeHandler {
    override val name: String = "Player Join"
    override val category: String = "Player Events"
    override val description: String = "Triggered when a player joins the server"
    override val icon: String = "person_add"
    override val color: String = "#4CAF50"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who joined", true, ""),
        GraphPortDefinition("playerName", "string", "The name of the player", true, ""),
        GraphPortDefinition("playerUUID", "string", "The UUID of the player", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Player Quit",
    category = "Player Events",
    description = "Triggered when a player leaves the server",
    icon = "person_remove",
    color = "#F44336"
)
class PlayerQuitTriggerNode : GraphNodeHandler {
    override val name: String = "Player Quit"
    override val category: String = "Player Events"
    override val description: String = "Triggered when a player leaves the server"
    override val icon: String = "person_remove"
    override val color: String = "#F44336"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who left", true, ""),
        GraphPortDefinition("playerName", "string", "The name of the player", true, ""),
        GraphPortDefinition("playerUUID", "string", "The UUID of the player", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Player Death",
    category = "Player Events",
    description = "Triggered when a player dies",
    icon = "skull",
    color = "#9C27B0"
)
class PlayerDeathTriggerNode : GraphNodeHandler {
    override val name: String = "Player Death"
    override val category: String = "Player Events"
    override val description: String = "Triggered when a player dies"
    override val icon: String = "skull"
    override val color: String = "#9C27B0"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who died", true, ""),
        GraphPortDefinition("deathMessage", "string", "The death message", true, ""),
        GraphPortDefinition("killer", "Player", "The player who killed them", false, ""),
        GraphPortDefinition("drops", "array", "Items dropped on death", true, ""),
        GraphPortDefinition("expDropped", "number", "Experience points dropped", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Player Chat",
    category = "Player Events",
    description = "Triggered when a player sends a chat message",
    icon = "chat",
    color = "#2196F3"
)
class PlayerChatTriggerNode : GraphNodeHandler {
    override val name: String = "Player Chat"
    override val category: String = "Player Events"
    override val description: String = "Triggered when a player sends a chat message"
    override val icon: String = "chat"
    override val color: String = "#2196F3"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who sent the message", true, ""),
        GraphPortDefinition("message", "string", "The chat message", true, ""),
        GraphPortDefinition("format", "string", "The message format", true, ""),
        GraphPortDefinition("recipients", "array", "List of message recipients", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Player Move",
    category = "Player Events",
    description = "Triggered when a player moves",
    icon = "directions_walk",
    color = "#FF9800"
)
class PlayerMoveTriggerNode : GraphNodeHandler {
    override val name: String = "Player Move"
    override val category: String = "Player Events"
    override val description: String = "Triggered when a player moves"
    override val icon: String = "directions_walk"
    override val color: String = "#FF9800"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who moved", true, ""),
        GraphPortDefinition("from", "Location", "The previous location", true, ""),
        GraphPortDefinition("to", "Location", "The new location", true, ""),
        GraphPortDefinition("hasMoved", "boolean", "Whether the player actually moved", true, ""),
        GraphPortDefinition("hasRotated", "boolean", "Whether the player rotated", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Block Break",
    category = "Block Events",
    description = "Triggered when a block is broken",
    icon = "block",
    color = "#FF5722"
)
class BlockBreakTriggerNode : GraphNodeHandler {
    override val name: String = "Block Break"
    override val category: String = "Block Events"
    override val description: String = "Triggered when a block is broken"
    override val icon: String = "block"
    override val color: String = "#FF5722"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who broke the block", true, ""),
        GraphPortDefinition("block", "Block", "The block that was broken", true, ""),
        GraphPortDefinition("location", "Location", "The location of the broken block", true, ""),
        GraphPortDefinition("material", "Material", "The material of the broken block", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Block Place",
    category = "Block Events",
    description = "Triggered when a block is placed",
    icon = "add_box",
    color = "#4CAF50"
)
class BlockPlaceTriggerNode : GraphNodeHandler {
    override val name: String = "Block Place"
    override val category: String = "Block Events"
    override val description: String = "Triggered when a block is placed"
    override val icon: String = "add_box"
    override val color: String = "#4CAF50"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who placed the block", true, ""),
        GraphPortDefinition("block", "Block", "The block that was placed", true, ""),
        GraphPortDefinition("location", "Location", "The location of the placed block", true, ""),
        GraphPortDefinition("material", "Material", "The material of the placed block", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Player Interact",
    category = "Player Events",
    description = "Triggered when a player interacts with something",
    icon = "touch_app",
    color = "#9C27B0"
)
class PlayerInteractTriggerNode : GraphNodeHandler {
    override val name: String = "Player Interact"
    override val category: String = "Player Events"
    override val description: String = "Triggered when a player interacts with something"
    override val icon: String = "touch_app"
    override val color: String = "#9C27B0"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who interacted", true, ""),
        GraphPortDefinition("action", "string", "The type of interaction", true, ""),
        GraphPortDefinition("item", "ItemStack", "The item used in interaction", false, ""),
        GraphPortDefinition("block", "Block", "The block interacted with", false, ""),
        GraphPortDefinition("location", "Location", "The location of interaction", false, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Player Command",
    category = "Player Events",
    description = "Triggered when a player executes a command",
    icon = "terminal",
    color = "#607D8B"
)
class PlayerCommandTriggerNode : GraphNodeHandler {
    override val name: String = "Player Command"
    override val category: String = "Player Events"
    override val description: String = "Triggered when a player executes a command"
    override val icon: String = "terminal"
    override val color: String = "#607D8B"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who executed the command", true, ""),
        GraphPortDefinition("command", "string", "The full command string", true, ""),
        GraphPortDefinition("args", "array", "Command arguments", true, ""),
        GraphPortDefinition("isCancelled", "boolean", "Whether the command was cancelled", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Player Item Consume",
    category = "Player Events",
    description = "Triggered when a player consumes an item",
    icon = "restaurant",
    color = "#795548"
)
class PlayerItemConsumeTriggerNode : GraphNodeHandler {
    override val name: String = "Player Item Consume"
    override val category: String = "Player Events"
    override val description: String = "Triggered when a player consumes an item"
    override val icon: String = "restaurant"
    override val color: String = "#795548"
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("player", "Player", "The player who consumed the item", true, ""),
        GraphPortDefinition("item", "ItemStack", "The item that was consumed", true, ""),
        GraphPortDefinition("isCancelled", "boolean", "Whether the consumption was cancelled", true, "")
    )

    override suspend fun execute(context: GraphNodeContext): GraphNodeResult {
        return GraphNodeResult.Success
    }
}

@TriggerNode(
    name = "Test Trigger",
    category = "Test Events",
    description = "Test trigger that can be manually triggered via /flow graph testTrigger",
    icon = "play_arrow",
    color = "#FF9800"
)
class TestTriggerNode : TriggerNodeHandler {
    override val name: String = "Test Trigger"
    override val category: String = "Test Events"
    override val description: String = "Test trigger that can be manually triggered via /flow graph testTrigger"
    override val icon: String = "play_arrow"
    override val color: String = "#FF9800"
    override val nodeType: NodeType = NodeType.TRIGGER
    override val inputs: List<GraphPortDefinition> = emptyList()
    override val outputs: List<GraphPortDefinition> = listOf(
        GraphPortDefinition("triggerName", "string", "The name of the trigger", true, ""),
        GraphPortDefinition("triggerTime", "number", "The time when the trigger was activated", true, ""),
        GraphPortDefinition("triggeredBy", "string", "What triggered this event", true, "")
    )

    override suspend fun execute(): TriggerResult {
        return TriggerResult.Success
    }
}
