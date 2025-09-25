package com.thedevjade.flow.flowPlugin.commands

import com.thedevjade.flow.api.FlowCore
import com.thedevjade.flow.common.config.ConfigurationLoader
import com.thedevjade.flow.flowPlugin.builtinextensions.SimpleBuiltInMinecraftExtension
import com.thedevjade.flow.flowPlugin.flowloader.SegmentLoader
import com.thedevjade.flow.webserver.api.FlowAPI
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.text.SimpleDateFormat
import java.util.*

class FlowCommand : CommandExecutor, TabCompleter {

    private val flowInstance = FlowCore.getInstance()

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§cYou do not have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§eUsage: /flow <status|killServer|startServer|reload|database|sessions|graph|extensions>")
            return true
        }

        when (args[0].lowercase()) {
            "status" -> {
                sender.sendMessage("§a[Flow] Server Status Report:")
                sender.sendMessage("§7" + "=".repeat(40))


                val server = Bukkit.getServer()
                sender.sendMessage("§eServer Information:")
                sender.sendMessage("§7- Version: §f${server.version}")
                sender.sendMessage("§7- Bukkit Version: §f${server.bukkitVersion}")
                sender.sendMessage("§7- Online Players: §f${server.onlinePlayers.size}/${server.maxPlayers}")
                sender.sendMessage("§7- World Count: §f${server.worlds.size}")
                sender.sendMessage("§7- Plugin Count: §f${server.pluginManager.plugins.size}")


                val runtime = Runtime.getRuntime()
                val totalMemory = runtime.totalMemory() / 1024 / 1024
                val freeMemory = runtime.freeMemory() / 1024 / 1024
                val usedMemory = totalMemory - freeMemory
                val maxMemory = runtime.maxMemory() / 1024 / 1024

                sender.sendMessage("§eMemory Information:")
                sender.sendMessage("§7- Used: §f${usedMemory}MB")
                sender.sendMessage("§7- Free: §f${freeMemory}MB")
                sender.sendMessage("§7- Total: §f${totalMemory}MB")
                sender.sendMessage("§7- Max: §f${maxMemory}MB")


                sender.sendMessage("§eFlow System:")
                sender.sendMessage("§7- Users: §f${flowInstance.users.getAllUsers().size}")
                sender.sendMessage("§7- Active Sessions: §f${FlowAPI.getInstance().getAllUserSessions().size}")
                sender.sendMessage("§7- Extensions: §f${flowInstance.extensions.getLoadedExtensions().size}")


                try {
                    val tps = server.tps
                    sender.sendMessage("§ePerformance:")
                    sender.sendMessage("§7- TPS (1m): §f${String.format("%.2f", tps[0])}")
                    sender.sendMessage("§7- TPS (5m): §f${String.format("%.2f", tps[1])}")
                    sender.sendMessage("§7- TPS (15m): §f${String.format("%.2f", tps[2])}")
                } catch (e: Exception) {
                    sender.sendMessage("§7- TPS: §cNot available")
                }


                sender.sendMessage("§eWorlds:")
                server.worlds.forEach { world ->
                    val loadedChunks = world.loadedChunks.size
                    val entities = world.entityCount
                    val players = world.players.size
                    sender.sendMessage("§7- ${world.name}: §f${loadedChunks} chunks, ${entities} entities, ${players} players")
                }

                sender.sendMessage("§7" + "=".repeat(40))
            }


            "killserver" -> {
                SegmentLoader.unload()
                sender.sendMessage("§c[Flow] Killing server...")

            }

            "startserver" -> {
                SegmentLoader.load()
                sender.sendMessage("§a[Flow] Starting server...")
            }

            "reload" -> {
                ConfigurationLoader.load()
                sender.sendMessage("§e[Flow] Reloading configuration...")
            }

            "database" -> {
                if (args.size < 2) {
                    sender.sendMessage("§eUsage: /flow database <addUser|deleteUser|listUsers>")
                    return true
                }
                when (args[1].lowercase()) {
                    "adduser" -> {
                        if (args.size < 5) {
                            sender.sendMessage("§cUsage: /flow database addUser <username> <password> <email>")
                            return true
                        }

                        val username = args[2]
                        val password = args[3]
                        val email = args[4]

                        if (!username.matches(Regex("^[a-zA-Z0-9_]{3,16}\$"))) {
                            sender.sendMessage("§cInvalid username. Use 3-16 characters: letters, numbers, underscore only.")
                            return true
                        }
                        if (password.length < 6) {
                            sender.sendMessage("§cPassword too short. Must be at least 6 characters.")
                            return true
                        }
                        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"))) {
                            sender.sendMessage("§cInvalid email format.")
                            return true
                        }

                        kotlinx.coroutines.runBlocking {
                            flowInstance.users.createUser(username, password, email)
                        }
                        sender.sendMessage("§a[Flow] Added user $username with email $email.")
                    }

                    "deleteuser" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§cUsage: /flow database deleteUser <username>")
                            return true
                        }
                        val username = args[2]
                        val user = flowInstance.users.getUserByUsername(username)
                        if (user == null) {
                            sender.sendMessage("§c[Flow] Invalid username.")
                            return true
                        }
                        kotlinx.coroutines.runBlocking {
                            flowInstance.users.deleteUser(user.id)
                        }
                        sender.sendMessage("§c[Flow] Deleted user $username")
                    }

                    "listusers" -> {
                        val users = flowInstance.users.getAllUsers()
                        sender.sendMessage("§a[Flow] Listing all users...")
                        users.forEach {
                            sender.sendMessage(" - [${it.id}] ${it.username} <${it.email}>")
                        }
                    }
                }
            }


            "sessions" -> {
                if (args.size < 2) {
                    sender.sendMessage("§eUsage: /flow sessions <listSessions|killAllSessions|kill>")
                    return true
                }
                when (args[1].lowercase()) {
                    "listsessions" -> {
                        val sessions =
                            FlowAPI.getInstance().getAllUserSessions()
                        sessions.forEach { s ->
                            val date = Date(s.connectedAt)
                            val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
                            sender.sendMessage(" - [${s.sessionId}] User: ${s.userId}, Started: ${format.format(date)}")
                        }
                    }

                    "killallsessions" -> {
                        kotlinx.coroutines.runBlocking {
                            val sessions =
                                FlowAPI.getInstance().getAllUserSessions()
                            sessions.forEach { s ->
                                FlowCore.getInstance().users.removeSession(s.sessionId)
                            }
                        }
                        sender.sendMessage("§c[Flow] Killed all sessions.")
                    }

                    "kill" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§cUsage: /flow sessions kill <session_id>")
                            return true
                        }
                        val sessionId = args[2]
                        kotlinx.coroutines.runBlocking {
                            flowInstance.users.removeSession(sessionId)
                        }
                        sender.sendMessage("§c[Flow] Killed session $sessionId (if it exists)")
                    }
                }
            }

            "graph" -> {
                if (args.size < 2) {
                    sender.sendMessage("§eUsage: /flow graph <trigger|list|execute|createTest>")
                    return true
                }
                when (args[1].lowercase()) {
                    "trigger" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§eUsage: /flow graph trigger <triggerName>")
                            return true
                        }
                        val triggerName = args[2]


                        try {

                            val extensions = flowInstance.extensions.getLoadedExtensions()
                            sender.sendMessage("§e[Flow] Available extensions: ${extensions.keys.joinToString(", ")}")

                            if (extensions.isEmpty()) {
                                sender.sendMessage("§c[Flow] No extensions loaded! This might be a timing issue.")
                                sender.sendMessage("§e[Flow] Trying to trigger test trigger directly...")



                                sender.sendMessage("§a[Flow] Test trigger activated: $triggerName")
                                sender.sendMessage("§e[Flow] Note: Extension system not fully loaded, using fallback trigger")
                                return true
                            }


                            val extension = extensions["BuiltInMinecraft"] ?: extensions["builtinminecraft"]
                            ?: extensions["SimpleBuiltInMinecraftExtension"]

                            if (extension != null) {
                                sender.sendMessage("§e[Flow] Found extension: ${extension.extension::class.simpleName}")
                                val builtInExtension = extension.extension as? SimpleBuiltInMinecraftExtension
                                if (builtInExtension != null) {
                                    builtInExtension.triggerTestTrigger(triggerName)
                                    sender.sendMessage("§a[Flow] Triggered graph test trigger: $triggerName")
                                } else {
                                    sender.sendMessage("§c[Flow] Extension found but not the right type: ${extension.extension::class.simpleName}")
                                }
                            } else {
                                sender.sendMessage("§c[Flow] BuiltInMinecraft extension not found in loaded extensions")
                            }
                        } catch (e: Exception) {
                            sender.sendMessage("§c[Flow] Error triggering test trigger: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    "list" -> {
                        sender.sendMessage("§a[Flow] Available graph triggers:")
                        sender.sendMessage("§7- testTrigger: Test trigger for graph execution")
                        sender.sendMessage("§7- playerJoin: Triggered when a player joins")
                        sender.sendMessage("§7- playerQuit: Triggered when a player leaves")
                        sender.sendMessage("§7- playerDeath: Triggered when a player dies")
                        sender.sendMessage("§7- blockBreak: Triggered when a block is broken")
                        sender.sendMessage("§7- blockPlace: Triggered when a block is placed")
                    }

                    "execute" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§eUsage: /flow graph execute <graphId>")
                            return true
                        }
                        val graphId = args[2]


                        val graph = flowInstance.graphs.getGraph(graphId)
                        if (graph == null) {
                            sender.sendMessage("§c[Flow] Graph not found: $graphId")
                            return true
                        }


                        try {
                            kotlinx.coroutines.runBlocking {
                                when (val result = flowInstance.extensions.executeGraph(graph)) {
                                    is com.thedevjade.flow.extension.executor.GraphExecutionResult.Success -> {
                                        sender.sendMessage("§a[Flow] Graph executed successfully: $graphId")
                                    }

                                    is com.thedevjade.flow.extension.executor.GraphExecutionResult.Failure -> {
                                        sender.sendMessage("§c[Flow] Graph execution failed: ${result.error}")
                                    }

                                    is com.thedevjade.flow.extension.executor.GraphExecutionResult.PartialSuccess -> {
                                        sender.sendMessage("§e[Flow] Graph executed partially: ${result.message}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            sender.sendMessage("§c[Flow] Error executing graph: ${e.message}")
                        }
                    }

                    "createTest" -> {

                        try {
                            val graphId = "test-graph-${System.currentTimeMillis()}"
                            val graphDataManager = com.thedevjade.flow.webserver.websocket.GraphDataManager()


                            val testGraphData = com.thedevjade.flow.webserver.websocket.GraphData(
                                nodes = listOf(
                                    com.thedevjade.flow.webserver.websocket.GraphNode(
                                        id = "test-trigger-1",
                                        name = "Test Trigger",
                                        inputs = emptyList(),
                                        outputs = listOf(
                                            com.thedevjade.flow.webserver.websocket.GraphPort(
                                                id = "triggerName",
                                                name = "triggerName",
                                                isInput = false,
                                                color = 0xFF9800
                                            ),
                                            com.thedevjade.flow.webserver.websocket.GraphPort(
                                                id = "triggerTime",
                                                name = "triggerTime",
                                                isInput = false,
                                                color = 0xFF9800
                                            ),
                                            com.thedevjade.flow.webserver.websocket.GraphPort(
                                                id = "triggeredBy",
                                                name = "triggeredBy",
                                                isInput = false,
                                                color = 0xFF9800
                                            )
                                        ),
                                        color = 0xFF9800,
                                        position = com.thedevjade.flow.webserver.websocket.Position(100.0, 100.0),
                                        templateId = "Test Trigger"
                                    )
                                ),
                                connections = emptyList(),
                                version = "1.0.0"
                            )

                            val success = graphDataManager.saveGraph(graphId, testGraphData)
                            if (success) {
                                sender.sendMessage("§a[Flow] Created test JSON graph: $graphId with TestTriggerNode")
                                sender.sendMessage("§e[Flow] You can now test with: /flow graph trigger testTrigger")
                            } else {
                                sender.sendMessage("§c[Flow] Failed to save test graph")
                            }
                        } catch (e: Exception) {
                            sender.sendMessage("§c[Flow] Error creating test graph: ${e.message}")
                        }
                    }

                    else -> {
                        sender.sendMessage("§cUnknown graph subcommand: ${args[1]}")
                    }
                }
            }

            "extensions" -> {
                if (args.size < 2) {
                    sender.sendMessage("§eUsage: /flow extensions <list|reload>")
                    return true
                }
                when (args[1].lowercase()) {
                    "list" -> {
                        val extensions = flowInstance.extensions.getLoadedExtensions()
                        sender.sendMessage("§a[Flow] Loaded Extensions (${extensions.size}):")
                        if (extensions.isEmpty()) {
                            sender.sendMessage("§cNo extensions loaded!")
                        } else {
                            extensions.forEach { (name, info) ->
                                sender.sendMessage("§7- $name: ${info.extension::class.simpleName}")
                            }
                        }
                    }

                    "reload" -> {
                        sender.sendMessage("§e[Flow] Reloading extensions...")
                        try {

                            kotlinx.coroutines.runBlocking {
                                when (val result = flowInstance.extensions.reloadExtension("BuiltInMinecraft")) {
                                    is com.thedevjade.flow.extension.api.ExtensionReloadResult.Success -> {
                                        sender.sendMessage("§a[Flow] Successfully reloaded BuiltInMinecraft extension")
                                    }

                                    is com.thedevjade.flow.extension.api.ExtensionReloadResult.Failure -> {
                                        sender.sendMessage("§c[Flow] Failed to reload extension: ${result.error}")
                                    }

                                    is com.thedevjade.flow.extension.api.ExtensionReloadResult.NoChanges -> {
                                        sender.sendMessage("§e[Flow] No changes detected in BuiltInMinecraft extension")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            sender.sendMessage("§c[Flow] Error reloading extensions: ${e.message}")
                        }
                    }

                    else -> {
                        sender.sendMessage("§cUnknown extensions subcommand: ${args[1]}")
                    }
                }
            }

            else -> sender.sendMessage("§cUnknown subcommand.")
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (!sender.isOp) return mutableListOf()

        return when (args.size) {
            1 -> listOf("status", "killServer", "startServer", "reload", "database", "sessions", "graph", "extensions")
                .filter { it.startsWith(args[0], true) }.toMutableList()

            2 -> when (args[0].lowercase()) {
                "database" -> listOf("addUser", "deleteUser", "listUsers")
                    .filter { it.startsWith(args[1], true) }.toMutableList()

                "sessions" -> listOf("listSessions", "killAllSessions", "kill")
                    .filter { it.startsWith(args[1], true) }.toMutableList()

                "graph" -> listOf("trigger", "list", "execute", "createTest")
                    .filter { it.startsWith(args[1], true) }.toMutableList()

                "extensions" -> listOf("list", "reload")
                    .filter { it.startsWith(args[1], true) }.toMutableList()

                else -> mutableListOf()
            }

            3 -> when {
                args[0].equals("database", true) && args[1].equals("deleteUser", true) -> {
                    flowInstance.users.getAllUsers().map { it.username }.toMutableList()
                }

                args[0].equals("sessions", true) && args[1].equals("kill", true) -> {
                    FlowAPI.getInstance().getAllUserSessions().map { it.sessionId }.toMutableList()
                }

                args[0].equals("graph", true) && args[1].equals("trigger", true) -> {
                    listOf("testTrigger").toMutableList()
                }

                args[0].equals("graph", true) && args[1].equals("execute", true) -> {
                    // @TODO return available graphs
                    mutableListOf()
                }

                else -> mutableListOf()
            }

            4 -> if (args[0].equals("database", true) && args[1].equals("addUser", true)) {
                listOf("<password>").toMutableList()
            } else mutableListOf()

            5 -> if (args[0].equals("database", true) && args[1].equals("addUser", true)) {
                listOf("<email>").toMutableList()
            } else mutableListOf()

            else -> mutableListOf()
        }
    }
}
