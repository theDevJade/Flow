package com.thedevjade.flow.flowPlugin.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.io.File

class SimpleFlowLangCommand : CommandExecutor, TabCompleter {
    private val loadedScripts = mutableMapOf<String, String>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§cYou do not have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /flowlang <load|reload|unload|list> [script]")
            return true
        }

        when (args[0].lowercase()) {
            "load" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /flowlang load <script>")
                    return true
                }
                loadScript(sender, args[1])
            }

            "reload" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /flowlang reload <script>")
                    return true
                }
                reloadScript(sender, args[1])
            }

            "unload" -> {
                if (args.size < 2) {
                    sender.sendMessage("§cUsage: /flowlang unload <script>")
                    return true
                }
                unloadScript(sender, args[1])
            }

            "list" -> {
                listLoadedScripts(sender)
            }

            else -> {
                sender.sendMessage("§cUnknown subcommand: ${args[0]}")
                sender.sendMessage("§cUsage: /flowlang <load|reload|unload|list> [script]")
            }
        }

        return true
    }

    private fun loadScript(sender: CommandSender, scriptName: String) {
        val scriptFile = getScriptFile(scriptName)

        if (!scriptFile.exists()) {
            sender.sendMessage("§cScript file not found: ${scriptFile.name}")
            return
        }

        try {
            val content = scriptFile.readText()
            loadedScripts[scriptName] = content
            sender.sendMessage("§aScript '$scriptName' loaded successfully!")
            sender.sendMessage("§7Content preview: ${content.take(100)}...")
        } catch (e: Exception) {
            sender.sendMessage("§cError loading script: ${e.message}")
        }
    }

    private fun reloadScript(sender: CommandSender, scriptName: String) {
        if (!loadedScripts.containsKey(scriptName)) {
            sender.sendMessage("§cScript '$scriptName' is not loaded. Use 'load' first.")
            return
        }

        loadScript(sender, scriptName)
        sender.sendMessage("§aScript '$scriptName' reloaded!")
    }

    private fun unloadScript(sender: CommandSender, scriptName: String) {
        if (!loadedScripts.containsKey(scriptName)) {
            sender.sendMessage("§cScript '$scriptName' is not loaded.")
            return
        }

        loadedScripts.remove(scriptName)
        sender.sendMessage("§aScript '$scriptName' unloaded!")
    }

    private fun listLoadedScripts(sender: CommandSender) {
        if (loadedScripts.isEmpty()) {
            sender.sendMessage("§7No scripts are currently loaded.")
            return
        }

        sender.sendMessage("§aLoaded scripts (${loadedScripts.size}):")
        loadedScripts.keys.forEach { scriptName ->
            sender.sendMessage("§7- $scriptName")
        }
    }

    private fun getScriptFile(scriptName: String): File {
        val flowLangPath = "./flowlang"
        val scriptDir = File(flowLangPath)

        if (!scriptDir.exists()) {
            scriptDir.mkdirs()
        }

        val fileName = if (scriptName.endsWith(".flowlang")) scriptName else "$scriptName.flowlang"
        return File(scriptDir, fileName)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.isOp) return emptyList()

        return when (args.size) {
            1 -> listOf("load", "reload", "unload", "list").filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> when (args[0].lowercase()) {
                "load", "reload", "unload" -> getAvailableScripts().filter { it.startsWith(args[1], ignoreCase = true) }
                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun getAvailableScripts(): List<String> {
        val flowLangPath = "./flowlang"
        val scriptDir = File(flowLangPath)

        if (!scriptDir.exists()) {
            return listOf("!all")
        }

        val scripts = scriptDir.listFiles { _, name -> name.endsWith(".flowlang") }
            ?.map { it.name.removeSuffix(".flowlang") }
            ?: emptyList()

        return listOf("!all") + scripts
    }
}
