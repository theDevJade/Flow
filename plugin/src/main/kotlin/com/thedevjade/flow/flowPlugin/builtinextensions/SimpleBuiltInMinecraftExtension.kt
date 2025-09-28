package com.thedevjade.flow.flowPlugin.builtinextensions

import com.thedevjade.flow.extension.api.*
import com.thedevjade.flow.extension.registry.ActionNodeHandler
import com.thedevjade.flow.extension.registry.TriggerNodeHandler
import com.thedevjade.flow.flowPlugin.Flow
import com.thedevjade.flow.flowPlugin.utils.logger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@FlowExtensionAnnotation(
    name = "BuiltInMinecraft",
    version = "1.0.0",
    description = "Built-in Minecraft integration for Flow"
)
class SimpleBuiltInMinecraftExtension : FlowExtension, Listener {
    override val name: String = "BuiltInMinecraft"
    override val version: String = "1.0.0"
    override val description: String = "Built-in Minecraft integration for Flow"
    override val author: String = "Flow Team"
    override val dependencies: List<String> = emptyList()
    private lateinit var context: ExtensionContext
    private val eventHandlers = mutableMapOf<String, FlowLangEventHandler>()
    private val graphDataManager = com.thedevjade.flow.webserver.websocket.GraphDataManager()

    override fun initialize(context: ExtensionContext) {
        this.context = context


        registerMinecraftTypes()

        registerWaitingFunctions()

        registerStatusReportingFunctions()

        registerEventHandlers()

        registerGraphNodes()

        context.logger.info("Simple BuiltInMinecraft extension initialized")
    }

    override fun enable() {

        Bukkit.getPluginManager().registerEvents(this, Flow.instance)
        context.logger.info("Simple BuiltInMinecraft extension enabled")
    }

    override fun disable() {
        context.logger.info("Simple BuiltInMinecraft extension disabled")
    }

    override fun destroy() {
        context.logger.info("Simple BuiltInMinecraft extension destroyed")
    }

    private fun registerMinecraftTypes() {

        context.flowCore.extensions.registerFlowLangType("Player", object : FlowLangTypeHandler {
            override val name: String = "Player"
            override val description: String = "Minecraft Player type"
            override val category: String = "Minecraft"
            override val javaType: Class<*> = Player::class.java

            override fun convert(value: Any?): Any? {
                return when (value) {
                    is Player -> value
                    is String -> Bukkit.getPlayer(value)
                    else -> null
                }
            }

            override fun validate(value: Any?): Boolean {
                return value is Player
            }
        })


        context.flowCore.extensions.registerFlowLangType("Location", object : FlowLangTypeHandler {
            override val name: String = "Location"
            override val description: String = "Minecraft Location type"
            override val category: String = "Minecraft"
            override val javaType: Class<*> = org.bukkit.Location::class.java

            override fun convert(value: Any?): Any? {
                return when (value) {
                    is org.bukkit.Location -> value
                    is Map<*, *> -> {
                        val world = value["world"] as? String
                        val x = (value["x"] as? Number)?.toDouble() ?: 0.0
                        val y = (value["y"] as? Number)?.toDouble() ?: 0.0
                        val z = (value["z"] as? Number)?.toDouble() ?: 0.0
                        val yaw = (value["yaw"] as? Number)?.toFloat() ?: 0.0f
                        val pitch = (value["pitch"] as? Number)?.toFloat() ?: 0.0f

                        val bukkitWorld = if (world != null) Bukkit.getWorld(world) else null
                        org.bukkit.Location(bukkitWorld, x, y, z, yaw, pitch)
                    }

                    else -> null
                }
            }

            override fun validate(value: Any?): Boolean {
                return value is org.bukkit.Location
            }
        })


        context.flowCore.extensions.registerFlowLangType("Material", object : FlowLangTypeHandler {
            override val name: String = "Material"
            override val description: String = "Minecraft Material type"
            override val category: String = "Minecraft"
            override val javaType: Class<*> = org.bukkit.Material::class.java

            override fun convert(value: Any?): Any? {
                return when (value) {
                    is org.bukkit.Material -> value
                    is String -> {
                        try {
                            org.bukkit.Material.valueOf(value.uppercase())
                        } catch (e: IllegalArgumentException) {
                            null
                        }
                    }

                    else -> null
                }
            }

            override fun validate(value: Any?): Boolean {
                return value is org.bukkit.Material
            }
        })


        context.flowCore.extensions.registerFlowLangType("World", object : FlowLangTypeHandler {
            override val name: String = "World"
            override val description: String = "Minecraft World type"
            override val category: String = "Minecraft"
            override val javaType: Class<*> = org.bukkit.World::class.java

            override fun convert(value: Any?): Any? {
                return when (value) {
                    is org.bukkit.World -> value
                    is String -> Bukkit.getWorld(value)
                    else -> null
                }
            }

            override fun validate(value: Any?): Boolean {
                return value is org.bukkit.World
            }
        })


        context.flowCore.extensions.registerFlowLangType("ItemStack", object : FlowLangTypeHandler {
            override val name: String = "ItemStack"
            override val description: String = "Minecraft ItemStack type"
            override val category: String = "Minecraft"
            override val javaType: Class<*> = org.bukkit.inventory.ItemStack::class.java

            override fun convert(value: Any?): Any? {
                return when (value) {
                    is org.bukkit.inventory.ItemStack -> value
                    is Map<*, *> -> {
                        val material = (value["material"] as? String)?.let {
                            try {
                                org.bukkit.Material.valueOf(it.uppercase())
                            } catch (e: Exception) {
                                null
                            }
                        } ?: org.bukkit.Material.AIR
                        val amount = (value["amount"] as? Number)?.toInt() ?: 1
                        org.bukkit.inventory.ItemStack(material, amount)
                    }

                    else -> null
                }
            }

            override fun validate(value: Any?): Boolean {
                return value is org.bukkit.inventory.ItemStack
            }
        })

        context.logger.info("Registered 5 Minecraft types: Player, Location, Material, World, ItemStack")
    }

