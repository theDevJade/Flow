package com.thedevjade.flow.webserver.terminal

import com.thedevjade.flow.common.models.FlowLogger
import com.thedevjade.flow.webserver.terminal.commands.*
import kotlinx.serialization.json.*

class TerminalInterpreter {
    private val commandRegistry = TerminalCommandRegistry()

    init {
        registerBuiltinCommands()
    }

    private fun registerBuiltinCommands() {

        commandRegistry.register(HelpCommand())
        commandRegistry.register(ClearCommand())
        commandRegistry.register(EchoCommand())
        commandRegistry.register(PwdCommand())
        commandRegistry.register(LsCommand())
        commandRegistry.register(CdCommand())


        commandRegistry.register(GraphCommand())
        commandRegistry.register(WorkspaceCommand())
        commandRegistry.register(FlowCommand())

        FlowLogger.info("TerminalInterpreter", "Registered ${commandRegistry.getAllCommands().size} built-in commands")
    }

    fun registerCommand(command: TerminalCommand) {
        commandRegistry.register(command)
    }

    fun unregisterCommand(commandName: String) {
        commandRegistry.unregister(commandName)
    }

    suspend fun executeCommand(
        input: String,
        context: TerminalContext
    ): TerminalResult {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) {
            return TerminalResult.Success()
        }

        val parts = parseCommand(trimmedInput)
        if (parts.isEmpty()) {
            return TerminalResult.Success()
        }

        val commandName = parts[0]
        val args = parts.drop(1)

        FlowLogger.info("TerminalInterpreter",
            "Executing command: '$commandName' with args: $args")

        val command = commandRegistry.getCommand(commandName)
        return if (command != null) {
            try {
                command.execute(args, context)
            } catch (e: Exception) {
                FlowLogger.error("TerminalInterpreter",
                    "Error executing command '$commandName': ${e.message}", e)
                TerminalResult.Error("Error executing command: ${e.message}")
            }
        } else {
            TerminalResult.Error("Unknown command: $commandName. Type 'help' for available commands.")
        }
    }

    fun getAutocompleteSuggestions(
        input: String,
        cursorPosition: Int,
        context: TerminalContext
    ): List<AutocompleteSuggestion> {
        val suggestions = mutableListOf<AutocompleteSuggestion>()

        try {
            val parts = parseCommand(input)

            if (parts.isEmpty() || (parts.size == 1 && !input.endsWith(" "))) {

                val prefix = parts.firstOrNull() ?: ""
                val commandSuggestions = commandRegistry.findCommandsStartingWith(prefix)

                commandSuggestions.forEach { commandName ->
                    val command = commandRegistry.getCommand(commandName)
                    suggestions.add(
                        AutocompleteSuggestion(
                            text = commandName,
                            type = "command",
                            description = command?.description ?: "Command",
                            insertText = commandName
                        )
                    )
                }
            } else if (parts.isNotEmpty()) {

                val commandName = parts[0]
                val command = commandRegistry.getCommand(commandName)

                if (command != null) {
                    suggestions.addAll(getCommandSpecificSuggestions(command, parts.drop(1), context))
                }
            }
        } catch (e: Exception) {
            FlowLogger.error("TerminalInterpreter",
                "Error generating autocomplete suggestions: ${e.message}", e)
        }

        return suggestions.take(10)
    }

    private fun getCommandSpecificSuggestions(
        command: TerminalCommand,
        args: List<String>,
        context: TerminalContext
    ): List<AutocompleteSuggestion> {
        val suggestions = mutableListOf<AutocompleteSuggestion>()

        when (command.name) {
            "help" -> {
                if (args.isEmpty()) {
                    commandRegistry.getCommandNames().forEach { commandName ->
                        suggestions.add(
                            AutocompleteSuggestion(
                                text = commandName,
                                type = "help-topic",
                                description = "Help for $commandName",
                                insertText = commandName
                            )
                        )
                    }
                }
            }

            "graph" -> {
                if (args.isEmpty()) {
                    listOf("list", "create", "open", "save", "delete", "info").forEach { action ->
                        suggestions.add(
                            AutocompleteSuggestion(
                                text = action,
                                type = "graph-action",
                                description = "Graph $action action",
                                insertText = action
                            )
                        )
                    }
                } else if (args[0] in listOf("open", "delete", "info")) {

                    listOf("main-graph", "test-graph", "prototype", "ai-workflow").forEach { graphName ->
                        suggestions.add(
                            AutocompleteSuggestion(
                                text = graphName,
                                type = "graph-name",
                                description = "Graph file",
                                insertText = graphName
                            )
                        )
                    }
                }
            }

            "workspace" -> {
                if (args.isEmpty()) {
                    listOf("info", "list", "switch", "create", "delete").forEach { action ->
                        suggestions.add(
                            AutocompleteSuggestion(
                                text = action,
                                type = "workspace-action",
                                description = "Workspace $action action",
                                insertText = action
                            )
                        )
                    }
                } else if (args[0] in listOf("switch", "delete")) {

                    listOf("default", "development", "production", "experimental").forEach { workspaceName ->
                        suggestions.add(
                            AutocompleteSuggestion(
                                text = workspaceName,
                                type = "workspace-name",
                                description = "Workspace",
                                insertText = workspaceName
                            )
                        )
                    }
                }
            }

            "flow" -> {
                if (args.isEmpty()) {
                    listOf("status", "info", "version", "config", "restart").forEach { action ->
                        suggestions.add(
                            AutocompleteSuggestion(
                                text = action,
                                type = "flow-action",
                                description = "Flow $action action",
                                insertText = action
                            )
                        )
                    }
                }
            }

            "cd" -> {
                if (args.isEmpty()) {

                    listOf("/", "/graphs", "/workspaces", "/files", "/projects").forEach { dir ->
                        suggestions.add(
                            AutocompleteSuggestion(
                                text = dir,
                                type = "directory",
                                description = "Directory",
                                insertText = dir
                            )
                        )
                    }
                }
            }
        }

        return suggestions
    }

    private fun parseCommand(input: String): List<String> {

        return input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    }

    fun getCommandList(): List<CommandInfo> {
        return commandRegistry.getAllCommands().values.map { command ->
            CommandInfo(
                name = command.name,
                description = command.description,
                usage = command.usage
            )
        }.sortedBy { it.name }
    }
}

data class AutocompleteSuggestion(
    val text: String,
    val type: String,
    val description: String,
    val insertText: String = text
)

data class CommandInfo(
    val name: String,
    val description: String,
    val usage: String
)