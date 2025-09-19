package com.thedevjade.flow.flowPlugin.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class FlowLangCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§cYou do not have permission to use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§eUsage: /flowlang <load|reload|unload> <file_name|!all>")
            return true
        }

        when (args[0].lowercase()) {
            "load" -> {
                // @TODO: Load script by filename
                sender.sendMessage("§a[FlowLang] Loading ${args.getOrNull(1)}")
            }

            "reload" -> {
                // @TODO: Reload script by filename or all
                sender.sendMessage("§e[FlowLang] Reloading ${args.getOrNull(1)}")
            }

            "unload" -> {
                // @TODO: Unload script by filename or all
                sender.sendMessage("§c[FlowLang] Unloading ${args.getOrNull(1)}")
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
            1 -> listOf("load", "reload", "unload")
                .filter { it.startsWith(args[0], true) }
                .toMutableList()

            2 -> when (args[0].lowercase()) {
                "load", "reload", "unload" -> {
                    // @TODO: Autocomplete script files from disk
                    listOf("example.flow", "!all").filter { it.startsWith(args[1], true) }.toMutableList()
                }

                else -> mutableListOf()
            }

            else -> mutableListOf()
        }
    }
}