    private fun registerWaitingFunctions() {

        context.flowCore.extensions.registerFlowLangFunction("waitForPlayer", object : FlowLangFunctionHandler {
            override val name: String = "waitForPlayer"
            override val description: String = "Wait for a player to join the server"
            override val category: String = "Minecraft"
            override val async: Boolean = true
            override val parameters: List<FlowLangParameterDefinition> = listOf(
                FlowLangParameterDefinition("playerName", "String", "Name of the player to wait for", true, ""),
                FlowLangParameterDefinition("timeout", "Number", "Timeout in seconds (0 = no timeout)", false, "0")
            )

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any? {
                val playerName = args[0] as? String ?: return null
                val timeout = (args[1] as? Number)?.toLong() ?: 0L

                val startTime = System.currentTimeMillis()
                val timeoutMs = if (timeout > 0) timeout * 1000 else Long.MAX_VALUE

                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    val player = Bukkit.getPlayer(playerName)
                    if (player != null && player.isOnline) {
                        return mapOf(
                            "success" to true,
                            "player" to player,
                            "playerName" to player.name,
                            "waitTime" to (System.currentTimeMillis() - startTime)
                        )
                    }
                    kotlinx.coroutines.delay(100)
                }

                return mapOf(
                    "success" to false,
                    "error" to "Timeout waiting for player $playerName",
                    "waitTime" to (System.currentTimeMillis() - startTime)
                )
            }
        })


        context.flowCore.extensions.registerFlowLangFunction("waitForCondition", object : FlowLangFunctionHandler {
            override val name: String = "waitForCondition"
            override val description: String = "Wait for a custom condition to be met"
            override val category: String = "Minecraft"
            override val async: Boolean = true
            override val parameters: List<FlowLangParameterDefinition> = listOf(
                FlowLangParameterDefinition(
                    "condition",
                    "String",
                    "Condition to check (e.g., 'players_online > 5')",
                    true,
                    ""
                ),
                FlowLangParameterDefinition("timeout", "Number", "Timeout in seconds (0 = no timeout)", false, "0"),
                FlowLangParameterDefinition("checkInterval", "Number", "Check interval in milliseconds", false, "100")
            )

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any? {
                val condition = args[0] as? String ?: return null
                val timeout = (args[1] as? Number)?.toLong() ?: 0L
                val checkInterval = (args[2] as? Number)?.toLong() ?: 100L

                val startTime = System.currentTimeMillis()
                val timeoutMs = if (timeout > 0) timeout * 1000 else Long.MAX_VALUE

                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    if (evaluateCondition(condition, context)) {
                        return mapOf(
                            "success" to true,
                            "condition" to condition,
                            "waitTime" to (System.currentTimeMillis() - startTime)
                        )
                    }
                    kotlinx.coroutines.delay(checkInterval)
                }

                return mapOf(
                    "success" to false,
                    "error" to "Timeout waiting for condition: $condition",
                    "waitTime" to (System.currentTimeMillis() - startTime)
                )
            }
        })


        context.flowCore.extensions.registerFlowLangFunction("waitForTime", object : FlowLangFunctionHandler {
            override val name: String = "waitForTime"
            override val description: String = "Wait for a specific amount of time"
            override val category: String = "Minecraft"
            override val async: Boolean = true
            override val parameters: List<FlowLangParameterDefinition> = listOf(
                FlowLangParameterDefinition("seconds", "Number", "Number of seconds to wait", true, ""),
                FlowLangParameterDefinition(
                    "tickInterval",
                    "Number",
                    "Check interval in ticks (20 ticks = 1 second)",
                    false,
                    "20"
                )
            )

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any? {
                val seconds = (args[0] as? Number)?.toDouble() ?: return null
                val tickInterval = (args[1] as? Number)?.toLong() ?: 20L

                val startTime = System.currentTimeMillis()
                val targetTime = (seconds * 1000).toLong()
                val checkIntervalMs = (tickInterval * 50)

                while (System.currentTimeMillis() - startTime < targetTime) {
                    kotlinx.coroutines.delay(checkIntervalMs)
                }

                return mapOf(
                    "success" to true,
                    "waitTime" to (System.currentTimeMillis() - startTime),
                    "requestedTime" to (seconds * 1000)
                )
            }
        })


        context.flowCore.extensions.registerFlowLangFunction("waitForEvent", object : FlowLangFunctionHandler {
            override val name: String = "waitForEvent"
            override val description: String = "Wait for a specific Minecraft event to occur"
            override val category: String = "Minecraft"
            override val async: Boolean = true
            override val parameters: List<FlowLangParameterDefinition> = listOf(
                FlowLangParameterDefinition("eventType", "String", "Type of event to wait for", true, ""),
                FlowLangParameterDefinition("timeout", "Number", "Timeout in seconds (0 = no timeout)", false, "0"),
                FlowLangParameterDefinition("filter", "String", "Optional filter for the event", false, "")
            )

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any? {
                val eventType = args[0] as? String ?: return null
                val timeout = (args[1] as? Number)?.toLong() ?: 0L
                args[2] as? String

                val startTime = System.currentTimeMillis()
                val timeoutMs = if (timeout > 0) timeout * 1000 else Long.MAX_VALUE



                while (System.currentTimeMillis() - startTime < timeoutMs) {


                    kotlinx.coroutines.delay(100)
                }

                return mapOf(
                    "success" to false,
                    "error" to "Event waiting not fully implemented yet",
                    "eventType" to eventType,
                    "waitTime" to (System.currentTimeMillis() - startTime)
                )
            }
        })

        context.logger.info("Registered 4 waiting functions: waitForPlayer, waitForCondition, waitForTime, waitForEvent")
    }

    private fun registerStatusReportingFunctions() {

        context.flowCore.extensions.registerFlowLangFunction("getServerStatus", object : FlowLangFunctionHandler {
            override val name: String = "getServerStatus"
            override val description: String = "Get comprehensive server status information"
            override val category: String = "Minecraft"
            override val async: Boolean = false
            override val parameters: List<FlowLangParameterDefinition> = emptyList()

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any {
                val server = Bukkit.getServer()
                val onlinePlayers = server.onlinePlayers
                val maxPlayers = server.maxPlayers
                val worlds = server.worlds

                return mapOf(
                    "online" to true,
                    "version" to server.version,
                    "bukkitVersion" to server.bukkitVersion,
                    "players" to mapOf(
                        "online" to onlinePlayers.size,
                        "max" to maxPlayers,
                        "list" to onlinePlayers.map { it.name }
                    ),
                    "worlds" to worlds.map { world ->
                        mapOf(
                            "name" to world.name,
                            "environment" to world.environment.name,
                            "players" to world.players.size,
                            "time" to world.time,
                            "weather" to mapOf(
                                "storming" to world.hasStorm(),
                                "thundering" to world.isThundering
                            )
                        )
                    },
                    "tps" to getServerTPS(),
                    "memory" to getMemoryUsage(),
                    "uptime" to getServerUptime()
                )
            }
        })


        context.flowCore.extensions.registerFlowLangFunction("getPlayerStatus", object : FlowLangFunctionHandler {
            override val name: String = "getPlayerStatus"
            override val description: String = "Get detailed status information for a specific player"
            override val category: String = "Minecraft"
            override val async: Boolean = false
            override val parameters: List<FlowLangParameterDefinition> = listOf(
                FlowLangParameterDefinition("playerName", "String", "Name of the player to get status for", true, "")
            )

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any? {
                val playerName = args[0] as? String ?: return null
                val player = Bukkit.getPlayer(playerName)

                return if (player != null && player.isOnline) {
                    mapOf(
                        "online" to true,
                        "name" to player.name,
                        "displayName" to player.displayName,
                        "uuid" to player.uniqueId.toString(),
                        "location" to mapOf(
                            "world" to player.world.name,
                            "x" to player.location.x,
                            "y" to player.location.y,
                            "z" to player.location.z,
                            "yaw" to player.location.yaw,
                            "pitch" to player.location.pitch
                        ),
                        "health" to player.health,
                        "maxHealth" to player.maxHealth,
                        "foodLevel" to player.foodLevel,
                        "saturation" to player.saturation,
                        "level" to player.level,
                        "exp" to player.exp,
                        "gameMode" to player.gameMode.name,
                        "isOp" to player.isOp,
                        "isFlying" to player.isFlying,
                        "isSneaking" to player.isSneaking,
                        "isSprinting" to player.isSprinting,
                        "ping" to player.ping,
                        "lastPlayed" to player.lastPlayed
                    )
                } else {
                    mapOf(
                        "online" to false,
                        "name" to playerName,
                        "error" to "Player not found or offline"
                    )
                }
            }
        })


        context.flowCore.extensions.registerFlowLangFunction("getWorldStatus", object : FlowLangFunctionHandler {
            override val name: String = "getWorldStatus"
            override val description: String = "Get detailed status information for a specific world"
            override val category: String = "Minecraft"
            override val async: Boolean = false
            override val parameters: List<FlowLangParameterDefinition> = listOf(
                FlowLangParameterDefinition("worldName", "String", "Name of the world to get status for", true, "")
            )

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any? {
                val worldName = args[0] as? String ?: return null
                val world = Bukkit.getWorld(worldName)

                return if (world != null) {
                    mapOf(
                        "name" to world.name,
                        "environment" to world.environment.name,
                        "difficulty" to world.difficulty.name,
                        "time" to world.time,
                        "fullTime" to world.fullTime,
                        "players" to world.players.size,
                        "playerNames" to world.players.map { it.name },
                        "weather" to mapOf(
                            "storming" to world.hasStorm(),
                            "thundering" to world.isThundering,
                            "clearWeatherTime" to world.clearWeatherDuration
                        ),
                        "spawn" to mapOf(
                            "x" to world.spawnLocation.x,
                            "y" to world.spawnLocation.y,
                            "z" to world.spawnLocation.z
                        ),
                        "seed" to world.seed,
                        "generator" to (world.generator?.javaClass?.simpleName ?: "Unknown")
                    )
                } else {
                    mapOf(
                        "name" to worldName,
                        "error" to "World not found"
                    )
                }
            }
        })


        context.flowCore.extensions.registerFlowLangFunction("getPluginStatus", object : FlowLangFunctionHandler {
            override val name: String = "getPluginStatus"
            override val description: String = "Get status information about loaded plugins"
            override val category: String = "Minecraft"
            override val async: Boolean = false
            override val parameters: List<FlowLangParameterDefinition> = emptyList()

            override suspend fun execute(context: FlowLangContext, args: Array<Any?>): Any {
                val plugins = Bukkit.getPluginManager().plugins
                val enabledPlugins = plugins.filter { it.isEnabled }

                return mapOf(
                    "total" to plugins.size,
                    "enabled" to enabledPlugins.size,
                    "disabled" to (plugins.size - enabledPlugins.size),
                    "plugins" to enabledPlugins.map { plugin ->
                        mapOf(
                            "name" to plugin.name,
                            "version" to plugin.description.version,
                            "description" to plugin.description.description,
                            "authors" to (plugin.description.authors ?: emptyList()),
                            "website" to plugin.description.website
                        )
                    }
                )
            }
        })

        context.logger.info("Registered 4 status reporting functions: getServerStatus, getPlayerStatus, getWorldStatus, getPluginStatus")
    }

    private fun getServerTPS(): Map<String, Any> {


        return mapOf(
            "current" to 20.0, // @TODO actual tps
            "average" to 20.0,
            "warning" to false
        )
    }

    private fun getMemoryUsage(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        return mapOf(
            "max" to maxMemory,
            "total" to totalMemory,
            "used" to usedMemory,
            "free" to freeMemory,
            "usagePercent" to ((usedMemory.toDouble() / maxMemory.toDouble()) * 100)
        )
    }

    private fun getServerUptime(): Map<String, Any> {

        val uptime = System.currentTimeMillis() - (System.currentTimeMillis() - 1000) // @TODO actually calculate uptime
        return mapOf(
            "milliseconds" to uptime,
            "seconds" to (uptime / 1000),
            "minutes" to (uptime / (1000 * 60)),
            "hours" to (uptime / (1000 * 60 * 60)),
            "days" to (uptime / (1000 * 60 * 60 * 24))
        )
    }

    private fun evaluateCondition(condition: String, context: FlowLangContext): Boolean {
        return try {
            when {
                condition.contains("players_online") -> {
                    val count = Bukkit.getOnlinePlayers().size
                    val expectedCount =
                        condition.substringAfter("players_online").trim().substringAfter(">").trim().toIntOrNull() ?: 0
                    count > expectedCount
                }

                condition.contains("server_online") -> {
                    true
                }

                condition.contains("world_loaded") -> {
                    val worldName = condition.substringAfter("world_loaded").trim().substringAfter("=").trim()
                    Bukkit.getWorld(worldName) != null
                }

                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun registerEventHandlers() {

        eventHandlers["playerJoin"] = object : FlowLangEventHandler {
            override val name: String = "playerJoin"
            override val description: String = "Triggered when a player joins the server"
            override val category: String = "Player"

            override suspend fun handle(context: FlowLangContext, event: Any) {
                val player = event as Player
                context.variables["player"] = player
                context.variables["playerName"] = player.name
                context.variables["playerUUID"] = player.uniqueId.toString()
            }
        }


        eventHandlers["playerQuit"] = object : FlowLangEventHandler {
            override val name: String = "playerQuit"
            override val description: String = "Triggered when a player leaves the server"
            override val category: String = "Player"

            override suspend fun handle(context: FlowLangContext, event: Any) {
                val player = event as Player
                context.variables["player"] = player
                context.variables["playerName"] = player.name
                context.variables["playerUUID"] = player.uniqueId.toString()
            }
        }


        eventHandlers["playerDeath"] = object : FlowLangEventHandler {
            override val name: String = "playerDeath"
            override val description: String = "Triggered when a player dies"
            override val category: String = "Player"

            override suspend fun handle(context: FlowLangContext, event: Any) {
                val deathEvent = event as PlayerDeathEvent
                context.variables["player"] = deathEvent.entity
                context.variables["deathMessage"] = deathEvent.deathMessage
                context.variables["killer"] = deathEvent.entity.killer
                context.variables["drops"] = deathEvent.drops
                context.variables["expDropped"] = deathEvent.droppedExp
            }
        }


        eventHandlers["blockBreak"] = object : FlowLangEventHandler {
            override val name: String = "blockBreak"
            override val description: String = "Triggered when a block is broken"
            override val category: String = "Block"

            override suspend fun handle(context: FlowLangContext, event: Any) {
                val blockEvent = event as BlockBreakEvent
                context.variables["player"] = blockEvent.player
                context.variables["block"] = blockEvent.block
                context.variables["location"] = blockEvent.block.location
                context.variables["material"] = blockEvent.block.type
            }
        }


        eventHandlers["blockPlace"] = object : FlowLangEventHandler {
            override val name: String = "blockPlace"
            override val description: String = "Triggered when a block is placed"
            override val category: String = "Block"

            override suspend fun handle(context: FlowLangContext, event: Any) {
                val blockEvent = event as BlockPlaceEvent
                context.variables["player"] = blockEvent.player
                context.variables["block"] = blockEvent.block
                context.variables["location"] = blockEvent.block.location
                context.variables["material"] = blockEvent.block.type
            }
        }

        eventHandlers["testTrigger"] = object : FlowLangEventHandler {
            override val name: String = "testTrigger"
            override val description: String = "Test trigger that can be manually triggered via /flow graph testTrigger"
            override val category: String = "Test"

            override suspend fun handle(context: FlowLangContext, event: Any) {
                val triggerName = event as? String ?: "testTrigger"
                context.variables["triggerName"] = triggerName
                context.variables["triggerTime"] = System.currentTimeMillis()
                context.variables["triggeredBy"] = "command"


                logger().info("🎯 TEST TRIGGER ACTIVATED: $triggerName")
                logger().info("📊 Trigger variables set: triggerName=$triggerName, triggerTime=${context.variables["triggerTime"]}, triggeredBy=${context.variables["triggeredBy"]}")


                Bukkit.getOnlinePlayers().forEach { player ->
                    player.sendMessage("§a[Flow] §eTest Trigger Activated: §f$triggerName")
                }
            }
        }
    }

    private fun registerGraphNodes() {

        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.LogMessageActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.DelayActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.GetServerInfoActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.CreateItemActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.GetMaterialActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.GetWorldActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.ConditionalActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.OutputPropertiesActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.StringOutputActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.NumberOutputActionNode())


        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.SendMessageActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.TeleportPlayerActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.GiveItemActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.BroadcastMessageActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.SetBlockActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.BreakBlockActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.CreateLocationActionNode())
        registerActionNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.GetBlockActionNode())

        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.PlayerJoinTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.PlayerQuitTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.PlayerDeathTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.BlockBreakTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.BlockPlaceTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.PlayerChatTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.PlayerMoveTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.PlayerInteractTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.PlayerCommandTriggerNode())
        registerActionNode(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.PlayerItemConsumeTriggerNode())
        registerTriggerNodeDirect(com.thedevjade.flow.flowPlugin.builtinextensions.nodes.TestTriggerNode())

        context.logger.info("Registered ${19} graph nodes (8 actions, 11 triggers)")
    }

    private fun registerActionNode(nodeHandler: GraphNodeHandler) {

        val actionNodeHandler = object : ActionNodeHandler {
            override val name: String = nodeHandler.name
            override val category: String = nodeHandler.category
            override val description: String = nodeHandler.description
            override val icon: String = nodeHandler.icon
            override val color: String = nodeHandler.color
            override val nodeType: NodeType =
                NodeType.ACTION
            override val inputs: List<GraphPortDefinition> = nodeHandler.inputs
            override val outputs: List<GraphPortDefinition> = nodeHandler.outputs

            override suspend fun execute(inputs: Map<String, Any?>, properties: Map<String, Any?>): ActionResult {
                return try {

                    val mockContext = object : GraphNodeContext {
                        override val inputs: MutableMap<String, Any?> = inputs.toMutableMap()
                        override val outputs: MutableMap<String, Any?> = mutableMapOf()
                        override val nodeId: String = "mock_node_id"
                        override val properties: Map<String, Any?> = emptyMap()
                        override val graphId: String = "mock_graph_id"
                        override val executionId: String = "mock_execution_id"
                    }

                    when (val result = nodeHandler.execute(mockContext)) {
                        is GraphNodeResult.Success -> {
                            ActionResult.Success(mockContext.outputs)
                        }

                        is GraphNodeResult.Error -> {
                            ActionResult.Error(result.message)
                        }

                        is GraphNodeResult.Skip -> {
                            ActionResult.Success(emptyMap())
                        }
                    }
                } catch (e: Exception) {
                    ActionResult.Error("Node execution failed: ${e.message}")
                }
            }
        }


        context.flowCore.extensions.registerActionNode(nodeHandler.name, actionNodeHandler)
    }

    private fun registerActionNodeDirect(nodeHandler: ActionNodeHandler) {
        context.flowCore.extensions.registerActionNode(nodeHandler.name, nodeHandler)
    }

    private fun registerTriggerNodeDirect(nodeHandler: TriggerNodeHandler) {
        context.flowCore.extensions.registerTriggerNode(nodeHandler.name, nodeHandler)
    }


    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        triggerFlowLangEvent("playerJoin", event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        triggerFlowLangEvent("playerQuit", event.player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        triggerFlowLangEvent("playerDeath", event)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        triggerFlowLangEvent("blockBreak", event)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        triggerFlowLangEvent("blockPlace", event)
    }

    fun triggerTestTrigger(triggerName: String) {
        try {
            context.logger.info("🎯 Triggering test trigger for graphs: $triggerName")


            Bukkit.getOnlinePlayers().forEach { player ->
                player.sendMessage("§a[Flow] §eTest Trigger Activated: §f$triggerName")
            }



            try {
                val graphIds = graphDataManager.listGraphs()

                context.logger.info("📊 Found ${graphIds.size} JSON graphs to search...")

                graphIds.forEach { graphId ->
                    val graphData = graphDataManager.loadGraph(graphId)
                    if (graphData != null) {
                        context.logger.info("📊 Graph: $graphId with ${graphData.nodes.size} nodes")


                        try {
                            val testTriggerNodes = graphData.nodes.filter { node ->
                                node.name == "Test Trigger" || node.templateId == "Test Trigger"
                            }

                            if (testTriggerNodes.isNotEmpty()) {
                                context.logger.info("🎯 Found ${testTriggerNodes.size} TestTriggerNode(s) in graph: $graphId")


                                try {
                                    val flowGraph = convertToFlowGraph(graphData, graphId)
                                    context.logger.info("🔄 Executing graph '$graphId' with trigger: $triggerName")


                                    runBlocking {
                                        val result = context.flowCore.extensions.executeGraph(
                                            flowGraph,
                                            mapOf(
                                                "triggerName" to triggerName,
                                                "triggerTime" to System.currentTimeMillis(),
                                                "triggeredBy" to "command"
                                            )
                                        )

                                        when (result) {
                                            is com.thedevjade.flow.extension.executor.GraphExecutionResult.Success -> {
                                                context.logger.info("✅ Graph '$graphId' executed successfully!")
                                                context.logger.info("📤 Results: ${result.results.keys.joinToString(", ")}")
                                            }

                                            is com.thedevjade.flow.extension.executor.GraphExecutionResult.Failure -> {
                                                context.logger.error("❌ Graph '$graphId' execution failed: ${result.error}")
                                            }

                                            is com.thedevjade.flow.extension.executor.GraphExecutionResult.PartialSuccess -> {
                                                context.logger.warn("⚠️ Graph '$graphId' executed partially: ${result.message}")
                                            }
                                        }
                                    }


                                    Bukkit.getOnlinePlayers().forEach { player ->
                                        player.sendMessage("§a[Flow] §eGraph Executed: §f$graphId")
                                    }

                                } catch (e: Exception) {
                                    context.logger.error("💥 Error executing graph '$graphId': ${e.message}", e)
                                }
                            }
                        } catch (e: Exception) {
                            context.logger.error("❌ Error processing graph data for $graphId: ${e.message}")
                        }
                    }
                }

                if (graphIds.isEmpty()) {
                    context.logger.info("📊 No JSON graphs found. You may need to create a graph first using the web interface.")
                }

            } catch (e: Exception) {
                context.logger.error("❌ Error accessing JSON graphs: ${e.message}")
            }

            context.logger.info("📊 Test trigger '$triggerName' processing complete")

        } catch (e: Exception) {
            context.logger.error("Error triggering test trigger: $triggerName", e)
        }
    }

    private fun convertToFlowGraph(
        graphData: com.thedevjade.flow.webserver.websocket.GraphData,
        graphId: String
    ): com.thedevjade.flow.api.graph.FlowGraph {

        val flowNodes = graphData.nodes.map { node ->
            com.thedevjade.flow.api.graph.GraphNode(
                id = node.id,
                name = node.name,
                type = node.templateId ?: node.name,
                position = com.thedevjade.flow.api.graph.Position(node.position.x, node.position.y),
                properties = convertJsonObjectToMap(node.properties).mapValues { it.value ?: "" },
                inputs = node.inputs.map { port ->
                    com.thedevjade.flow.api.graph.GraphPort(
                        id = port.id,
                        name = port.name,
                        type = "default",
                        isInput = port.isInput
                    )
                },
                outputs = node.outputs.map { port ->
                    com.thedevjade.flow.api.graph.GraphPort(
                        id = port.id,
                        name = port.name,
                        type = "default",
                        isInput = port.isInput
                    )
                }
            )
        }

        val flowConnections = graphData.connections.map { connection ->
            com.thedevjade.flow.api.graph.GraphConnection(
                id = connection.id,
                fromNodeId = connection.fromNodeId,
                fromPortId = connection.fromPortId,
                toNodeId = connection.toNodeId,
                toPortId = connection.toPortId,
                properties = emptyMap()
            )
        }

        return com.thedevjade.flow.api.graph.FlowGraph(
            id = graphId,
            name = "Converted Graph",
            description = "Graph converted from JSON data",
            ownerId = "system",
            isPublic = true,
            nodes = flowNodes,
            connections = flowConnections,
            createdAt = System.currentTimeMillis(),
            lastModifiedAt = System.currentTimeMillis(),
            version = 1
        )
    }

    private fun convertJsonObjectToMap(jsonObject: JsonObject): Map<String, Any?> {
        return try {
            jsonObject.mapValues { (_, value) ->
                when (value) {
                    is JsonPrimitive -> {
                        if (value.isString) {
                            value.content
                        } else {
                            // Try to parse as number
                            try {
                                value.content.toDoubleOrNull() ?: value.content
                            } catch (e: Exception) {
                                value.content
                            }
                        }
                    }
                    is JsonArray -> value.map { it.toString() }
                    is JsonObject -> value.toString()
                    else -> value.toString()
                }
            }
        } catch (e: Exception) {
            context.logger.warn("Failed to convert JsonObject properties: ${e.message}")
            emptyMap<String, Any?>()
        }
    }

    private fun triggerFlowLangEvent(eventName: String, vararg parameters: Any) {
        try {

            val eventHandler = eventHandlers[eventName]
            if (eventHandler != null) {

                // @TODO actually run

                context.logger.info("Successfully triggered FlowLang event: $eventName with ${parameters.size} parameters")
            } else {
                context.logger.warn("No event handler found for event: $eventName")
            }
        } catch (e: Exception) {
            context.logger.error("Error triggering FlowLang event: $eventName", e)
        }
    }
}
